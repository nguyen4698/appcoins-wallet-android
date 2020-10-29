package com.asfoundation.wallet.support

class SupportDeepLinkPresenter(private val supportDeepLinkInteractor: SupportDeepLinkInteractor,
                               private val supportDeepLinkNavigator: SupportDeepLinkNavigator) {

  fun present() {
    handleNavigation()
  }

  private fun handleNavigation() {
    if (supportDeepLinkInteractor.hasSeenOnboarding()) {
      supportDeepLinkInteractor.displayChatScreen()
    } else {
      supportDeepLinkNavigator.showOnBoarding()
    }
  }
}
