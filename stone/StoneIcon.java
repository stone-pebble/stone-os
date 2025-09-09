package com.android.systemui.stone;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * StoneIcon - The 🗿 icon that appears at the bottom of the screen.
 * This icon serves as the entry point to the Stone assistant.
 * Users can swipe up from this icon to reveal the Stone panel.
 */
public class StoneIcon extends View {
    private static final String TAG = "StoneIcon";
    private static final int ICON_SIZE = 48; // dp
    private static final int TOUCH_AREA_SIZE = 64; // dp
    
    private Paint mPaint;
    private Path mStonePath;
    private RectF mBounds;
    private boolean mIsPressed = false;
    private OnSwipeUpListener mSwipeUpListener;
    private float mTouchStartY;
    private float mTouchStartTime;
    
    // Interface for swipe up detection
    public interface OnSwipeUpListener {
        void onSwipeUp();
    }
    
    public StoneIcon(Context context) {
        super(context);
        init();
    }
    
    public StoneIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public StoneIcon(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.parseColor("#808080")); // Gray color
        mPaint.setStyle(Paint.Style.FILL);
        
        // Create the stone moai emoji path
        mStonePath = new Path();
        mBounds = new RectF();
        
        // Set minimum touch area
        int touchAreaPx = dpToPx(TOUCH_AREA_SIZE);
        setMinimumWidth(touchAreaPx);
        setMinimumHeight(touchAreaPx);
        
        // Make it clickable
        setClickable(true);
        setFocusable(true);
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        // Update bounds for the icon
        int iconSizePx = dpToPx(ICON_SIZE);
        float centerX = w / 2f;
        float centerY = h / 2f;
        
        mBounds.set(
            centerX - iconSizePx / 2f,
            centerY - iconSizePx / 2f,
            centerX + iconSizePx / 2f,
            centerY + iconSizePx / 2f
        );
        
        // Create simplified moai stone shape
        createMoaiPath();
    }
    
    private void createMoaiPath() {
        mStonePath.reset();
        
        float width = mBounds.width();
        float height = mBounds.height();
        float left = mBounds.left;
        float top = mBounds.top;
        
        // Create a simplified moai-like shape
        // Head
        mStonePath.moveTo(left + width * 0.3f, top);
        mStonePath.lineTo(left + width * 0.7f, top);
        mStonePath.lineTo(left + width * 0.8f, top + height * 0.2f);
        mStonePath.lineTo(left + width * 0.8f, top + height * 0.5f);
        
        // Nose bump
        mStonePath.lineTo(left + width * 0.85f, top + height * 0.55f);
        mStonePath.lineTo(left + width * 0.85f, top + height * 0.6f);
        mStonePath.lineTo(left + width * 0.8f, top + height * 0.65f);
        
        // Bottom
        mStonePath.lineTo(left + width * 0.75f, top + height * 0.9f);
        mStonePath.lineTo(left + width * 0.25f, top + height * 0.9f);
        mStonePath.lineTo(left + width * 0.2f, top + height * 0.65f);
        
        // Other side nose
        mStonePath.lineTo(left + width * 0.15f, top + height * 0.6f);
        mStonePath.lineTo(left + width * 0.15f, top + height * 0.55f);
        mStonePath.lineTo(left + width * 0.2f, top + height * 0.5f);
        
        mStonePath.lineTo(left + width * 0.2f, top + height * 0.2f);
        mStonePath.close();
        
        // Add eyes (as separate rectangles)
        // These will be drawn as separate shapes
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Draw shadow if pressed
        if (mIsPressed) {
            Paint shadowPaint = new Paint(mPaint);
            shadowPaint.setAlpha(128);
            canvas.save();
            canvas.translate(2, 2);
            canvas.drawPath(mStonePath, shadowPaint);
            canvas.restore();
        }
        
        // Draw the main stone shape
        canvas.drawPath(mStonePath, mPaint);
        
        // Draw eyes
        Paint eyePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        eyePaint.setColor(Color.parseColor("#404040")); // Darker gray for eyes
        eyePaint.setStyle(Paint.Style.FILL);
        
        float eyeWidth = mBounds.width() * 0.15f;
        float eyeHeight = mBounds.height() * 0.2f;
        float eyeY = mBounds.top + mBounds.height() * 0.35f;
        
        // Left eye
        RectF leftEye = new RectF(
            mBounds.left + mBounds.width() * 0.25f,
            eyeY,
            mBounds.left + mBounds.width() * 0.25f + eyeWidth,
            eyeY + eyeHeight
        );
        canvas.drawRect(leftEye, eyePaint);
        
        // Right eye
        RectF rightEye = new RectF(
            mBounds.left + mBounds.width() * 0.6f,
            eyeY,
            mBounds.left + mBounds.width() * 0.6f + eyeWidth,
            eyeY + eyeHeight
        );
        canvas.drawRect(rightEye, eyePaint);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mIsPressed = true;
                mTouchStartY = event.getY();
                mTouchStartTime = System.currentTimeMillis();
                invalidate();
                return true;
                
            case MotionEvent.ACTION_UP:
                mIsPressed = false;
                invalidate();
                
                // Check for swipe up
                float deltaY = mTouchStartY - event.getY();
                long deltaTime = System.currentTimeMillis() - (long)mTouchStartTime;
                
                if (deltaY > dpToPx(50) && deltaTime < 500) {
                    // Swipe up detected
                    if (mSwipeUpListener != null) {
                        mSwipeUpListener.onSwipeUp();
                    }
                } else if (Math.abs(deltaY) < dpToPx(10)) {
                    // Regular tap
                    performClick();
                }
                return true;
                
            case MotionEvent.ACTION_CANCEL:
                mIsPressed = false;
                invalidate();
                return true;
        }
        
        return super.onTouchEvent(event);
    }
    
    @Override
    public boolean performClick() {
        super.performClick();
        // Handle regular tap - could also open Stone panel
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
    
    /**
     * Convert dp to pixels
     */
    private int dpToPx(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    
    /**
     * Set the icon color
     */
    public void setIconColor(int color) {
        mPaint.setColor(color);
        invalidate();
    }
    
    /**
     * Animate the icon (e.g., pulse when Stone is listening)
     */
    public void startPulseAnimation() {
        animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(500)
            .withEndAction(() -> {
                animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(500)
                    .start();
            })
            .start();
    }
}