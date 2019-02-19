package com.xujiaji.learnopengl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

public class LessonFiveGLSurfaceView extends GLSurfaceView {

    private LessonFiveRenderer mRenderer;

    public LessonFiveGLSurfaceView(Context context) {
        super(context);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (
                event == null
                || event.getAction() != MotionEvent.ACTION_DOWN
                || mRenderer == null) {
            return super.onTouchEvent(event);
        }
        // 确保我们在OpenGL线程上调用switchMode()
        // queueEvent() 是GLSurfaceView的一个方法，它将为我们做到这点
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.switchMode();
            }
        });
        return true;
    }

    public void setRenderer(LessonFiveRenderer renderer) {
        mRenderer = renderer;
        super.setRenderer(renderer);
    }
}
