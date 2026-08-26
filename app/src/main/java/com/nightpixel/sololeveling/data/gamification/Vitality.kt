package com.nightpixel.sololeveling.data.gamification

import com.nightpixel.sololeveling.data.dao.FoodDao
import com.nightpixel.sololeveling.data.entity.FoodRating
import kotlin.math.roundToInt

/** Not in the spec - a user-requested mechanic (2026-08-26 feedback): eating unhealthy food
 * repeatedly puts VIT XP grants into a reduced-gain mode. Simplified to a fixed rolling window
 * (derive, don't store - same as every other computed value in this app) rather than a stateful
 * "streak since last Healthy entry": if your most recent [WINDOW] logged meals are *all*
 * UNHEALTHY, VIT grants are halved; logging anything else (OK or HEALTHY) immediately changes
 * that window and lifts it. */
private const val WINDOW = 3
const val LOW_VITALITY_XP_MULTIPLIER = 0.5

fun isLowVitalityMode(recentRatingsNewestFirst: List<FoodRating>): Boolean {
    val recent = recentRatingsNewestFirst.take(WINDOW)
    return recent.size == WINDOW && recent.all { it == FoodRating.UNHEALTHY }
}

/** Applies [isLowVitalityMode] to a base VIT XP amount - call at every VIT grant site
 * (water goal, food logged, VIT-tagged habit) so the debuff is felt everywhere VIT is earned,
 * not just from food logging itself. */
suspend fun applyVitalityMultiplier(foodDao: FoodDao, baseAmount: Int): Int {
    val recent = foodDao.getRecentOnce(WINDOW).map { it.rating }
    return if (isLowVitalityMode(recent)) (baseAmount * LOW_VITALITY_XP_MULTIPLIER).roundToInt() else baseAmount
}
