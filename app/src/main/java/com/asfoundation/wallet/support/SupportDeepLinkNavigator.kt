package com.asfoundation.wallet.support

import android.content.Context
import com.asfoundation.wallet.router.OnboardingRouter

class SupportDeepLinkNavigator(private val context: Context) {

  fun showOnBoarding() {
    OnboardingRouter().open(context, true)
  }
}
