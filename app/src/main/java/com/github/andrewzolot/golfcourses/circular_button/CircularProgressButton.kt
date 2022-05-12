package com.github.andrewzolot.golfcourses.circular_button

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.os.Handler
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatButton
import android.util.AttributeSet

import br.com.simplepass.loading_button_lib.Utils
import com.github.andrewzolot.golfcourses.R


/**
 * Made by Leandro Ferreira.
 *
 */
class CircularProgressButton : AppCompatButton, AnimatedButton, CustomizableByCode {

    //private CircularAnimatedDrawable mAnimatedDrawable;
    private var mGradientDrawable: GradientDrawable? = null

    private var mIsMorphingInProgress: Boolean = false
    private var mState: State? = null
    private var mAnimatedDrawable: CircularAnimatedDrawable? = null
    private var mRevealDrawable: CircularRevealAnimatedDrawable? = null
    private var mAnimatorSet: AnimatorSet? = null

    private var mFillColorDone: Int = 0
    private var progress: Int = 0

    private var mBitmapDone: Bitmap? = null

    private var mParams: Params? = null

    private var doneWhileMorphing: Boolean = false
    private var shouldStartAnimation: Boolean = false
    private var layoutDone: Boolean = false


    /**
     * Check if button is animating
     */
    val isAnimating: Boolean?
        get() = mState == State.PROGRESS

    private enum class State {
        PROGRESS, IDLE, DONE, STOPED
    }

    /**
     *
     * @param context
     */
    constructor(context: Context) : super(context) {

        init(context, null, 0, 0)
    }

    /**
     *
     * @param context
     * @param attrs
     */
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {

        init(context, attrs, 0, 0)
    }

