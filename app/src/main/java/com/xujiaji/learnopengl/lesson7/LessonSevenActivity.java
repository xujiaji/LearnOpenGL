package com.xujiaji.learnopengl.lesson7;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;

import com.xujiaji.learnopengl.R;

public class LessonSevenActivity extends AppCompatActivity {
    private LessonSevenGLSurfaceView mGLSurfaceView;
    private LessonSevenRenderer mRenderer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lesson_seven);

        mGLSurfaceView = (LessonSevenGLSurfaceView) findViewById(R.id.gl_surface_view);

        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

        if (supportsEs2) {
            mGLSurfaceView.setEGLContextClientVersion(2);

            final DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

            mRenderer = new LessonSevenRenderer(this, mGLSurfaceView);
            mGLSurfaceView.setRenderer(mRenderer, displayMetrics.density);
        } else {
            return;
        }

        findViewById(R.id.button_decrease_num_cubes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decreaseCubeCount();
            }
        });

        findViewById(R.id.button_increase_num_cubes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                increaseCubeCount();
            }
        });

        findViewById(R.id.button_switch_VBOs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleVBOs();
            }
        });

        findViewById(R.id.button_switch_stride).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleStride();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLSurfaceView.onPause();
    }


    private void decreaseCubeCount() {
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.decreaseCubeCount();
            }
        });
    }

    private void increaseCubeCount() {
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.increaseCubeCount();
            }
        });
    }

    private void toggleVBOs() {
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.toggleVBOs();
            }
        });
    }

    protected void toggleStride() {
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.toggleStride();
            }
        });
    }

    public void updateVboStatus(final boolean usingVbos) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (usingVbos) {
                    ((Button) findViewById(R.id.button_switch_VBOs)).setText("使用VBOs");
                } else {
                    ((Button) findViewById(R.id.button_switch_VBOs)).setText("未使用VBOs");
                }
            }
        });
    }

    public void updateStrideStatus(final boolean useStride) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (useStride) {
                    ((Button) findViewById(R.id.button_switch_stride)).setText("使用跨度");
                } else {
                    ((Button) findViewById(R.id.button_switch_stride)).setText("未使用跨度");
                }
            }
        });
    }
}
