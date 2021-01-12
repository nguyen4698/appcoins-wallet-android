package com.asfoundation.wallet.promotions

import androidx.fragment.app.Fragment
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

@InstallIn(FragmentComponent::class)
@Module
class PromotionsModule {

  @Provides
  fun providesPromotionsPresenter(fragment: PromotionsFragment,
                                  navigator: PromotionsNavigator,
                                  interactor: PromotionsInteractor): PromotionsPresenter {
    return PromotionsPresenter(fragment, navigator, interactor, CompositeDisposable(),
        Schedulers.io(), AndroidSchedulers.mainThread())
  }

  @Provides
  fun providesPromotionsNavigator(fragment: PromotionsFragment): PromotionsNavigator {
    return PromotionsNavigator(fragment)
  }

  @Provides
  fun providesPromotionsFragment(fragment: Fragment): PromotionsFragment {
    return fragment as PromotionsFragment
  }
}