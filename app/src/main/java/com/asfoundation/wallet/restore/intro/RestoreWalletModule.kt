package com.asfoundation.wallet.restore.intro

import android.content.Context
import androidx.fragment.app.Fragment
import com.asfoundation.wallet.billing.analytics.WalletsEventSender
import com.asfoundation.wallet.logging.Logger
import com.asfoundation.wallet.navigator.ActivityNavigatorContract
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.qualifiers.ActivityContext
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

@InstallIn(FragmentComponent::class)
@Module
class RestoreWalletModule {

  @Provides
  fun providesRestoreWalletNavigator(fragment: RestoreWalletFragment,
                                     @ActivityContext context: Context): RestoreWalletNavigator {
    return RestoreWalletNavigator(fragment.parentFragmentManager,
        context as ActivityNavigatorContract)
  }

  @Provides
  fun providesRestoreWalletPresenter(fragment: RestoreWalletFragment,
                                     navigator: RestoreWalletNavigator,
                                     interactor: RestoreWalletInteractor, logger: Logger,
                                     eventSender: WalletsEventSender): RestoreWalletPresenter {
    return RestoreWalletPresenter(fragment as RestoreWalletView, CompositeDisposable(), navigator,
        interactor, eventSender, logger, AndroidSchedulers.mainThread(), Schedulers.computation()
    )
  }

  @Provides
  fun providesFragment(fragment: Fragment): RestoreWalletFragment {
    return fragment as RestoreWalletFragment
  }
}