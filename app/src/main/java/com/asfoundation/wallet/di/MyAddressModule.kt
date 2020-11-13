package com.asfoundation.wallet.di

import com.asfoundation.wallet.router.TransactionsRouter
import com.asfoundation.wallet.viewmodel.MyAddressViewModelFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@InstallIn(ActivityComponent::class)
@Module
class MyAddressModule {
  @Provides
  fun providesMyAddressViewModelFactory(transactionsRouter: TransactionsRouter) =
      MyAddressViewModelFactory(transactionsRouter)
}