    /**
     *
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {

        init(context, attrs, defStyleAttr, 0)
    }

    /**
     *
     * @param context
     * @param attrs
     * @param defStyleAttr
     * @param defStyleRes
     */
    @TargetApi(23)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr) {

        init(context, attrs, defStyleAttr, defStyleRes)
    }

    /**
     * Commom initializer method.
     *
     * @param context Context
     * @param attrs Atributes passed in the XML
     */
    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        mParams = Params()

        mParams!!.mPaddingProgress = 0f

        val drawables: BackgroundAndMorphingDrawables?

        if (attrs == null) {
            drawables = loadGradientDrawable(UtilsJava.getDrawable(getContext(), R.drawable.load_button_shape_default))
        } else {
            val attrsArray = intArrayOf(android.R.attr.background)// 0

            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CircularProgressButton, defStyleAttr, defStyleRes)
            val typedArrayBG = context.obtainStyledAttributes(attrs, attrsArray, defStyleAttr, defStyleRes)

            drawables = loadGradientDrawable(typedArrayBG.getDrawable(0))

            mParams!!.mInitialCornerRadius = typedArray.getDimension(
                    R.styleable.CircularProgressButton_initialCornerAngle, 0f)
            mParams!!.mFinalCornerRadius = typedArray.getDimension(
                    R.styleable.CircularProgressButton_finalCornerAngle, 100f)
            mParams!!.mSpinningBarWidth = typedArray.getDimension(
                    R.styleable.CircularProgressButton_spinning_bar_width, 10f)
            mParams!!.mSpinningBarColor = typedArray.getColor(R.styleable.CircularProgressButton_spinning_bar_color,
                    Utils.getColorWrapper(context, android.R.color.black))
            mParams!!.mPaddingProgress = typedArray.getDimension(R.styleable.CircularProgressButton_spinning_bar_padding, 0f)

            typedArray.recycle()
            typedArrayBG.recycle()
        }

        mState = State.IDLE

        mParams!!.mText = this.text.toString()
        mParams!!.mDrawables = this.compoundDrawablesRelative

        if (drawables != null) {
            mGradientDrawable = drawables.morphingDrawable
            if (drawables.backGroundDrawable != null) {
                background = drawables.backGroundDrawable
            }
        }

        resetProgress()
    }

    override fun setBackgroundColor(color: Int) {
        mGradientDrawable!!.setColor(color)
    }

    override fun setBackgroundResource(@ColorRes resid: Int) {
        mGradientDrawable!!.setColor(ContextCompat.getColor(context, resid))
    }

    override fun setSpinningBarColor(color: Int) {
        mParams!!.mSpinningBarColor = color
        if (mAnimatedDrawable != null) {
            mAnimatedDrawable!!.setLoadingBarColor(color)
        }
    }

    override fun setSpinningBarWidth(width: Float) {
        mParams!!.mSpinningBarWidth = width
    }

    override fun setDoneColor(color: Int) {
        mParams!!.mDoneColor = color
    }

    override fun setPaddingProgress(padding: Float) {
        mParams!!.mPaddingProgress = padding
    }

    override fun setInitialHeight(height: Int) {
        mParams!!.mInitialHeight = height
    }

    override fun setInitialCornerRadius(radius: Float) {
        mParams!!.mInitialCornerRadius = radius
    }

    override fun setFinalCornerRadius(radius: Float) {
        mParams!!.mFinalCornerRadius = radius
    }

    /**
     * This method is called when the button and its dependencies are going to draw it selves.
     *
     * @param canvas Canvas
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        layoutDone = true
        if (shouldStartAnimation) {
            startAnimation()
        }

        if (mState == State.PROGRESS && !mIsMorphingInProgress) {
            drawProgress(canvas)
        } else if (mState == State.DONE) {
            drawDoneAnimation(canvas)
        }
    }

    /**
     * If the mAnimatedDrawable is null or its not running, it get created. Otherwise its draw method is
     * called here.
     *
     * @param canvas Canvas
     */
    private fun drawProgress(canvas: Canvas) {
        if (mAnimatedDrawable == null || !mAnimatedDrawable!!.isRunning) {
            mAnimatedDrawable = CircularAnimatedDrawable(this,
                    mParams!!.mSpinningBarWidth,
                    mParams!!.mSpinningBarColor)

            val offset = (width - height) / 2

            val left = offset + mParams!!.mPaddingProgress!!.toInt()
            val right = width - offset - mParams!!.mPaddingProgress!!.toInt()
            val bottom = height - mParams!!.mPaddingProgress!!.toInt()
            val top = mParams!!.mPaddingProgress!!.toInt()

            mAnimatedDrawable!!.setBounds(left, top, right, bottom)
            mAnimatedDrawable!!.callback = this
            mAnimatedDrawable!!.start()
        } else {
            mAnimatedDrawable!!.setProgress(progress)
            mAnimatedDrawable!!.draw(canvas)
        }
    }


    /**
     * @param progress set a seekbar_progress to switch displaying a determinate circular seekbar_progress
     */
    override fun setProgress(progress: Int) {
        var progress = progress
        progress = Math.max(CircularAnimatedDrawable.MIN_PROGRESS,
                Math.min(CircularAnimatedDrawable.MAX_PROGRESS, progress))
        this.progress = progress
    }

    /**
     * resets a given seekbar_progress and shows an indeterminate seekbar_progress animation
     */
    override fun resetProgress() {
        this.progress = CircularAnimatedDrawable.MIN_PROGRESS - 1
    }

    /**
     * Stops the animation and sets the button in the STOPED state.
     */
    fun stopAnimation() {
        if (mState == State.PROGRESS && !mIsMorphingInProgress) {
            mState = State.STOPED
            mAnimatedDrawable!!.stop()
        }
    }

    /**
     * Call this method when you want to show a 'completed' or a 'done' status. You have to choose the
     * color and the image to be shown. If your loading seekbar_progress ended with a success status you probrably
     * want to put a icon for "sucess" and a blue color, otherwise red and a failure icon. You can also
     * show that a music is completed... or show some status on a game... be creative!
     *
     * @param fillColor The color of the background of the button
     * @param bitmap The image that will be shown
     */
    fun doneLoadingAnimation(fillColor: Int, bitmap: Bitmap?) {
        if (mState != State.PROGRESS) {
            return
        }

        if (mIsMorphingInProgress) {
            doneWhileMorphing = true
            mFillColorDone = fillColor
            mBitmapDone = bitmap
            return
        }

        mState = State.DONE
        mAnimatedDrawable!!.stop()

        mRevealDrawable = CircularRevealAnimatedDrawable(this, fillColor, bitmap)

        val left = 0
        val right = width
        val bottom = height
        val top = 0

        mRevealDrawable!!.setBounds(left, top, right, bottom)
        mRevealDrawable!!.callback = this
        mRevealDrawable!!.start()
    }

    /**
     * Method called on the onDraw when the button is on DONE status
     *
     * @param canvas Canvas
     */
    private fun drawDoneAnimation(canvas: Canvas) {
        mRevealDrawable!!.draw(canvas)
    }

    override fun revertAnimation() {
        revertAnimation(null)
    }

    override fun revertAnimation(onAnimationEndListener: OnAnimationEndListener?) {
        if (mState == State.IDLE) {
            return
        }

        mState = State.IDLE
        resetProgress()

        if (mAnimatedDrawable != null && mAnimatedDrawable!!.isRunning) {
            stopAnimation()
        }

        if (mIsMorphingInProgress) {
            mAnimatorSet!!.cancel()
        }

        isClickable = false

        val fromWidth = width
        val fromHeight = height

        val toHeight = mParams!!.mInitialHeight!!
        val toWidth = mParams!!.mInitialWidth
        var cornerAnimation: ObjectAnimator? = null
        if (mGradientDrawable != null) {
            cornerAnimation = ObjectAnimator.ofFloat(mGradientDrawable,
                    "cornerRadius",
                    mParams!!.mFinalCornerRadius,
                    mParams!!.mInitialCornerRadius)
        }

        val widthAnimation = ValueAnimator.ofInt(fromWidth, toWidth)
        widthAnimation.addUpdateListener { valueAnimator ->
            val `val` = valueAnimator.animatedValue as Int
            val layoutParams = layoutParams
            layoutParams.width = `val`
            setLayoutParams(layoutParams)
        }

        val heightAnimation = ValueAnimator.ofInt(fromHeight, toHeight)
        heightAnimation.addUpdateListener { valueAnimator ->
            val `val` = valueAnimator.animatedValue as Int
            val layoutParams = layoutParams
            layoutParams.height = `val`
            setLayoutParams(layoutParams)
        }

        /*ValueAnimator strokeAnimation = ValueAnimator.ofFloat(
                getResources().getDimension(R.dimen.stroke_login_button),
                getResources().getDimension(R.dimen.stroke_login_button_loading));

        strokeAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {

                ((ShapeDrawable)mGradientDrawable).getPaint().setStrokeWidth((Float)animation.getAnimatedValue());
            }
        });*/

        mAnimatorSet = AnimatorSet()
        mAnimatorSet!!.duration = 300
        if (mGradientDrawable != null) {
            mAnimatorSet!!.playTogether(cornerAnimation, widthAnimation, heightAnimation)
        } else {
            mAnimatorSet!!.playTogether(widthAnimation, heightAnimation)
        }
        mAnimatorSet!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                isClickable = true
                mIsMorphingInProgress = false
                text = mParams!!.mText
                setCompoundDrawablesRelative(mParams!!.mDrawables!![0], mParams!!.mDrawables!![1], mParams!!.mDrawables!![2], mParams!!.mDrawables!![3])
                onAnimationEndListener?.onAnimationEnd()
            }
        })

        mIsMorphingInProgress = true
        mAnimatorSet!!.start()
    }

    override fun dispose() {
        if (mAnimatedDrawable != null) {
            mAnimatedDrawable!!.dispose()
        }

        if (mRevealDrawable != null) {
            mRevealDrawable!!.dispose()
        }
    }

    /**
     * Method called to start the animation. Morphs in to a ball and then starts a loading spinner.
     */
    override fun startAnimation() {
        if (mState != State.IDLE) {
            return
        }

        if (!layoutDone) {
            shouldStartAnimation = true
            return
        }

        shouldStartAnimation = false

        if (mIsMorphingInProgress) {
            mAnimatorSet!!.cancel()
        } else {
            mParams!!.mInitialWidth = width
            mParams!!.mInitialHeight = height
        }

        mState = State.PROGRESS

        mParams!!.mText = text.toString()

        this.setCompoundDrawables(null, null, null, null)
        this.text = null
        isClickable = false

        val toHeight = mParams!!.mInitialHeight!!

        val cornerAnimation = ObjectAnimator.ofFloat(mGradientDrawable,
                "cornerRadius",
                mParams!!.mInitialCornerRadius,
                mParams!!.mFinalCornerRadius)

        val widthAnimation = ValueAnimator.ofInt(mParams!!.mInitialWidth, toHeight)
        widthAnimation.addUpdateListener { valueAnimator ->
            val `val` = valueAnimator.animatedValue as Int
            val layoutParams = layoutParams
            layoutParams.width = `val`
            setLayoutParams(layoutParams)
        }

        val heightAnimation = ValueAnimator.ofInt(mParams!!.mInitialHeight!!, toHeight)
        heightAnimation.addUpdateListener { valueAnimator ->
            val `val` = valueAnimator.animatedValue as Int
            val layoutParams = layoutParams
            layoutParams.height = `val`
            setLayoutParams(layoutParams)
        }

        /*ValueAnimator strokeAnimation = ValueAnimator.ofFloat(
                getResources().getDimension(R.dimen.stroke_login_button),
                getResources().getDimension(R.dimen.stroke_login_button_loading));

        strokeAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {

                ((ShapeDrawable)mGradientDrawable).getPaint().setStrokeWidth((Float)animation.getAnimatedValue());
            }
        });*/

        mAnimatorSet = AnimatorSet()
        mAnimatorSet!!.duration = 300
        mAnimatorSet!!.playTogether(cornerAnimation, widthAnimation, heightAnimation)
        mAnimatorSet!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mIsMorphingInProgress = false

                if (doneWhileMorphing) {
                    doneWhileMorphing = false

                    val runnable = Runnable { doneLoadingAnimation(mFillColorDone, mBitmapDone) }

                    Handler().postDelayed(runnable, 50)
                }
            }
        })

        mIsMorphingInProgress = true
        mAnimatorSet!!.start()
    }

    /**
     * Class with all the params to configure the button.
     */
    private inner class Params {
        var mSpinningBarWidth: Float = 0.toFloat()
        var mSpinningBarColor: Int = 0
        var mDoneColor: Int = 0
        var mPaddingProgress: Float? = null
        var mInitialHeight: Int? = null
        var mInitialWidth: Int = 0
        var mText: String? = null
        var mInitialCornerRadius: Float = 0.toFloat()
        var mFinalCornerRadius: Float = 0.toFloat()
        var mDrawables: Array<Drawable>? = null
    }

    internal class BackgroundAndMorphingDrawables {
        var backGroundDrawable: Drawable? = null
        var morphingDrawable: GradientDrawable? = null

        fun setBothDrawables(drawable: GradientDrawable) {
            this.backGroundDrawable = drawable
            this.morphingDrawable = drawable
        }
    }

    companion object {

        /**
         * finds or creates the drawable for the morphing animation and the drawable to set the background to
         *
         * @param drawable Drawable set with android:background setting
         * @return BackgroundAndMorphingDrawables object holding the Drawable to morph and to set a background
         */
        internal fun loadGradientDrawable(drawable: Drawable?): BackgroundAndMorphingDrawables? {
            var mGradientDrawable: BackgroundAndMorphingDrawables? = BackgroundAndMorphingDrawables()

            if (drawable == null)
                return null
            else {
                if (drawable is GradientDrawable) {
                    mGradientDrawable!!.setBothDrawables(drawable as GradientDrawable)
                } else if (drawable is ColorDrawable) {
                    val colorDrawable = drawable as ColorDrawable?
                    val gradientDrawable = GradientDrawable()
                    gradientDrawable.setColor(colorDrawable!!.color)
                    mGradientDrawable!!.setBothDrawables(gradientDrawable)
                } else if (drawable is InsetDrawable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    val insetDrawable = drawable as InsetDrawable?
                    mGradientDrawable = loadGradientDrawable(insetDrawable!!.drawable)
                    // use the original inset as background to keep margins, and use the inner drawable for morphing
                    mGradientDrawable!!.backGroundDrawable = insetDrawable
                } else if (drawable is StateListDrawable) {
                    val stateListDrawable = drawable as StateListDrawable?
                    //try to get the drawable for an active, enabled, unpressed button
                    stateListDrawable!!.state = intArrayOf(android.R.attr.state_enabled, android.R.attr.state_active, -android.R.attr.state_pressed)
                    val current = stateListDrawable.current
                    mGradientDrawable = loadGradientDrawable(current)

                }
                if (mGradientDrawable!!.morphingDrawable == null) {
                    throw RuntimeException("Error reading background... Use a shape or a color in xml!")
                }
            }

            return mGradientDrawable
        }
    }
}
