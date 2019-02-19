package com.xujiaji.learnopengl;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class LessonOneActivity extends AppCompatActivity {

    /** 保留对GLSurfaceView的引用*/
    private LessonFiveGLSurfaceView mGLSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGLSurfaceView = new LessonFiveGLSurfaceView(this);
        //检测系统是否支持OpenGL ES 2.0
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

        if (supportsEs2) {
            // 请求一个OpenGL ES 2.0兼容的上下文
            mGLSurfaceView.setEGLContextClientVersion(2);
            // 设置我们的Demo渲染器，定义在后面讲
            mGLSurfaceView.setRenderer(new LessonFiveRenderer(this));
        } else {
            // 如果您想同时支持ES 1.0和2.0的话，这里您可以创建兼容OpenGL ES 1.0的渲染器
            return;
        }
        setContentView(mGLSurfaceView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Activity 必须在onResume中调用GLSurfaceView的onResume方法
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Activity 必须在onPause中调用GLSurfaceView的onPause方法
        mGLSurfaceView.onPause();
    }
}
