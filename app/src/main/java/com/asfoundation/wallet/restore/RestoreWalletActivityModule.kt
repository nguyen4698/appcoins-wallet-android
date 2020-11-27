package com.asfoundation.wallet.restore

import android.content.Context
import com.asfoundation.wallet.billing.analytics.WalletsEventSender
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext


@InstallIn(ActivityComponent::class)
@Module
class RestoreWalletActivityModule {

  @Provides
  fun providesRestoreWalletActivityPresenter(activity: RestoreWalletActivity,
                                             walletsEventSender: WalletsEventSender,
                                             navigator: RestoreWalletActivityNavigator): RestoreWalletActivityPresenter {
    return RestoreWalletActivityPresenter(activity as RestoreWalletActivityView, walletsEventSender,
        navigator)
  }

  @Provides
  fun providesRestoreWalletActivityNavigator(
      activity: RestoreWalletActivity): RestoreWalletActivityNavigator {
    return RestoreWalletActivityNavigator(activity, activity.supportFragmentManager)
  }

  @Provides
  fun providesRestoreWalletActivity(@ActivityContext context: Context): RestoreWalletActivity {
    return context as RestoreWalletActivity
  }
}

