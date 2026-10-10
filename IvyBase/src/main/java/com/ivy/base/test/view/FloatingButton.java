package com.ivy.base.test.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.ImageView;

import com.ivy.base.R;


public class FloatingButton extends ImageView {

    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;

    private int screenWidth;

    private float downX, downY;
    private int startX, startY;

    private boolean isLongPress = false;
    private boolean isDragging = false;

    private Handler handler = new Handler(Looper.getMainLooper());

    private Runnable longPressRunnable;

    private static final int LONG_PRESS_TIME = 300;
    private static final int TOUCH_SLOP = 10;

    public FloatingButton(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        setPadding(12, 12, 12, 12);
        setBackgroundColor(Color.GRAY);
        setImageResource(R.drawable.icon_warning);

        screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;

        longPressRunnable = () -> isLongPress = true;

        setOnTouchListener((v, event) -> {

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:

                    downX = event.getRawX();
                    downY = event.getRawY();

                    startX = layoutParams.x;
                    startY = layoutParams.y;

                    isDragging = false;
                    isLongPress = false;

                    handler.postDelayed(longPressRunnable, LONG_PRESS_TIME);

                    return true;

                case MotionEvent.ACTION_MOVE:

                    float dx = event.getRawX() - downX;
                    float dy = event.getRawY() - downY;

                    if (Math.abs(dx) > TOUCH_SLOP || Math.abs(dy) > TOUCH_SLOP) {
                        isDragging = true;
                        handler.removeCallbacks(longPressRunnable);
                    }

                    // 只有长按后才允许拖动（避免误触）
                    if (isLongPress) {
                        layoutParams.x = startX + (int) dx;
                        layoutParams.y = startY + (int) dy;

                        windowManager.updateViewLayout(this, layoutParams);
                    }

                    return true;

                case MotionEvent.ACTION_UP:

                    handler.removeCallbacks(longPressRunnable);

                    // 点击逻辑（非拖动 + 非长按）
                    if (!isDragging && !isLongPress) {
                        performClick();
                    }

                    // 拖动结束吸边
                    if (isDragging || isLongPress) {
                        autoAttachEdge();
                    }

                    return true;
            }

            return false;
        });

        setOnClickListener(v -> onFloatClick());
    }

    // ===== 对外点击事件 =====
    protected void onFloatClick() {
        // override
    }

    public void update(boolean state){
        setColorFilter(state ? Color.RED : Color.WHITE);
    }

    // ===== attach window =====
    public void attach(WindowManager wm, WindowManager.LayoutParams params) {
        this.windowManager = wm;
        this.layoutParams = params;
        wm.addView(this, params);
    }

    // ===== 自动吸边 =====
    private void autoAttachEdge() {
        int viewWidth = getWidth();

        int center = 0;//screenWidth / 2;

        int targetX;

        // 当前中心点判断
        int currentCenterX = layoutParams.x + viewWidth / 2;

        if (currentCenterX < center) {
            // 吸左边
            targetX = -1 * screenWidth / 2;
        } else {
            // 吸右边（必须减去自身宽度）
            targetX = screenWidth / 2;
        }

        animateMove(targetX, layoutParams.y);
    }

    // ===== 吸边动画 =====
    private void animateMove(int targetX, int targetY) {

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);

        int startX = layoutParams.x;
        int startY = layoutParams.y;

        animator.setDuration(200);

        animator.addUpdateListener(animation -> {

            float t = (float) animation.getAnimatedValue();

            layoutParams.x = (int) (startX + (targetX - startX) * t);
            layoutParams.y = (int) (startY + (targetY - startY) * t);

            windowManager.updateViewLayout(this, layoutParams);
        });

        animator.start();
    }
}