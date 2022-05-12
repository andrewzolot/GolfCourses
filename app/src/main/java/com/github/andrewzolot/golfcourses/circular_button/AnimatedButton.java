package com.github.andrewzolot.golfcourses.circular_button;


/**
 * Created by hinovamobile on 23/12/16.
 */

public interface AnimatedButton {
    void startAnimation();
    void revertAnimation();
    void revertAnimation(final OnAnimationEndListener onAnimationEndListener);
    void dispose();
    void setProgress(int progress);
    void resetProgress();
}
