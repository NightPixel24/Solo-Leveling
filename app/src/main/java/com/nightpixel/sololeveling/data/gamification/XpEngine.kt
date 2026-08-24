package com.nightpixel.sololeveling.data.gamification

import com.nightpixel.sololeveling.data.dao.StatDao
import com.nightpixel.sololeveling.data.entity.Stat
import com.nightpixel.sololeveling.data.entity.StatTag
import com.nightpixel.sololeveling.data.entity.XpLog
import kotlin.math.pow
import kotlin.math.roundToInt

const val MAX_STAT_LEVEL = 99

/** Spec Section 5.2 - XP required to advance from [level] to the next one. */
fun xpForLevel(level: Int): Int = (50.0 * level.toDouble().pow(1.2)).roundToInt()

/** Grants XP to a stat (spec Section 5.2's example amounts - explicitly "tunable"), rolling
 * levels up as thresholds are crossed, and records an [XpLog] row as an audit trail. Grants are
 * one-directional: nothing in the spec calls for clawing XP back when a habit/task is unchecked,
 * so callers only invoke this on a false->true completion transition. */
class XpEngine(private val statDao: StatDao) {
    suspend fun grant(tag: StatTag, amount: Int, source: String) {
        if (amount <= 0) return
        val current = statDao.getStat(tag) ?: Stat(tag = tag)
        var level = current.level
        var xp = current.currentXp + amount
        while (level < MAX_STAT_LEVEL) {
            val needed = xpForLevel(level)
            if (xp < needed) break
            xp -= needed
            level++
        }
        statDao.upsertStat(current.copy(level = level, currentXp = xp))
        statDao.insertXpLog(XpLog(statTag = tag, amount = amount, source = source))
    }
}
