package com.xujiaji.learnopengl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class LessonSixGLSurfaceView extends GLSurfaceView {

    private LessonSixRenderer mRenderer;

    // 用来触摸偏移
    private float mPreviousX;
    private float mPreviousY;

    private float mDensity;

    public LessonSixGLSurfaceView(Context context) {
        super(context);
    }

    public LessonSixGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event == null) return super.onTouchEvent(event);
        float x = event.getX();
        float y = event.getY();

        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (mRenderer != null) {
                float deltaX = (x - mPreviousX) / mDensity / 2F;
                float deltaY = (y - mPreviousY) / mDensity / 2F;

                mRenderer.mDeltaX += deltaX;
                mRenderer.mDeltaY += deltaY;
            }
        }

        mPreviousX = x;
        mPreviousY = y;
        return true;
    }

    public void setRenderer(LessonSixRenderer renderer, float density) {
        mRenderer = renderer;
        mDensity = density;
        super.setRenderer(renderer);
    }
}
