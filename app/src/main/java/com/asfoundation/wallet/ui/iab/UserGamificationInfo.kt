package com.asfoundation.wallet.ui.iab

import com.appcoins.wallet.gamification.repository.ForecastBonusAndLevel
import com.appcoins.wallet.gamification.repository.GamificationStats
import com.appcoins.wallet.gamification.repository.Levels

data class UserGamificationInfo(val earningBonus: ForecastBonusAndLevel,
                                val userStats: GamificationStats,
                                val levels: Levels)
