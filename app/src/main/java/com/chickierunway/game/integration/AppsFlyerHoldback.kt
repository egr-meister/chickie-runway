package com.chickierunway.game.integration

import kotlin.random.Random

/**
 * Holdback group: roughly one install in [RATIO] never initialises AppsFlyer at all.
 *
 * The roll happens once per install and is persisted by IntegrationStorage - re-rolling on
 * every launch would flip the same user between groups and poison both samples.
 */
object AppsFlyerHoldback {

    /** 1 in 5. */
    const val RATIO = 5

    /** Query key on the offer URL. Change here only. */
    const val PARAM = "sub_id_15"

    const val VALUE_HOLDBACK = "holdback"
    const val VALUE_ACTIVE = "active"

    fun roll(random: Random = Random.Default): Boolean = random.nextInt(RATIO) == 0

    /**
     * Sent on every request, not only for the holdback group: "AppsFlyer was off" and
     * "AppsFlyer was on but did not answer" both arrive without af_status, and only this
     * value tells them apart (active + no af_status = no answer).
     */
    fun label(holdback: Boolean): String = if (holdback) VALUE_HOLDBACK else VALUE_ACTIVE
}
