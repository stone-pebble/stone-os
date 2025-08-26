package com.android.systemui.stone;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * StonePanel - The sliding chat interface that takes up 1/3 of the screen
 * when activated. This panel slides up from the bottom when the user
 * swipes up from the Stone icon.
 */
public class StonePanel extends FrameLayout {
    private static final String TAG = "StonePanel";
    private static final float PANEL_HEIGHT_RATIO = 0.33f; // 1/3 of screen
    private static final int ANIMATION_DURATION = 300; // milliseconds
    private static final String STONE_CHAT_URL = "http://localhost:8080/chat";
    
    private WebView mChatWebView;
    private View mHandleBar;
    private TextView mStatusText;
    private GestureDetector mGestureDetector;
    private boolean mIsExpanded = false;
    private int mScreenHeight;
    private WindowManager mWindowManager;
    
    public StonePanel(Context context) {
        super(context);
        init(context);
    }
    
    public StonePanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public StonePanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);
        mScreenHeight = metrics.heightPixels;
        
        // Set up the layout
        setBackgroundColor(Color.parseColor("#1a1a1a")); // Dark gray background
        setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (int)(mScreenHeight * PANEL_HEIGHT_RATIO)
        ));
        
        // Create the main container
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        
        // Create handle bar for dragging
        mHandleBar = new View(context);
        mHandleBar.setBackgroundColor(Color.parseColor("#4a4a4a"));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(
            120, // width
            8   // height
        );
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.topMargin = 12;
        handleParams.bottomMargin = 12;
        mHandleBar.setLayoutParams(handleParams);
        container.addView(mHandleBar);
        
        // Create status text
        mStatusText = new TextView(context);
        mStatusText.setText("Stone Assistant");
        mStatusText.setTextColor(Color.WHITE);
        mStatusText.setTextSize(16);
        mStatusText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        statusParams.bottomMargin = 8;
        mStatusText.setLayoutParams(statusParams);
        container.addView(mStatusText);
        
        // Create WebView for chat interface
        mChatWebView = new WebView(context);
        mChatWebView.setBackgroundColor(Color.parseColor("#1a1a1a"));
        mChatWebView.getSettings().setJavaScriptEnabled(true);
        mChatWebView.getSettings().setDomStorageEnabled(true);
        mChatWebView.setWebViewClient(new WebViewClient());
        LinearLayout.LayoutParams webViewParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1.0f // Take remaining space
        );
        mChatWebView.setLayoutParams(webViewParams);
        container.addView(mChatWebView);
        
        // Load the chat interface
        mChatWebView.loadUrl(STONE_CHAT_URL);
        
        addView(container);
        
        // Set up gesture detection
        setupGestureDetection();
        
        // Initially hide the panel below the screen
        setTranslationY(mScreenHeight);
    }
    
    private void setupGestureDetection() {
        mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                
                float deltaY = e2.getY() - e1.getY();
                
                // Swipe down to hide panel
                if (deltaY > 100 && Math.abs(velocityY) > 100) {
                    hide();
                    return true;
                }
                // Swipe up to show panel
                else if (deltaY < -100 && Math.abs(velocityY) > 100) {
                    show();
                    return true;
                }
                
                return false;
            }
        });
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }
    
    /**
     * Show the Stone panel with animation
     */
    public void show() {
        if (mIsExpanded) return;
        
        ValueAnimator animator = ValueAnimator.ofFloat(mScreenHeight, mScreenHeight * (1 - PANEL_HEIGHT_RATIO));
        animator.setDuration(ANIMATION_DURATION);
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            setTranslationY(value);
        });
        animator.start();
        
        mIsExpanded = true;
        mStatusText.setText("Stone Assistant - Active");
        
        // Notify apps to resize
        notifyAppResize(true);
    }
    
    /**
     * Hide the Stone panel with animation
     */
    public void hide() {
        if (!mIsExpanded) return;
        
        ValueAnimator animator = ValueAnimator.ofFloat(getTranslationY(), mScreenHeight);
        animator.setDuration(ANIMATION_DURATION);
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            setTranslationY(value);
        });
        animator.start();
        
        mIsExpanded = false;
        mStatusText.setText("Stone Assistant");
        
        // Notify apps to resize
        notifyAppResize(false);
    }
    
    /**
     * Toggle the panel visibility
     */
    public void toggle() {
        if (mIsExpanded) {
            hide();
        } else {
            show();
        }
    }
    
    /**
     * Notify the system that apps should resize
     */
    private void notifyAppResize(boolean shouldShrink) {
        Intent intent = new Intent("com.android.systemui.stone.RESIZE_APP");
        intent.putExtra("should_shrink", shouldShrink);
        intent.putExtra("resize_ratio", shouldShrink ? (1 - PANEL_HEIGHT_RATIO) : 1.0f);
        getContext().sendBroadcast(intent);
    }
    
    /**
     * Check if the panel is currently expanded
     */
    public boolean isExpanded() {
        return mIsExpanded;
    }
    
    /**
     * Load a specific URL in the chat WebView
     */
    public void loadUrl(String url) {
        if (mChatWebView != null) {
            mChatWebView.loadUrl(url);
        }
    }
    
    /**
     * Execute JavaScript in the chat WebView
     */
    public void executeJavaScript(String javascript) {
        if (mChatWebView != null) {
            mChatWebView.evaluateJavascript(javascript, null);
        }
    }
}