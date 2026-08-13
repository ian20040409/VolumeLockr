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
     * Home tab: Fast and springy jelly squash-and-stretch bounce animation.
     */
    fun playHomeJellyAnimation(iconView: ImageView) {
        iconView.animate().cancel()
        iconView.pivotX = iconView.width / 2f
        iconView.pivotY = iconView.height * 0.95f
        iconView.rotation = 0f
        iconView.scaleX = 1f
        iconView.scaleY = 1f
        iconView.translationY = 0f

        // Stage 1: Quick squash down to charge up energy
        iconView.animate()
            .scaleX(1.24f)
            .scaleY(0.78f)
            .translationY(3f)
            .setDuration(90)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                // Stage 2: Pop up & stretch with spring overshoot
                iconView.animate()
                    .scaleX(0.88f)
                    .scaleY(1.20f)
                    .translationY(-8f)
                    .setDuration(140)
                    .setInterpolator(OvershootInterpolator(2.0f))
                    .withEndAction {
                        // Stage 3: Smooth settle landing
                        iconView.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .translationY(0f)
                            .setDuration(120)
                            .setInterpolator(DecelerateInterpolator())
                            .start()
                    }
                    .start()
            }
            .start()
    }

    /**
     * Settings tab: Fast, silky smooth gear spin with elastic snap.
     */
    fun playSettingsGearAnimation(iconView: ImageView) {
        iconView.animate().cancel()
        iconView.pivotX = iconView.width / 2f
        iconView.pivotY = iconView.height / 2f
        iconView.scaleX = 1f
        iconView.scaleY = 1f

        // Stage 1: Rapid 180-degree smooth spin with pop
        iconView.animate()
            .rotationBy(180f)
            .scaleX(1.18f)
            .scaleY(1.18f)
            .setDuration(220)
            .setInterpolator(FastOutSlowInInterpolator())
            .withEndAction {
                // Stage 2: Elastic settle
                iconView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            .start()
    }

    /**
     * TV Remote tab: Physics-based snappy pendulum wobble.
     */
    fun playTvRemoteWobbleAnimation(iconView: ImageView) {
        iconView.animate().cancel()
        iconView.pivotX = iconView.width / 2f
        iconView.pivotY = iconView.height * 0.9f
        iconView.rotation = 0f
        iconView.scaleX = 1f
        iconView.scaleY = 1f
        iconView.translationY = 0f

        // Stage 1: Fast initial tilt left with lift
        iconView.animate()
            .rotation(-22f)
            .scaleX(1.20f)
            .scaleY(1.20f)
            .translationY(-5f)
            .setDuration(100)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                // Stage 2: Swing right
                iconView.animate()
                    .rotation(18f)
                    .setDuration(90)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction {
                        // Stage 3: Rebound left
                        iconView.animate()
                            .rotation(-8f)
                            .setDuration(80)
                            .setInterpolator(DecelerateInterpolator())
                            .withEndAction {
                                // Stage 4: Settle back to center with spring
                                iconView.animate()
                                    .rotation(0f)
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .translationY(0f)
                                    .setDuration(80)
                                    .setInterpolator(OvershootInterpolator(1.5f))
                                    .start()
                            }
                            .start()
                    }
                    .start()
            }
            .start()
    }
}
