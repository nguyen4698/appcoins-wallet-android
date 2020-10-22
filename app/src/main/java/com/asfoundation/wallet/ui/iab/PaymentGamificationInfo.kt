package com.asfoundation.wallet.ui.iab

import com.appcoins.wallet.gamification.repository.ForecastBonusAndLevel
import com.appcoins.wallet.gamification.repository.GamificationStats
import com.appcoins.wallet.gamification.repository.Levels
import java.math.BigDecimal

data class PaymentGamificationInfo(val earningBonus: ForecastBonusAndLevel,
                                   val userStats: GamificationStats,
                                   val levels: Levels,
                                   val paymentAppcAmount: BigDecimal)
