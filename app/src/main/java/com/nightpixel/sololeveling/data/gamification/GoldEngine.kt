package com.nightpixel.sololeveling.data.gamification

import com.nightpixel.sololeveling.data.dao.RewardDao
import com.nightpixel.sololeveling.data.entity.GoldBalance
import com.nightpixel.sololeveling.data.entity.GoldTransaction

/** Spec Section 5.7 - "Habits and gym completions grant Gold in addition to stat XP (e.g. 1 Gold
 * per 10 XP)." Mirrors XpEngine's shape (a denormalized current value plus an append-only audit
 * trail - GoldBalance/GoldTransaction ~ Stat/XpLog) so Dashboard/Rewards reads stay cheap. Gold
 * grants are driven directly off the XP amount already granted for the same completion rather
 * than a second tunable number, applying the spec's stated conversion rate literally. Redeeming a
 * reward (Rewards Screen) spends Gold back via a negative transaction - the only place balance
 * moves down. */
class GoldEngine(private val rewardDao: RewardDao) {
    suspend fun grantFromXp(xpAmount: Int, source: String) = apply(xpAmount / 10, source)

    suspend fun spend(amount: Int, source: String) {
        if (amount <= 0) return
        apply(-amount, source)
    }

    private suspend fun apply(delta: Int, source: String) {
        if (delta == 0) return
        val current = rewardDao.getBalanceOnce() ?: GoldBalance()
        rewardDao.upsertBalance(current.copy(balance = current.balance + delta))
        rewardDao.insertTransaction(GoldTransaction(amount = delta, source = source))
    }
}
