package com.chickierunway.game

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * The ONLY place that references in-game image resources. Replace files in res/drawable-nodpi
 * with the same names and nothing else needs to change.
 */
object Assets {
    @DrawableRes val bgMain: Int = R.drawable.bg_main
    @DrawableRes val logo: Int = R.drawable.logo

    /** Mascots: a = happy jump, b = wink / cheer, c = surprised, d = running. */
    @DrawableRes val mascotA: Int = R.drawable.mascot_a
    @DrawableRes val mascotB: Int = R.drawable.mascot_b
    @DrawableRes val mascotC: Int = R.drawable.mascot_c
    @DrawableRes val mascotD: Int = R.drawable.mascot_d

    /** Ornate framed plate (main menu) and the simple long plate (all other buttons). */
    @DrawableRes val btnMenu: Int = R.drawable.btn_menu
    @DrawableRes val btnPlate: Int = R.drawable.btn_plate
    @DrawableRes val plateRound: Int = R.drawable.plate_round
    @DrawableRes val iconBack: Int = R.drawable.icon_back
    @DrawableRes val iconPause: Int = R.drawable.icon_pause

    /** Width / height of btn_menu — keep in sync if replacement art changes proportions. */
    const val BTN_MENU_ASPECT: Float = 720f / 244f

    /**
     * Plates are drawn as 3-slice (left cap | stretched middle | right cap) so they can be any
     * width without distorting the rounded ends. Cap width = plate height * this factor.
     */
    const val PLATE_CAP_FACTOR: Float = 0.62f

    /** Inner (dark) area inset of the plates, as a fraction of the plate height. */
    const val PLATE_INNER_INSET: Float = 0.2f

    /** Matching-pairs / decoration icons, index 0..17. */
    val items: List<Int> = listOf(
        R.drawable.item_01, R.drawable.item_02, R.drawable.item_03, R.drawable.item_04,
        R.drawable.item_05, R.drawable.item_06, R.drawable.item_07, R.drawable.item_08,
        R.drawable.item_09, R.drawable.item_10, R.drawable.item_11, R.drawable.item_12,
        R.drawable.item_13, R.drawable.item_14, R.drawable.item_15, R.drawable.item_16,
        R.drawable.item_17, R.drawable.item_18
    )

    /** Text alternatives for the item icons (accessibility), as string resources. Same order as [items]. */
    val itemNames: List<Int> = listOf(
        R.string.item_01, R.string.item_02, R.string.item_03, R.string.item_04, R.string.item_05,
        R.string.item_06, R.string.item_07, R.string.item_08, R.string.item_09, R.string.item_10,
        R.string.item_11, R.string.item_12, R.string.item_13, R.string.item_14, R.string.item_15,
        R.string.item_16, R.string.item_17, R.string.item_18
    )

    @DrawableRes
    fun item(index: Int): Int = items.getOrElse(index) { items.first() }

    @StringRes
    fun itemName(index: Int): Int = itemNames.getOrElse(index) { R.string.item_unknown }
}
