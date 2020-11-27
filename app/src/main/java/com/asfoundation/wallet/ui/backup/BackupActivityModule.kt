package com.asfoundation.wallet.ui.backup

import android.content.Context
import com.asfoundation.wallet.billing.analytics.WalletsEventSender
import com.asfoundation.wallet.ui.backup.BackupActivity.Companion.WALLET_ADDRESS
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext

@InstallIn(ActivityComponent::class)
@Module
class BackupActivityModule {

  @Provides
  fun providesBackupActivityPresenter(activity: BackupActivity,
                                      data: BackupActivityData,
                                      navigator: BackupActivityNavigator,
                                      walletsEventSender: WalletsEventSender): BackupActivityPresenter {
    return BackupActivityPresenter(activity as BackupActivityView, data, navigator,
        walletsEventSender)
  }

  @Provides
  fun providesBackupActivityData(activity: BackupActivity): BackupActivityData {
    activity.intent.extras!!.apply {
      return BackupActivityData(getString(WALLET_ADDRESS)!!)
    }
  }

  @Provides
  fun providesBackupActivityNavigator(activity: BackupActivity): BackupActivityNavigator {
    return BackupActivityNavigator(activity.supportFragmentManager, activity)
  }

  @Provides
  fun providesBackupActivity(@ActivityContext context: Context): BackupActivity {
    return context as BackupActivity
  }
}