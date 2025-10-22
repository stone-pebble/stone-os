package com.android.systemui.stone;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * StoneIcon - The 🗿 icon that appears at the bottom of the screen.
 * This icon serves as the entry point to the Stone assistant.
 * Users can swipe up from this icon to reveal the Stone panel.
 */
public class StoneIcon extends View {
    private static final String TAG = "StoneIcon";
    private static final int ICON_SIZE_DP = 64; // 64dp for touch target
    private static final int SWIPE_THRESHOLD_VELOCITY = 1000; // pixels per second

    private Paint mStonePaint;
    private Paint mEyePaint;
    private RectF mBounds;
    private OnSwipeUpListener mSwipeUpListener;
    private GestureDetector mGestureDetector;
    private boolean mIsPressed = false;

    /**
     * Interface for swipe up detection
     */
    public interface OnSwipeUpListener {
        void onSwipeUp();
    }

    public StoneIcon(Context context) {
        super(context);
        init(context);
    }

    public StoneIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StoneIcon(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // Initialize paint for stone shape
        mStonePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mStonePaint.setColor(Color.parseColor("#808080")); // Gray stone color
        mStonePaint.setStyle(Paint.Style.FILL);

        // Initialize paint for eyes
        mEyePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mEyePaint.setColor(Color.WHITE);
        mEyePaint.setStyle(Paint.Style.FILL);

        mBounds = new RectF();

        // Initialize GestureDetector with custom listener
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                // Detect swipe-up: negative velocityY (upward) with significant velocity
                if (velocityY < -SWIPE_THRESHOLD_VELOCITY) {
                    if (mSwipeUpListener != null) {
                        mSwipeUpListener.onSwipeUp();
                    }
                    return true;
                }
                return false;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                // Must return true to continue gesture detection
                return true;
            }
        });

        setClickable(true);
        setFocusable(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Calculate 64dp in pixels
        float density = getContext().getResources().getDisplayMetrics().density;
        int sizePx = Math.round(ICON_SIZE_DP * density);

        // Set measured dimensions to 64dp x 64dp
        setMeasuredDimension(sizePx, sizePx);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Add some padding for the icon drawing
        float padding = Math.min(w, h) * 0.1f;
        mBounds.set(padding, padding, w - padding, h - padding);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the stone shape (simple rounded rectangle)
        float cornerRadius = mBounds.width() * 0.15f;
        canvas.drawRoundRect(mBounds, cornerRadius, cornerRadius, mStonePaint);

        // Draw two white eyes
        float eyeWidth = mBounds.width() * 0.15f;
        float eyeHeight = mBounds.height() * 0.12f;
        float eyeY = mBounds.centerY() - eyeHeight / 2;

        // Left eye
        float leftEyeX = mBounds.left + mBounds.width() * 0.3f;
        canvas.drawCircle(leftEyeX, eyeY, eyeWidth / 2, mEyePaint);

        // Right eye
        float rightEyeX = mBounds.left + mBounds.width() * 0.7f;
        canvas.drawCircle(rightEyeX, eyeY, eyeWidth / 2, mEyePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Pass all motion events to GestureDetector
        boolean gestureHandled = mGestureDetector.onTouchEvent(event);

        // Handle press visual feedback
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mIsPressed = true;
                // Darken the stone color using ColorFilter
                mStonePaint.setColorFilter(new LightingColorFilter(0xFF666666, 0x00000000));
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsPressed = false;
                // Clear ColorFilter to return to normal state
                mStonePaint.setColorFilter(null);
                invalidate();

                if (event.getAction() == MotionEvent.ACTION_UP && !gestureHandled) {
                    // Handle as a click if gesture was not a fling
                    performClick();
                }
                break;
        }

        return gestureHandled || super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        // Treat regular click as swipe-up as well
        if (mSwipeUpListener != null) {
            mSwipeUpListener.onSwipeUp();
        }
        return true;
    }

    /**
     * Set the listener for swipe up gestures
     */
    public void setOnSwipeUpListener(OnSwipeUpListener listener) {
        mSwipeUpListener = listener;
    }
}
