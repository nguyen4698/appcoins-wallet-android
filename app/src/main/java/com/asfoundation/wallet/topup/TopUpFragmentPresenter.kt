package com.asfoundation.wallet.topup

import android.os.Bundle
import android.util.Log
import com.appcoins.wallet.gamification.repository.ForecastBonusAndLevel
import com.appcoins.wallet.gamification.repository.GamificationStats
import com.appcoins.wallet.gamification.repository.Levels
import com.asfoundation.wallet.billing.adyen.PaymentType
import com.asfoundation.wallet.logging.Logger
import com.asfoundation.wallet.repository.PreferencesRepositoryType
import com.asfoundation.wallet.topup.TopUpData.Companion.DEFAULT_VALUE
import com.asfoundation.wallet.ui.gamification.GamificationMapper
import com.asfoundation.wallet.ui.iab.FiatValue
import com.asfoundation.wallet.ui.iab.PaymentGamificationInfo
import com.asfoundation.wallet.util.CurrencyFormatUtils
import com.asfoundation.wallet.util.isNoNetworkException
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function3
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit


class TopUpFragmentPresenter(private val view: TopUpFragmentView,
                             private val activity: TopUpActivityView?,
                             private val interactor: TopUpInteractor,
                             private val preferencesRepositoryType: PreferencesRepositoryType,
                             private val viewScheduler: Scheduler,
                             private val networkScheduler: Scheduler,
                             private val topUpAnalytics: TopUpAnalytics,
                             private val formatter: CurrencyFormatUtils,
                             private val selectedValue: String?,
                             private val gamificationMapper: GamificationMapper,
                             private val logger: Logger) {

  private val disposables: CompositeDisposable = CompositeDisposable()
  private var cachedTopUpData: TopUpData? = null
  private var cachedGamificationLevel = 0
  private var hasDefaultValues = false

  companion object {
    private val TAG = TopUpFragmentPresenter::class.java.name
    private const val NUMERIC_REGEX = "^([1-9]|[0-9]+[,.]+[0-9])[0-9]*?\$"
    private const val TOP_UP_DATA = "top_up_data"
    private const val GAMIFICATION_LEVEL = "gamification_level"
  }

  fun present(appPackage: String, savedInstanceState: Bundle?) {
    savedInstanceState?.let {
      cachedTopUpData = savedInstanceState.getSerializable(TOP_UP_DATA) as TopUpData?
      cachedGamificationLevel = savedInstanceState.getInt(GAMIFICATION_LEVEL)
    }
    setupUi()
    handleChangeCurrencyClick()
    handleNextClick()
    handleRetryClick()
    handleManualAmountChange(appPackage)
    handlePaymentMethodSelected()
    handleValuesClicks()
    handleKeyboardEvents()
    handleAuthenticationResult()
  }

  fun stop() {
    interactor.cleanCachedValues()
    disposables.dispose()
  }

  private fun setupUi() {
    disposables.add(Single.zip(
        interactor.getLimitTopUpValues()
            .subscribeOn(networkScheduler)
            .observeOn(viewScheduler),
        interactor.getDefaultValues()
            .subscribeOn(networkScheduler)
            .observeOn(viewScheduler),
        BiFunction { values: TopUpLimitValues, defaultValues: TopUpValuesModel ->
          if (values.error.hasError || defaultValues.error.hasError &&
              (values.error.isNoNetwork || defaultValues.error.isNoNetwork)) {
            view.showNoNetworkError()
          } else {
            view.setupCurrency(LocalCurrency(values.maxValue.symbol, values.maxValue.currency))
            updateDefaultValues(defaultValues)
          }
        })
        .subscribe({}, { handleError(it) }))
  }

  private fun retrievePaymentMethods(fiatAmount: String, currency: String): Completable {
    return interactor.getPaymentMethods(fiatAmount, currency)
        .subscribeOn(networkScheduler)
        .observeOn(viewScheduler)
        .doOnSuccess {
          if (it.isNotEmpty()) view.setupPaymentMethods(it)
        }
        .ignoreElement()
  }

  private fun updateDefaultValues(topUpValuesModel: TopUpValuesModel) {
    hasDefaultValues = topUpValuesModel.error.hasError.not() && topUpValuesModel.values.size >= 3
    if (hasDefaultValues) {
      val defaultValues = topUpValuesModel.values
      val defaultFiatValue = defaultValues.drop(1)
          .first()
      view.setDefaultAmountValue(selectedValue ?: defaultFiatValue.amount.toString())
      view.setValuesAdapter(defaultValues)
    } else {
      view.hideValuesAdapter()
    }
  }

  private fun handleKeyboardEvents() {
    disposables.add(view.getKeyboardEvents()
        .doOnNext {
          if (it && hasDefaultValues) view.showValuesAdapter()
          else view.hideValuesAdapter()
        }
        .subscribeOn(viewScheduler)
        .subscribe({}, { it.printStackTrace() })
    )
  }

  private fun handleError(throwable: Throwable) {
    throwable.printStackTrace()
    if (throwable.isNoNetworkException()) view.showNoNetworkError()
  }

  private fun handleChangeCurrencyClick() {
    disposables.add(view.getChangeCurrencyClick()
        .doOnNext {
          view.toggleSwitchCurrencyOn()
          view.rotateChangeCurrencyButton()
          view.switchCurrencyData()
          view.toggleSwitchCurrencyOff()
        }
        .subscribe({}, { it.printStackTrace() }))
  }

  private fun handleNextClick() {
    disposables.add(
        view.getNextClick()
            .flatMap { topUpData ->
              interactor.getLimitTopUpValues()
                  .toObservable()
                  .filter {
                    isCurrencyValid(topUpData.currency) && isValueInRange(it,
                        topUpData.currency.fiatValue.toDouble()) && topUpData.paymentMethod != null
                  }
                  .subscribeOn(networkScheduler)
                  .observeOn(viewScheduler)
                  .doOnNext {
                    topUpAnalytics.sendSelectionEvent(topUpData.currency.appcValue.toDouble(),
                        "next", topUpData.paymentMethod!!.paymentType.name)
                    if (preferencesRepositoryType.hasAuthenticationPermission()) {
                      cachedTopUpData = topUpData
                      activity?.showAuthenticationActivity()
                    } else {
                      navigateToPayment(topUpData, cachedGamificationLevel)
                    }
                  }
            }
            .subscribe({}, { handleError(it) }))
  }


  private fun handleManualAmountChange(packageName: String) {
    disposables.add(view.getEditTextChanges()
        .doOnNext { resetValues(it) }
        .debounce(700, TimeUnit.MILLISECONDS, viewScheduler)
        .doOnNext { handleInputValue(it) }
        .filter { isNumericOrEmpty(it) }
        .switchMapCompletable { topUpData ->
          getConvertedValue(topUpData)
              .subscribeOn(networkScheduler)
              .map { value -> updateConversionValue(value.amount, topUpData) }
              .filter { isConvertedValueAvailable(it) }
              .observeOn(viewScheduler)
              .doOnComplete { view.setConversionValue(topUpData) }
              .flatMapCompletable {
                interactor.getLimitTopUpValues()
                    .toObservable()
                    .subscribeOn(networkScheduler)
                    .observeOn(viewScheduler)
                    .flatMapCompletable { handleInsertedValue(packageName, topUpData, it) }
              }
              .doOnError { handleError(it) }
              .onErrorComplete()
        }
        .subscribe({}, { it.printStackTrace() }))
  }

  private fun handleInvalidFormatInput() {
    handleEmptyOrDefaultInput()
    view.hideValueInputWarning()
    view.changeMainValueColor(false)
  }

  private fun handleEmptyOrDefaultInput() {
    view.hideBonusAndSkeletons()
    view.setNextButtonState(false)
  }

  private fun resetValues(topUpData: TopUpData) {
    view.setNextButtonState(false)
    view.hideValueInputWarning()
    updateConversionValue(BigDecimal.ZERO, topUpData)
    view.setConversionValue(topUpData)
  }

  private fun handleInputValue(topUpData: TopUpData) {
    if (isNumericOrEmpty(topUpData)) {
      if (topUpData.currency.fiatValue == DEFAULT_VALUE) {
        handleEmptyOrDefaultInput()
      }
    } else {
      handleInvalidFormatInput()
    }
  }

  private fun isNumericOrEmpty(data: TopUpData): Boolean {
    return if (data.selectedCurrencyType == TopUpData.FIAT_CURRENCY) {
      data.currency.fiatValue == DEFAULT_VALUE || data.currency.fiatValue.matches(
          NUMERIC_REGEX.toRegex())
    } else {
      data.currency.appcValue == DEFAULT_VALUE || data.currency.appcValue.matches(
          NUMERIC_REGEX.toRegex())
    }
  }

  private fun getConvertedValue(data: TopUpData): Observable<FiatValue> {
    return if (data.selectedCurrencyType == TopUpData.FIAT_CURRENCY
        && data.currency.fiatValue != DEFAULT_VALUE) {
      interactor.convertLocal(data.currency.fiatCurrencyCode,
          data.currency.fiatValue, 2)
    } else if (data.selectedCurrencyType == TopUpData.APPC_C_CURRENCY
        && data.currency.appcValue != DEFAULT_VALUE) {
      interactor.convertAppc(data.currency.appcValue)
    } else {
      Observable.just(FiatValue(BigDecimal.ZERO, ""))
    }
  }

  private fun handlePaymentMethodSelected() {
    disposables.add(view.getPaymentMethodClick()
        .doOnNext { view.paymentMethodsFocusRequest() }
        .subscribe({}, { it.printStackTrace() }))
  }

  private fun loadBonusIntoView(appPackage: String, amount: String,
                                currency: String): Completable {
    return interactor.convertLocal(currency, amount, 18)
        .flatMapSingle {
          Single.zip(interactor.getEarningBonus(appPackage, it.amount),
              interactor.getUserStatus(), interactor.getLevels(),
              Function3 { earningBonus: ForecastBonusAndLevel, userStats: GamificationStats, levels: Levels ->
                PaymentGamificationInfo(earningBonus, userStats, levels, it.amount)
              })
        }
        .subscribeOn(networkScheduler)
        .observeOn(viewScheduler)
        .doOnNext {
          cachedGamificationLevel = it.earningBonus.level
          if (interactor.isBonusValidAndActive(it.earningBonus)) {
            val scaledBonus = formatter.scaleFiat(it.earningBonus.amount)
            if (shouldShowLevelUp(it.userStats, it.levels)) {
              setupNextLevelInformation(it.userStats, it.levels, it.paymentAppcAmount, scaledBonus,
                  it.earningBonus.currency)
            } else {
              view.showLegacyBonus(scaledBonus, it.earningBonus.currency)
            }
          } else {
            view.removeBonus()
          }
          view.setNextButtonState(true)
        }
        .ignoreElements()
  }

  private fun setupNextLevelInformation(userStats: GamificationStats, levels: Levels,
                                        topUpAmount: BigDecimal, scaledBonus: BigDecimal,
                                        currency: String) {
    val progress = getProgressPercentage(userStats, levels.list)
    val currentLevelInfo = gamificationMapper.mapCurrentLevelInfo(cachedGamificationLevel)
    val nextLevelInfo = gamificationMapper.mapCurrentLevelInfo(cachedGamificationLevel + 1)
    view.setLevelUpInformation(cachedGamificationLevel, progress,
        gamificationMapper.getRectangleGamificationBackground(currentLevelInfo.levelColor),
        gamificationMapper.getRectangleGamificationBackground(nextLevelInfo.levelColor),
        currentLevelInfo.levelColor, willLevelUp(userStats, topUpAmount),
        userStats.nextLevelAmount!!.minus(userStats.totalSpend), scaledBonus, currency)
  }

  private fun willLevelUp(userStats: GamificationStats, topUpAmount: BigDecimal): Boolean {
    return userStats.totalSpend + topUpAmount >= userStats.nextLevelAmount
  }

  private fun shouldShowLevelUp(userStats: GamificationStats, levels: Levels): Boolean {
    return cachedGamificationLevel < levels.list.size - 1 && levels.status == Levels.Status.OK && userStats.status == GamificationStats.Status.OK && userStats.nextLevelAmount != null
  }

  private fun getProgressPercentage(userStats: GamificationStats,
                                    list: List<Levels.Level>): Double {
    return if (cachedGamificationLevel <= list.size - 1) {
      var levelRange = userStats.nextLevelAmount?.minus(list[cachedGamificationLevel].amount)
      if (levelRange?.toDouble() == 0.0) {
        levelRange = BigDecimal.ONE
      }
      val amountSpentInLevel = userStats.totalSpend - list[cachedGamificationLevel].amount
      amountSpentInLevel.divide(levelRange, 2, RoundingMode.HALF_EVEN)
          .multiply(BigDecimal(100))
          .toDouble()
    } else 0.0
  }

  private fun handleInsertedValue(packageName: String, topUpData: TopUpData,
                                  limitValues: TopUpLimitValues): Completable {
    view.setNextButtonState(false)
    val fiatAmount = BigDecimal(topUpData.currency.fiatValue)
    if (topUpData.currency.fiatValue != DEFAULT_VALUE && !limitValues.error.hasError) {
      handleValueWarning(limitValues.maxValue, limitValues.minValue, fiatAmount)
    } else {
      handleInvalidFormatInput()
    }
    return updateUiInformation(packageName, limitValues,
        topUpData.currency.fiatValue, topUpData.currency.fiatCurrencyCode)
  }

  private fun updateUiInformation(appPackage: String,
                                  limitValues: TopUpLimitValues, fiatAmount: String,
                                  currency: String): Completable {
    return if (isValueInRange(limitValues, fiatAmount.toDouble())) {
      view.changeMainValueColor(true)
      view.hidePaymentMethods()
      if (interactor.isBonusValidAndActive()) view.showBonusSkeletons()
      retrievePaymentMethods(fiatAmount, currency)
          .andThen(loadBonusIntoView(appPackage, fiatAmount, currency))
    } else {
      view.hideBonusAndSkeletons()
      view.changeMainValueColor(false)
      view.setNextButtonState(false)
      Completable.complete()
    }
  }

  private fun handleRetryClick() {
    disposables.add(view.retryClick()
        .observeOn(viewScheduler)
        .doOnNext { view.showRetryAnimation() }
        .delay(1, TimeUnit.SECONDS)
        .observeOn(viewScheduler)
        .doOnNext {
          view.showSkeletons()
          setupUi()
        }
        .subscribe({}, { it.printStackTrace() }))
  }

  private fun handleValueWarning(maxValue: FiatValue, minValue: FiatValue, amount: BigDecimal) {
    val localCurrency = " ${maxValue.currency}"
    when {
      amount == BigDecimal(-1) -> {
        view.hideValueInputWarning()
        Log.w("TopUpFragmentPresenter", "Unable to retrieve values")
      }
      amount > maxValue.amount -> view.showMaxValueWarning(
          maxValue.amount.toPlainString() + localCurrency)
      amount < minValue.amount -> view.showMinValueWarning(
          minValue.amount.toPlainString() + localCurrency)
      else -> view.hideValueInputWarning()
    }
  }

  private fun isValueInRange(limitValues: TopUpLimitValues, value: Double): Boolean {
    return limitValues.error.hasError || limitValues.minValue.amount.toDouble() <= value &&
        limitValues.maxValue.amount.toDouble() >= value
  }

  private fun isCurrencyValid(currencyData: CurrencyData): Boolean {
    return currencyData.appcValue != DEFAULT_VALUE && currencyData.fiatValue != DEFAULT_VALUE
  }

  private fun updateConversionValue(value: BigDecimal, topUpData: TopUpData): TopUpData {
    if (topUpData.selectedCurrencyType == TopUpData.FIAT_CURRENCY) {
      topUpData.currency.appcValue =
          if (value == BigDecimal.ZERO) DEFAULT_VALUE else value.toString()
    } else {
      topUpData.currency.fiatValue =
          if (value == BigDecimal.ZERO) DEFAULT_VALUE else value.toString()
    }
    return topUpData
  }

  private fun isConvertedValueAvailable(data: TopUpData): Boolean {
    return if (data.selectedCurrencyType == TopUpData.FIAT_CURRENCY) {
      data.currency.appcValue != DEFAULT_VALUE
    } else {
      data.currency.fiatValue != DEFAULT_VALUE
    }
  }

  private fun handleValuesClicks() {
    disposables.add(view.getValuesClicks()
        .throttleFirst(50, TimeUnit.MILLISECONDS)
        .doOnNext { view.disableSwapCurrencyButton() }
        .doOnNext {
          if (view.getSelectedCurrency() == TopUpData.FIAT_CURRENCY) {
            view.changeMainValueText(it.amount.toString())
          } else {
            convertAndChangeMainValue(it.currency, it.amount)
          }
        }
        .debounce(300, TimeUnit.MILLISECONDS, viewScheduler)
        .doOnNext { view.enableSwapCurrencyButton() }
        .subscribe({}, { it.printStackTrace() }))
  }

  private fun convertAndChangeMainValue(currency: String, amount: BigDecimal) {
    disposables.add(interactor.convertLocal(currency, amount.toString(), 2)
        .subscribeOn(networkScheduler)
        .observeOn(viewScheduler)
        .doOnNext { view.changeMainValueText(it.amount.toString()) }
        .doOnError { handleError(it) }
        .subscribe({}, { it.printStackTrace() }))
  }

  private fun handleAuthenticationResult() {
    disposables.add(activity!!.onAuthenticationResult()
        .observeOn(viewScheduler)
        .doOnNext { success ->
          if (success && cachedTopUpData != null) {
            navigateToPayment(cachedTopUpData!!, cachedGamificationLevel)
          }
        }
        .subscribe({}, { it.printStackTrace() }))
  }

  private fun navigateToPayment(topUpData: TopUpData, gamificationLevel: Int) {
    val paymentMethod = topUpData.paymentMethod!!
    when (paymentMethod.paymentType) {
      PaymentType.CARD, PaymentType.PAYPAL -> activity?.navigateToAdyenPayment(
          paymentMethod.paymentType, mapTopUpPaymentData(topUpData, gamificationLevel))
      PaymentType.LOCAL_PAYMENTS ->
        activity?.navigateToLocalPayment(paymentMethod.paymentId, paymentMethod.icon,
            paymentMethod.label, mapTopUpPaymentData(topUpData, gamificationLevel))
    }
  }

  private fun mapTopUpPaymentData(topUpData: TopUpData, gamificationLevel: Int): TopUpPaymentData {
    return TopUpPaymentData(topUpData.currency.fiatValue, topUpData.currency.fiatCurrencyCode,
        topUpData.selectedCurrencyType, topUpData.bonusValue, topUpData.currency.fiatCurrencySymbol,
        topUpData.currency.appcValue, "TOPUP", gamificationLevel)
  }

  fun onSavedInstance(outState: Bundle) {
    outState.putSerializable(TOP_UP_DATA, cachedTopUpData)
    outState.putInt(GAMIFICATION_LEVEL, cachedGamificationLevel)
  }
}
