package com.xujiaji.learnopengl;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;

import com.xujiaji.learnopengl.common.BufferBuilder;
import com.xujiaji.learnopengl.common.RawResourceReader;
import com.xujiaji.learnopengl.common.ShaderHelper;
import com.xujiaji.learnopengl.common.ShapeBuilder;
import com.xujiaji.learnopengl.common.TextureHelper;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class LessonSixRenderer implements GLSurfaceView.Renderer {
    private Context mActivityContext;

    // 每个位置由3个元素组成
    private final static int POSITION_SIZE = 3;

    // 每个法线由3个元素组成
    private final static int NORMAL_SIZE = 3;

    // 每个二维坐标由2个元素组成
    private final static int COORDINATE_ZISE = 2;

    // 存储模型数据在float缓冲区
    private final FloatBuffer mCubePositions;
    private final FloatBuffer mCubeNormals;
    private final FloatBuffer mCubeTextureCoordinates;
    private final FloatBuffer mCubeTextureCoordiantesForPlane;

    /**
     * 存储view矩阵。可以认为这是一个相机，我们通过相机将世界空间转换为眼睛空间
     * 它定位相对于我们眼睛的东西
     */
    private float[] mViewMatrix = new float[16];
    // 存储累积的旋转
    private float[] mAccumulatedRotation = new float[16];
    // 存储当前的旋转
    private final float[] mCurrentRotation = new float[16];
    // 临时矩阵
    private float[] mTemporaryMatrix = new float[16];
    // 存放投影矩阵，用于将场景投影到2D视角
    private float[] mProjectionMatrix = new float[16];
    // 存放光源的model矩阵
    private float[] mLightModelMatrix = new float[16];

    private float[] mModelMatrix = new float[16];

    private float[] mMVPMatrix = new float[16];

    // 用来存放光源在模型空间的初始位置，我们需要第四个坐标
    // 这样我们就可以通过变换矩阵将它们相乘来实现平移
    private final float[] mLightPosInModelSpace = new float[]{0F, 0F, 0F, 1F};

    // 用来存放当前光源在世界空间中的位置
    private final float[] mLightPosInWorldSpace = new float[4];

    // 用来存放当前光源在眼睛空间中的位置
    private final float[] mLightPosInEyeSpace = new float[4];

    private int mProgramHandle;
    private int mPointProgramHandle;
    private int mBrickDataHandle;
    private int mGrassDataHandle;

    private int mMVPMatrixHandle;
    private int mMVMatrixHandle;
    private int mLightPosHandle;
    private int mPositionHandle;
    private int mNormalHandle;

    private int mTextureUniformHandle;
    private int mTextureCoordinateHandle;


    // 在Activity被重启的时候，保存放大和缩小的过滤器
    private int mQueueMinFilter;
    private int mQueueMagFilter;

    // 没有volatile仍然工作，但是不保证能刷新
    public volatile float mDeltaX;
    public volatile float mDeltaY;

    public LessonSixRenderer(Context context) {
        this.mActivityContext = context;
        // 定义立方体的点
        //X, Y, Z
        final float[] p1p = {-1.0f, 1.0f, 1.0f};
        final float[] p2p = {1.0f, 1.0f, 1.0f};
        final float[] p3p = {-1.0f, -1.0f, 1.0f};
        final float[] p4p = {1.0f, -1.0f, 1.0f};
        final float[] p5p = {-1.0f, 1.0f, -1.0f};
        final float[] p6p = {1.0f, 1.0f, -1.0f};
        final float[] p7p = {-1.0f, -1.0f, -1.0f};
        final float[] p8p = {1.0f, -1.0f, -1.0f};

        final float[] cubePositionData = ShapeBuilder.generateCubeData(p1p, p2p, p3p, p4p, p5p, p6p, p7p, p8p, p1p.length);

        // 法线
        final float[] cubeNormalData = ShapeBuilder.generateCubeNormalData();

        // 纹理坐标数据
        final float[] cubeTextureCoordinateData = ShapeBuilder.generateTextureCoordinateData(
                new float[]{
                0.0F, 0.0F,
                0.0F, 1.0F,
                1.0F, 0.0F,
                0.0F, 1.0F,
                1.0F, 1.0F,
                1.0F, 0.0F
        });

        // 平面纹理坐标数据
        final float[] cubeTextureCoordinateDataForPlane = ShapeBuilder.generateTextureCoordinateData(
                new float[]{
                 0.0F,  0.0F,
                 0.0F, 25.0F,
                25.0F,  0.0F,
                 0.0F, 25.0F,
                25.0F, 25.0F,
                25.0F,  0.0F
        });

        mCubePositions                  = BufferBuilder.generate(cubePositionData);
        mCubeNormals                    = BufferBuilder.generate(cubeNormalData);
        mCubeTextureCoordinates         = BufferBuilder.generate(cubeTextureCoordinateData);
        mCubeTextureCoordiantesForPlane = BufferBuilder.generate(cubeTextureCoordinateDataForPlane);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // 设置黑色清理背景
        GLES20.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);

        // 启动剔除背面
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // 启动深度测试
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // 将眼睛放到原点前
        final float eyeX = 0.0F;
        final float eyeY = 0.0F;
        final float eyeZ = -0.5F;

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

        final String vertexShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.per_pixel_vertex_shader_tex_and_light);
        final String fragmentShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.per_pixel_fragment_shader_tex_and_light);

        final int vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        final int fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        mProgramHandle = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                "a_Position", "a_Normal", "a_TexCoordinate");

        final String pointVertexShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.point_vertex_shader);
        final String pointFragmentShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.point_fragment_shader);

        final int pointVertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, pointVertexShader);
        final int pointFragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, pointFragmentShader);

        mPointProgramHandle = ShaderHelper.createAndLinkProgram(pointVertexShaderHandle, pointFragmentShaderHandle,
                "a_Position");

        // 加载纹理
        mBrickDataHandle = TextureHelper.loadTexture(mActivityContext, R.drawable.stone_wall_public_domain);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

        mGrassDataHandle = TextureHelper.loadTexture(mActivityContext, R.drawable.noisy_grass_public_domain);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

        if (mQueueMinFilter != 0) {
            setMinFilter(mQueueMinFilter);
        }

        if (mQueueMagFilter != 0) {
            setMagFilter(mQueueMagFilter);
        }

        // 初始化累积旋转矩阵
        Matrix.setIdentityM(mAccumulatedRotation, 0);
    }

    public void setMinFilter(final int filter) {
        if (mBrickDataHandle != 0 && mGrassDataHandle != 0) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mBrickDataHandle);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, filter);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mGrassDataHandle);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, filter);
        } else {
            mQueueMinFilter = filter;
        }
    }

    public void setMagFilter(final int filter) {
        if (mBrickDataHandle != 0 && mGrassDataHandle != 0) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mBrickDataHandle);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, filter);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mGrassDataHandle);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, filter);
        } else {
            mQueueMagFilter = filter;
        }
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
        long slowTime = SystemClock.uptimeMillis() % 100000L;
        float angleDegrees = (360.0F / 10000.0F) * ((int)time);
        float slowAngleInDegress = (360.0F / 100000.0F) * ((int) slowTime);

        // 告诉OpenGL渲染的时候使用这个程序
        GLES20.glUseProgram(mProgramHandle);

        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVMatrix");
        mLightPosHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_LightPos");
        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        mNormalHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Normal");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");

        // 计算光源的位置，旋转并移动位置
        Matrix.setIdentityM(mLightModelMatrix, 0);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, -2.0f);
        Matrix.rotateM(mLightModelMatrix, 0, angleDegrees, 0.0f, 1.0f, 0.0f);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, 3.5f);

        Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0);

        // 画立方体
        // 将立方体移动到屏幕
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0F, 0.8F, -3.5F);

        // 设置包含当前旋转度的矩阵
        Matrix.setIdentityM(mCurrentRotation, 0);
        Matrix.rotateM(mCurrentRotation, 0, mDeltaX, 0.0F, 1.0F, 0.0F);
        Matrix.rotateM(mCurrentRotation, 0, mDeltaY, 1.0F, 0.0F, 0.0F);
        mDeltaX = 0.0F;
        mDeltaY = 0.0F;

        // 当前旋转乘以累积选择，然后将结果设置到累积旋转
        Matrix.multiplyMM(mTemporaryMatrix, 0, mCurrentRotation, 0, mAccumulatedRotation, 0);
        System.arraycopy(mTemporaryMatrix, 0, mAccumulatedRotation, 0, 16);

        // 考虑整体旋转，旋转立方体
        Matrix.multiplyMM(mTemporaryMatrix, 0, mModelMatrix, 0, mAccumulatedRotation, 0);
        System.arraycopy(mTemporaryMatrix, 0, mModelMatrix, 0, 16);

        // 将纹理单元设置为纹理单元0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // 将纹理绑定到这个单元
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mBrickDataHandle);

        // 通过绑定到纹理单元0，告诉纹理统一采样器在着色器中使用此纹理
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        // 传入纹理坐标信息
        mCubeTextureCoordinates.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, COORDINATE_ZISE,
                GLES20.GL_FLOAT, false,
                0, mCubeTextureCoordinates);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

        drawCube();

        // 绘制平面
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0F, -2.0F, -5.0F);
        Matrix.scaleM(mModelMatrix, 0, 25.0F, 1.0F, 25.0F);
        Matrix.rotateM(mModelMatrix, 0, slowAngleInDegress, 0.0F, 1.0F, 0.0F);

        // 将纹理单元设置为纹理单元0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // 将纹理绑定到这个单元
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mGrassDataHandle);

        // 通过绑定到纹理单元0，告诉纹理统一采样器在着色器中使用此纹理
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        // 传入纹理坐标信息
        mCubeTextureCoordiantesForPlane.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, COORDINATE_ZISE,
                GLES20.GL_FLOAT, false,
                0, mCubeTextureCoordiantesForPlane);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

        drawCube();

        // 绘制光源
        GLES20.glUseProgram(mPointProgramHandle);
        drawLight();
    }

    /**
     * 画立方体
     */
    private void drawCube() {
        // 传入位置信息
        mCubePositions.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, POSITION_SIZE, GLES20.GL_FLOAT, false,
                0, mCubePositions);
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // 传入法线信息
        mCubeNormals.position(0);
        GLES20.glVertexAttribPointer(mNormalHandle, NORMAL_SIZE, GLES20.GL_FLOAT, false,
                0, mCubeNormals);
        GLES20.glEnableVertexAttribArray(mNormalHandle);

        // 将视图矩阵乘以模型矩阵，并将结果存放到MVP Matrix（model * view）
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

        // 传入modelview矩阵
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);

        // 投影矩阵乘以modelview矩阵，
        //  model * view * projection
        Matrix.multiplyMM(mTemporaryMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        System.arraycopy(mTemporaryMatrix, 0, mMVPMatrix, 0, 16);

        // 传入组合好的矩阵
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // 传入相对于眼睛空间的光源位置
        GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);

        // 绘制正方体
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
    }

    /**
     * 绘制光源
     */
    private void drawLight() {
        final int pointMVPMatrixHandle = GLES20.glGetUniformLocation(mPointProgramHandle, "u_MVPMatrix");
        final int pointPositionHandle = GLES20.glGetAttribLocation(mPointProgramHandle, "a_Position");

        // Pass in the position.
        GLES20.glVertexAttrib3f(pointPositionHandle, mLightPosInModelSpace[0], mLightPosInModelSpace[1], mLightPosInModelSpace[2]);

        // Since we are not using a buffer object, disable vertex arrays for this attribute.
        GLES20.glDisableVertexAttribArray(pointPositionHandle);

        // Pass in the transformation matrix.
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mLightModelMatrix, 0);
        Matrix.multiplyMM(mTemporaryMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        System.arraycopy(mTemporaryMatrix, 0, mMVPMatrix, 0, 16);
        GLES20.glUniformMatrix4fv(pointMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // Draw the point.
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
    }
}
