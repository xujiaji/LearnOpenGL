package com.xujiaji.learnopengl;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class LessonOneRenderer implements GLSurfaceView.Renderer {

    // new 类成员
    private final FloatBuffer mTriangle1Vertices;
    private final FloatBuffer mTriangle2Vertices;
    private final FloatBuffer mTriangle3Vertices;

    /** 每个Float多少字节*/
    private final int mBytePerFloat = 4;

    /** 这将用于传递变换矩阵*/
    private int mMVPMatrixHandle;
    /** 用于传递model位置信息*/
    private int mPositionHandle;
    /** 用于传递模型颜色信息*/
    private int mColorHandle;

    // 存放投影矩阵，用于将场景投影到2D视角
    private float[] mProjectionMatrix = new float[16];

    // 存放模型矩阵，该矩阵用于将模型从对象空间（可以认为每个模型开始都位于宇宙的中心）移动到世界空间
    private float[] mModelMatrix = new float[16];


    /**
     * 初始Model数据
     */
    public LessonOneRenderer() {
        // 这个三角形是红色，蓝色和绿色组成
        final float[] triangle1VerticesData = {
            // X, Y, Z,
            // R, G, B, A
            -0.5F, -0.25F, 0.0F,
            1.0F, 0.0F, 0.0F, 1.0F,

            0.5F, -0.25F, 0.0F,
            0.0F, 0.0F, 1.0F, 1.0F,

            0.0F, 0.559016994F, 0.0F,
            0.0F, 1.0F, 0.0F, 1.0F
        };

        // This triangle is yellow, cyan, and magenta.
        final float[] triangle2VerticesData = {
                // X, Y, Z,
                // R, G, B, A
                -0.5f, -0.25f, 0.0f,
                1.0f, 1.0f, 0.0f, 1.0f,

                0.5f, -0.25f, 0.0f,
                0.0f, 1.0f, 1.0f, 1.0f,

                0.0f, 0.559016994f, 0.0f,
                1.0f, 0.0f, 1.0f, 1.0f};

        // This triangle is white, gray, and black.
        final float[] triangle3VerticesData = {
                // X, Y, Z,
                // R, G, B, A
                -0.5f, -0.25f, 0.0f,
                1.0f, 1.0f, 1.0f, 1.0f,

                0.5f, -0.25f, 0.0f,
                0.5f, 0.5f, 0.5f, 1.0f,

                0.0f, 0.559016994f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f};

        // 初始化缓冲区
        mTriangle1Vertices = ByteBuffer.allocateDirect(triangle1VerticesData.length * mBytePerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangle2Vertices = ByteBuffer.allocateDirect(triangle2VerticesData.length * mBytePerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangle3Vertices = ByteBuffer.allocateDirect(triangle3VerticesData.length * mBytePerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();

        mTriangle1Vertices.put(triangle1VerticesData).position(0);
        mTriangle2Vertices.put(triangle2VerticesData).position(0);
        mTriangle3Vertices.put(triangle3VerticesData).position(0);
    }

    // new class 定义

    /**
     * 存储view矩阵。可以认为这是一个相机，我们通过相机将世界空间转换为眼睛空间
     * 它定位相对于我们眼睛的东西
     */
    private float[] mViewMatrix = new float[16];

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // 设置背景清理颜色为灰色
        GLES20.glClearColor(0.5F, 0.5F, 0.5F, 0.5F);

        // 将眼睛放到原点之后
        final float eyeX = 0.0F;
        final float eyeY = 0.0F;
        final float eyeZ = 2.5F;

        // 我们的眼睛望向哪
        final float lookX = 0.0F;
        final float lookY = 0.0F;
        final float lookZ = -5.0F;

        // 设置我们的向量，这是我们拿着相机时头指向的方向
        final float upX = 0.0F;
        final float upY = 1.0F;
        final float upZ = 0.0F;

        // 设置view矩阵，可以说这个矩阵代表相机的位置
        // 注意：在OpenGL 1中使用ModelView matrix，这是一个model和view矩阵的组合。
        //在OpenGL2中，我们选择分别跟踪这些矩阵
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

        final String vertexShader =
                "uniform mat4 u_MVPMatrix;    \n" + // 一个表示组合model、view、projection矩阵的常量
                "attribute vec4 a_Position;   \n" + // 我们将要传入的每个顶点的位置信息
                "attribute vec4 a_Color;      \n" + // 我们将要传入的每个顶点的颜色信息

                "varying vec4 v_Color;        \n" + // 他将被传入片段着色器

                "void main()                  \n" + // 顶点着色器入口
                "{                            \n" +
                "   v_Color = a_Color;        \n" + // 将颜色传递给片段着色器
                                                    // 它将在三角形内插值
                "   gl_Position = u_MVPMatrix \n" + // gl_Position是一个特殊的变量用来存储最终的位置
                "               * a_Position; \n" + // 将顶点乘以矩阵得到标准化屏幕坐标的最终点
                "}                            \n";

        final String fragmentShader =
                "precision mediump float;       \n" + // 我们将默认精度设置为中等，我们不需要片段着色器中的高精度
                "varying vec4 v_Color;          \n" + // 这是从三角形每个片段内插的顶点着色器的颜色
                "void main()                    \n" + // 片段着色器入口
                "{                              \n" +
                "   gl_FragColor = v_Color;     \n" + // 直接将颜色传递
                "}                              \n";

        // 加载顶点着色器
        int vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        if (vertexShaderHandle != 0) {
            // 传入顶点着色器源代码
            GLES20.glShaderSource(vertexShaderHandle, vertexShader);
            // 编译顶点着色器
            GLES20.glCompileShader(vertexShaderHandle);

            // 获取编译状态
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(vertexShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // 如果编译失败则删除着色器
            if (compileStatus[0] == 0) {
                GLES20.glDeleteShader(vertexShaderHandle);
                vertexShaderHandle = 0;
            }
        }
        if (vertexShaderHandle == 0) {
            throw new RuntimeException("Error creating vertex shader.");
        }

        int fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        if (fragmentShaderHandle != 0) {
            GLES20.glShaderSource(fragmentShaderHandle, fragmentShader);
            GLES20.glCompileShader(fragmentShaderHandle);
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(fragmentShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
            if (compileStatus[0] == 0) {
                GLES20.glDeleteShader(fragmentShaderHandle);
                fragmentShaderHandle = 0;
            }
        }
        if (fragmentShaderHandle == 0) {
            throw new RuntimeException("Error creating fragment shader.");
        }

        // 创建一个OpenGL程序对象并将引用放进去
        int programHandle = GLES20.glCreateProgram();
        if (programHandle != 0) {
            // 绑定顶点着色器到程序对象中
            GLES20.glAttachShader(programHandle, vertexShaderHandle);
            // 绑定片段着色器到程序对象中
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);
            // 绑定属性
            GLES20.glBindAttribLocation(programHandle, 0, "a_Position");
            GLES20.glBindAttribLocation(programHandle, 1, "a_Color");
            // 将两个着色器连接到程序
            GLES20.glLinkProgram(programHandle);
            // 获取连接状态
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);
            // 如果连接失败，删除这程序
            if (linkStatus[0] == 0) {
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }

        if (programHandle == 0) {
            throw new RuntimeException("Error creating program.");
        }

        // 设置程序引用，这将在之后传递值到程序时使用
        mMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_MVPMatrix");
        mPositionHandle = GLES20.glGetAttribLocation(programHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(programHandle, "a_Color");

        // 告诉OpenGL渲染的时候使用这个程序
        GLES20.glUseProgram(programHandle);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // 设置OpenGL界面和当前视图相同的尺寸
        GLES20.glViewport(0, 0, width, height);

        // 创建一个新的透视投影矩阵，高度保持不变，而高度根据纵横比而变换
        final float ratio = (float) width / height;
        final float left = -ratio;
        final float right = ratio;
        final float bottom = -1.0F;
        final float top = 1.0F;
        final float near = 1.0F;
        final float far = 10F;

        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        // 每10s完成一次旋转
        long time = SystemClock.uptimeMillis() % 10000L;
        float angleDegrees = (360.0F / 10000.0F) * ((int)time);

        // 画三角形
        Matrix.setIdentityM(mModelMatrix, 0);
//        Matrix.rotateM(mModelMatrix, 0, angleDegrees, 0.0F, 0.0F, 1.0F);
        drawTriangle(mTriangle1Vertices);

        // Draw one translated a bit down and rotated to be flat on the ground.
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, -1.0f, 0.0f);
        Matrix.rotateM(mModelMatrix, 0, 90.0f, 1.0f, 0.0f, 0.0f);
        Matrix.rotateM(mModelMatrix, 0, angleDegrees, 0.0f, 0.0f, 1.0f);
        drawTriangle(mTriangle2Vertices);

        // Draw one translated a bit to the right and rotated to be facing to the left.
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 1.0f, 0.0f, 0.0f);
        Matrix.rotateM(mModelMatrix, 0, 90.0f, 0.0f, 1.0f, 0.0f);
        Matrix.rotateM(mModelMatrix, 0, angleDegrees, 0.0f, 0.0f, 1.0f);
        drawTriangle(mTriangle3Vertices);

    }

    // 新的类成员
    /** 为最终的组合矩阵分配存储空间，这将用来传入着色器程序*/
    private float[] mMVPMatrix = new float[16];

    /** 每个顶点有多少字节组成，每次需要迈过这么一大步（每个顶点有7个元素，3个表示位置，4个表示颜色，7 * 4 = 28个字节）*/
    private final int mStrideBytes = 7 * mBytePerFloat;

    /** 位置数据偏移量*/
    private final int mPositionOffset = 0;

    /** 一个元素的位置数据大小*/
    private final int mPositionDataSize = 3;

    /** 颜色数据偏移量*/
    private final int mColorOffset = 3;

    /** 一个元素的颜色数据大小*/
    private final int mColorDataSize = 4;

    /**
     * 从给定的顶点数据中绘制一个三角形
     * @param aTriangleBuffer 包含顶点数据的缓冲区
     */
    private void drawTriangle(FloatBuffer aTriangleBuffer) {
        aTriangleBuffer.position(mPositionOffset);
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
                mStrideBytes, aTriangleBuffer);

        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // 传入颜色信息
        aTriangleBuffer.position(mColorOffset);
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
                mStrideBytes, aTriangleBuffer);

        GLES20.glEnableVertexAttribArray(mColorHandle);

        // 将视图矩阵乘以模型矩阵，并将结果存放到MVP Matrix（model * view）
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

        // 将上面计算好的视图模型矩阵乘以投影矩阵，并将结果存放到MVP Matrix（model * view * projection）
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);

    }
}
