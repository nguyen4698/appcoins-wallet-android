package com.asfoundation.wallet.ui.iab

import android.os.Bundle
import androidx.annotation.StringRes
import io.reactivex.Observable
import java.math.BigDecimal

interface OnChainBuyView {

  fun getOkErrorClick(): Observable<Any?>

  fun getSupportIconClick(): Observable<Any?>

  fun getSupportLogoClick(): Observable<Any?>

  fun close(data: Bundle?)

  fun finish(data: Bundle?)

  fun showError()

  fun showTransactionCompleted()

  fun showWrongNetworkError()

  fun showNoNetworkError()

  fun showApproving()

  fun showBuying()

  fun showNonceError()

  fun showNoTokenFundsError()

  fun showNoEtherFundsError()

  fun showNoFundsError()

  fun showForbiddenError()

  fun showRaidenChannelValues(values: List<BigDecimal>)

  fun getAnimationDuration(): Long

  fun lockRotation()

  fun showWalletValidation(@StringRes error: Int)

  fun launchPerkBonusService(address: String)
}