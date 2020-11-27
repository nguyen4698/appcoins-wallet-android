package com.asfoundation.wallet.ui.iab.localpayments

import androidx.fragment.app.Fragment
import com.asfoundation.wallet.logging.Logger
import com.asfoundation.wallet.navigator.UriNavigator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal

@InstallIn(FragmentComponent::class)
@Module
class LocalPaymentModule {

  @Provides
  fun providesLocalPaymentPresenter(fragment: LocalPaymentFragment, data: LocalPaymentData,
                                    interactor: LocalPaymentInteractor,
                                    navigator: LocalPaymentNavigator,
                                    analytics: LocalPaymentAnalytics,
                                    logger: Logger): LocalPaymentPresenter {
    return LocalPaymentPresenter(fragment as LocalPaymentView, data, interactor, navigator,
        analytics, AndroidSchedulers.mainThread(), Schedulers.io(), CompositeDisposable(),
        fragment.context, logger)
  }

  @Provides
  fun providesLocalPaymentData(fragment: LocalPaymentFragment): LocalPaymentData {
    fragment.arguments!!.apply {
      return LocalPaymentData(getString(LocalPaymentFragment.DOMAIN_KEY)!!,
          getString(LocalPaymentFragment.SKU_ID_KEY),
          getString(LocalPaymentFragment.ORIGINAL_AMOUNT_KEY),
          getString(LocalPaymentFragment.CURRENCY_KEY),
          getString(LocalPaymentFragment.BONUS_KEY),
          getString(LocalPaymentFragment.PAYMENT_KEY)!!,
          getString(LocalPaymentFragment.DEV_ADDRESS_KEY)!!,
          getString(LocalPaymentFragment.TYPE_KEY)!!,
          getSerializable(LocalPaymentFragment.AMOUNT_KEY) as BigDecimal,
          getString(LocalPaymentFragment.CALLBACK_URL),
          getString(LocalPaymentFragment.ORDER_REFERENCE),
          getString(LocalPaymentFragment.PAYLOAD),
          getString(LocalPaymentFragment.PAYMENT_METHOD_URL),
          getString(LocalPaymentFragment.PAYMENT_METHOD_LABEL),
          getBoolean(LocalPaymentFragment.ASYNC),
          getInt(LocalPaymentFragment.GAMIFICATION_LEVEL))
    }
  }

  @Provides
  fun providesLocalPaymentNavigator(fragment: LocalPaymentFragment): LocalPaymentNavigator {
    //This should have it's own provider in the IabActivity, but it's not in the scope of this branch
    return LocalPaymentNavigator(fragment.activity as UriNavigator)
  }

  @Provides
  fun providesLocalPaymentFragment(fragment: Fragment): LocalPaymentFragment {
    return fragment as LocalPaymentFragment
  }
}