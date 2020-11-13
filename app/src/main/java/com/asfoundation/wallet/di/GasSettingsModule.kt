package com.asfoundation.wallet.di

import com.asfoundation.wallet.interact.FindDefaultNetworkInteract
import com.asfoundation.wallet.viewmodel.GasSettingsViewModelFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@InstallIn(ActivityComponent::class)
@Module
class GasSettingsModule {
  @Provides
  fun provideGasSettingsViewModelFactory(findDefaultNetworkInteract: FindDefaultNetworkInteract) =
      GasSettingsViewModelFactory(findDefaultNetworkInteract)
}