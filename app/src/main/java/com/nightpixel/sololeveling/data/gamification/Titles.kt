package com.nightpixel.sololeveling.data.gamification

import com.nightpixel.sololeveling.data.entity.Stat
import com.nightpixel.sololeveling.data.entity.StatTag

/** Not in the spec - a user-requested "name + title" system (2026-08-26 feedback) replacing the
 * old plain "Rank" label on the Dashboard. Titles are never stored, only the *choice* of which
 * one is equipped ([com.nightpixel.sololeveling.data.entity.PlayerProfile.equippedTitleId]) -
 * every title itself is computed live from Rank/Stat data, the same "derive, don't store"
 * approach Rank/Quests/Rewards already use. Copy is this app's own invention (the spec names no
 * titles at all), themed to match the Solo Leveling "Hunter"/"Monarch" flavor the whole app
 * already borrows for its dark/blue/violet System-window look. */
data class Title(val id: String, val displayName: String)

private val RANK_TITLES: Map<RankTier, Title> = mapOf(
    RankTier.E to Title("RANK_E", "E-Rank Hunter"),
    RankTier.D to Title("RANK_D", "D-Rank Hunter"),
    RankTier.C to Title("RANK_C", "C-Rank Hunter"),
    RankTier.B to Title("RANK_B", "B-Rank Hunter"),
    RankTier.A to Title("RANK_A", "A-Rank Hunter"),
    RankTier.S to Title("RANK_S", "S-Rank Hunter"),
    RankTier.SS to Title("RANK_SS", "National Level Hunter")
)

private data class StatTierDef(val minLevel: Int, val name: String)

private val STAT_TIERS: Map<StatTag, List<StatTierDef>> = mapOf(
    StatTag.STR to listOf(
        StatTierDef(10, "Novice Brawler"),
        StatTierDef(25, "Iron Fist"),
        StatTierDef(50, "Berserker"),
        StatTierDef(75, "Titan's Strength"),
        StatTierDef(99, "Monarch of Strength")
    ),
    StatTag.VIT to listOf(
        StatTierDef(10, "Steady Heart"),
        StatTierDef(25, "Iron Skin"),
        StatTierDef(50, "Unbreakable"),
        StatTierDef(75, "Undying Will"),
        StatTierDef(99, "Monarch of Vitality")
    ),
    StatTag.DISCIPLINE to listOf(
        StatTierDef(10, "Focused Mind"),
        StatTierDef(25, "Iron Resolve"),
        StatTierDef(50, "Unshaken"),
        StatTierDef(75, "Relentless"),
        StatTierDef(99, "Monarch of Discipline")
    ),
    StatTag.INT to listOf(
        StatTierDef(10, "Quick Thinker"),
        StatTierDef(25, "Strategist"),
        StatTierDef(50, "Tactician"),
        StatTierDef(75, "Mastermind"),
        StatTierDef(99, "Monarch of Intellect")
    ),
    StatTag.SPIRITUALITY to listOf(
        StatTierDef(10, "Seeker"),
        StatTierDef(25, "Faithful Servant"),
        StatTierDef(50, "Steadfast in Faith"),
        StatTierDef(75, "Prayer Warrior"),
        StatTierDef(99, "Monarch of Spirit")
    )
)

private fun statTitleId(tag: StatTag, minLevel: Int) = "${tag.name}_$minLevel"

/** Every title currently unlocked - all Rank titles at or below the current rank, plus every
 * per-stat tier at or below that stat's current level (not just the highest one for each), so
 * older titles stay available to equip rather than disappearing once you outlevel them. */
fun unlockedTitles(rank: RankTier, stats: List<Stat>): List<Title> {
    val rankTitles = RankTier.entries.filter { it.ordinal <= rank.ordinal }.map { RANK_TITLES.getValue(it) }
    val statTitles = stats.flatMap { stat ->
        STAT_TIERS[stat.tag].orEmpty()
            .filter { stat.level >= it.minLevel }
            .map { Title(statTitleId(stat.tag, it.minLevel), it.name) }
    }
    return rankTitles + statTitles
}

/** The title shown when no title is equipped, or the equipped one is no longer unlocked
 * (shouldn't normally happen since titles only ever unlock, never lock again - but a safe
 * fallback costs nothing). */
fun defaultTitle(rank: RankTier): Title = RANK_TITLES.getValue(rank)

fun titleById(id: String?, rank: RankTier, stats: List<Stat>): Title =
    id?.let { targetId -> unlockedTitles(rank, stats).find { it.id == targetId } } ?: defaultTitle(rank)
