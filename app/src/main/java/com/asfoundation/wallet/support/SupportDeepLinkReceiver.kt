package com.asfoundation.wallet.support

import android.os.Bundle
import com.asfoundation.wallet.ui.BaseActivity
import dagger.android.AndroidInjection
import javax.inject.Inject

class SupportDeepLinkReceiver : BaseActivity() {

  @Inject
  lateinit var supportDeepLinkInteractor: SupportDeepLinkInteractor

  @Inject
  lateinit var supportDeepLinkNavigator: SupportDeepLinkNavigator

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    AndroidInjection.inject(this)
    val presenter = SupportDeepLinkPresenter(supportDeepLinkInteractor, supportDeepLinkNavigator)
    presenter.present()
  }
}