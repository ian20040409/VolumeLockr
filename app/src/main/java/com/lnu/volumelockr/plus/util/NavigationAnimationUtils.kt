package com.lnu.volumelockr.plus.util

import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.lnu.volumelockr.plus.R

/**
 * Handles high-performance, hardware-accelerated 60/120fps micro-interactions for navigation components.
 */
object NavigationAnimationUtils {

    /**
     * Triggers the appropriate animation and haptic feedback for a selected navigation item.
     */
    fun animateNavigationItem(itemId: Int, itemView: View?) {
        if (itemView == null) return

        itemView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

        // Animate the label text with a synchronized micro-pulse
        val labelGroup = itemView.findViewById<View>(
            com.google.android.material.R.id.navigation_bar_item_labels_group
        )
        labelGroup?.let { label ->
            label.animate().cancel()
            label.scaleX = 1f
            label.scaleY = 1f
            label.translationY = 0f
            label.animate()
                .scaleX(1.12f)
                .scaleY(1.12f)
                .translationY(-2f)
                .setDuration(120)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    label.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .translationY(0f)
                        .setDuration(120)
                        .setInterpolator(OvershootInterpolator(2.0f))
                        .start()
                }
                .start()
        }

        val iconView = itemView.findViewById<ImageView>(
            com.google.android.material.R.id.navigation_bar_item_icon_view
        ) ?: return

        when (itemId) {
            R.id.volumeSliderFragment -> playHomeJellyAnimation(iconView)
            R.id.settingsFragment -> playSettingsGearAnimation(iconView)
            R.id.tvRemoteFragment -> playTvRemoteWobbleAnimation(iconView)
        }
    }

    /**
     * Home tab: Dynamic cartoon-style jelly squash-and-stretch jump animation.
     */
    fun playHomeJellyAnimation(iconView: ImageView) {
        iconView.animate().cancel()
        iconView.pivotX = iconView.width / 2f
        iconView.pivotY = iconView.height * 0.95f
        iconView.rotation = 0f
        iconView.scaleX = 1f
        iconView.scaleY = 1f
        iconView.translationY = 0f

        // Stage 1: Anticipation squash down
        iconView.animate()
            .scaleX(1.38f)
            .scaleY(0.65f)
            .translationY(4f)
            .setDuration(80)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                // Stage 2: Explosive spring stretch & jump upward
                iconView.animate()
                    .scaleX(0.78f)
                    .scaleY(1.35f)
                    .translationY(-14f)
                    .setDuration(130)
                    .setInterpolator(OvershootInterpolator(2.5f))
                    .withEndAction {
                        // Stage 3: Landing bounce
                        iconView.animate()
                            .scaleX(1.14f)
                            .scaleY(0.90f)
                            .translationY(2f)
                            .setDuration(90)
                            .setInterpolator(DecelerateInterpolator())
                            .withEndAction {
                                // Stage 4: Settle to rest
                                iconView.animate()
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .translationY(0f)
                                    .setDuration(90)
                                    .setInterpolator(OvershootInterpolator(1.8f))
                                    .start()
                            }
                            .start()
                    }
                    .start()
            }
            .start()
    }

    /**
     * Settings tab: Mechanical wind-up click + accelerating turbo 360 spin + spring recoil.
     */
    fun playSettingsGearAnimation(iconView: ImageView) {
        iconView.animate().cancel()
        iconView.pivotX = iconView.width / 2f
        iconView.pivotY = iconView.height / 2f
        iconView.rotation = 0f
        iconView.scaleX = 1f
        iconView.scaleY = 1f
        iconView.translationY = 0f

        // Stage 1: Wind-up counter-clockwise anticipation
        iconView.animate()
            .rotation(-35f)
            .scaleX(0.85f)
            .scaleY(0.85f)
            .translationY(2f)
            .setDuration(80)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                // Stage 2: Rapid momentum turbo spin past 360° to 390°
                iconView.animate()
                    .rotation(390f)
                    .scaleX(1.28f)
                    .scaleY(1.28f)
                    .translationY(-4f)
                    .setDuration(180)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .withEndAction {
                        // Stage 3: Snap back to 360° (0°) with elastic mechanical lock
                        iconView.rotation = 30f // Equivalent angle for smooth return to 0
                        iconView.animate()
                            .rotation(0f)
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .translationY(0f)
                            .setDuration(110)
                            .setInterpolator(OvershootInterpolator(3.0f))
                            .start()
                    }
                    .start()
            }
            .start()
    }

    /**
     * TV Remote tab: Expressive pendulum sway & lateral infrared beam broadcast wobble.
     */
    fun playTvRemoteWobbleAnimation(iconView: ImageView) {
        iconView.animate().cancel()
        iconView.pivotX = iconView.width / 2f
        iconView.pivotY = iconView.height * 0.9f
        iconView.rotation = 0f
        iconView.scaleX = 1f
        iconView.scaleY = 1f
        iconView.translationX = 0f
        iconView.translationY = 0f

        // Stage 1: Energetic left swing with elevation and scale-up
        iconView.animate()
            .rotation(-28f)
            .scaleX(1.28f)
            .scaleY(1.28f)
            .translationX(-4f)
            .translationY(-8f)
            .setDuration(90)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                // Stage 2: Whip right
                iconView.animate()
                    .rotation(24f)
                    .translationX(4f)
                    .translationY(-6f)
                    .setDuration(85)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction {
                        // Stage 3: Counter-rebound left
                        iconView.animate()
                            .rotation(-12f)
                            .translationX(-2f)
                            .translationY(-2f)
                            .setDuration(75)
                            .setInterpolator(DecelerateInterpolator())
                            .withEndAction {
                                // Stage 4: Spring snap to center
                                iconView.animate()
                                    .rotation(0f)
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .translationX(0f)
                                    .translationY(0f)
                                    .setDuration(80)
                                    .setInterpolator(OvershootInterpolator(2.0f))
                                    .start()
                            }
                            .start()
                    }
                    .start()
            }
            .start()
    }
}
