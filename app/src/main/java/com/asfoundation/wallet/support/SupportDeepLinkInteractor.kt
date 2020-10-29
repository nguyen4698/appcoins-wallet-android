package com.asfoundation.wallet.support

import com.asfoundation.wallet.repository.PreferencesRepositoryType

class SupportDeepLinkInteractor(private val supportInteractor: SupportInteractor,
                                private val preferencesRepositoryType: PreferencesRepositoryType) {

  fun displayChatScreen() = supportInteractor.displayChatScreen()

  fun hasSeenOnboarding() = preferencesRepositoryType.hasCompletedOnboarding()
}
