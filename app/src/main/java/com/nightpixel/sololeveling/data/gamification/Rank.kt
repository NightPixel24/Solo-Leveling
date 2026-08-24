package com.nightpixel.sololeveling.data.gamification

import com.nightpixel.sololeveling.data.entity.Goal
import com.nightpixel.sololeveling.data.entity.GoalStatus
import com.nightpixel.sololeveling.data.entity.GoalTier

/** Spec Section 5.3 - E through SS, driven by completed Life Goals rather than XP; a separate
 * "life trajectory" track from the Stat/XP "daily grind" in [xpForLevel]. Not persisted anywhere -
 * there's no `Rank` table in spec Section 9's data model, so it's recomputed from Goal data every
 * time it's needed, the same way habit streaks are derived from HabitLog rather than stored. */
enum class RankTier(val label: String) {
    E("E"), D("D"), C("C"), B("B"), A("A"), S("S"), SS("SS")
}

private val TIER_TO_RANK = mapOf(
    GoalTier.ONE_MONTH to RankTier.D,
    GoalTier.THREE_MONTH to RankTier.C,
    GoalTier.SIX_MONTH to RankTier.B,
    GoalTier.YEARLY to RankTier.A,
    GoalTier.FIVE_YEAR to RankTier.S,
    GoalTier.TEN_YEAR to RankTier.SS,
    GoalTier.LIFETIME to RankTier.SS
)

/** Default rule (spec Section 5.3): completing just one goal at a tier unlocks that tier's rank,
 * so rank is simply the highest rank among all completed goals' tiers - not a strict step-by-step
 * unlock, so e.g. completing a Yearly goal before any 1-Month/3-Month/6-Month goal jumps straight
 * to A. The spec also floats an all-goals-at-a-tier alternative as a future Settings toggle;
 * deferred until there's real usage data to justify the extra complexity. */
fun computeRank(goals: List<Goal>): RankTier =
    goals.asSequence()
        .filter { it.status == GoalStatus.COMPLETED }
        .mapNotNull { TIER_TO_RANK[it.tier] }
        .maxByOrNull { it.ordinal }
        ?: RankTier.E
