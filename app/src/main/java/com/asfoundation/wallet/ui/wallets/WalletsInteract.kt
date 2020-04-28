package com.asfoundation.wallet.ui.wallets

import com.asfoundation.wallet.entity.Wallet
import com.asfoundation.wallet.interact.CreateWalletInteract
import com.asfoundation.wallet.interact.FetchWalletsInteract
import com.asfoundation.wallet.repository.SharedPreferencesRepository
import com.asfoundation.wallet.ui.balance.BalanceInteract
import com.asfoundation.wallet.ui.iab.FiatValue
import com.asfoundation.wallet.util.sumByBigDecimal
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

class WalletsInteract(private val balanceInteract: BalanceInteract,
                      private val fetchWalletsInteract: FetchWalletsInteract,
                      private val createWalletInteract: CreateWalletInteract,
                      private val preferencesRepository: SharedPreferencesRepository) {

  fun retrieveWalletsModel(): Single<WalletsModel> {
    val wallets = ArrayList<WalletBalance>()
    val currentWalletAddress = preferencesRepository.getCurrentWalletAddress()
    return retrieveWallets().filter { it.isNotEmpty() }
        .flatMapCompletable { list ->
          Observable.fromIterable(list)
              .flatMapCompletable { wallet ->
                balanceInteract.getTotalBalance(wallet.address)
                    .take(1)
                    .flatMapCompletable { fiatValue ->
                      Completable.fromAction {
                        wallets.add(WalletBalance(wallet.address, fiatValue,
                            currentWalletAddress == wallet.address))
                      }
                    }
              }
        }
        .toSingle {
          WalletsModel(getTotalBalance(wallets), wallets.size, wallets)
        }
  }

  fun createWallet(): Completable {
    return createWalletInteract.create()
        .flatMapCompletable {
          createWalletInteract.setDefaultWallet(it.address)
        }
  }

  private fun getTotalBalance(walletBalance: List<WalletBalance>): FiatValue {
    val totalBalance = walletBalance.sumByBigDecimal { it.balance.amount }
    return FiatValue(totalBalance, walletBalance[0].balance.currency,
        walletBalance[0].balance.symbol)
  }

  private fun retrieveWallets(): Observable<List<Wallet>> {
    return fetchWalletsInteract.fetch()
        .map { it.toList() }
        .toObservable()
  }
}