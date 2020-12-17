package com.asfoundation.wallet.ui.settings.wallets

import androidx.fragment.app.Fragment
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import io.reactivex.disposables.CompositeDisposable

@InstallIn(FragmentComponent::class)
@Module
class SettingsWalletsModule {

  @Provides
  fun providesSettingsWalletsPresenter(fragment: SettingsWalletsFragment,
                                       navigator: SettingsWalletsNavigator): SettingsWalletsPresenter {
    return SettingsWalletsPresenter(fragment as SettingsWalletsView, navigator,
        CompositeDisposable())
  }

  @Provides
  fun providesSettingsWalletsNavigator(
      fragment: SettingsWalletsFragment): SettingsWalletsNavigator {
    return SettingsWalletsNavigator(fragment.requireFragmentManager())
  }

  @Provides
  fun providesFragment(fragment: Fragment): SettingsWalletsFragment {
    return fragment as SettingsWalletsFragment
  }
}