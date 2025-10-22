package com.android.systemui.stone;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

/**
 * StonePanel - The sliding chat interface that takes up 1/3 of the screen
 * when activated. This panel slides up from the bottom when the user
 * swipes up from the Stone icon.
 */
public class StonePanel extends FrameLayout {
    private static final String TAG = "StonePanel";
    private static final int ANIMATION_DURATION_MS = 300;
    private static final String CHAT_URL = "http://localhost:8080/chat";

    private WebView mWebView;
    private boolean mIsExpanded = false;
    private int mPanelHeight;

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
        // Get screen height to calculate off-screen position
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        int screenHeight = metrics.heightPixels;

        // Panel height is 1/3 of screen (matches StoneManager layout params)
        mPanelHeight = (int) (screenHeight * 0.33f);

        // Create and configure WebView
        mWebView = new WebView(context);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.setWebViewClient(new WebViewClient());

        // Load the chat interface URL
        mWebView.loadUrl(CHAT_URL);

        // Add WebView to this FrameLayout
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        );
        addView(mWebView, params);

        // Initialize to off-screen position (below the screen)
        // Since the panel is already positioned at the bottom by WindowManager,
        // we translate it down by its own height to hide it
        setTranslationY(mPanelHeight);
    }

    /**
     * Show the panel with animation.
     * Animates translationY from off-screen position to 0 (on-screen).
     */
    public void show() {
        if (mIsExpanded) return;

        mIsExpanded = true;

        // Animate from current off-screen position to 0 (fully visible)
        ValueAnimator animator = ValueAnimator.ofFloat(getTranslationY(), 0f);
        animator.setDuration(ANIMATION_DURATION_MS);
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            setTranslationY(value);
        });
        animator.start();
    }

    /**
     * Hide the panel with animation.
     * Animates translationY from 0 (on-screen) to off-screen position.
     */
    public void hide() {
        if (!mIsExpanded) return;

        mIsExpanded = false;

        // Animate from current position to off-screen (panel height)
        ValueAnimator animator = ValueAnimator.ofFloat(getTranslationY(), mPanelHeight);
        animator.setDuration(ANIMATION_DURATION_MS);
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            setTranslationY(value);
        });
        animator.start();
    }

    /**
     * Toggle the panel visibility.
     * Checks current state and calls either show() or hide().
     */
    public void toggle() {
        if (mIsExpanded) {
            hide();
        } else {
            show();
        }
    }

    /**
     * Check if the panel is currently expanded.
     *
     * @return true if panel is visible, false if hidden
     */
    public boolean isExpanded() {
        return mIsExpanded;
    }
}
