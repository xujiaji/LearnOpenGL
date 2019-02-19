package com.xujiaji.learnopengl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import com.xujiaji.learnopengl.common.RawResourceReader;
import com.xujiaji.learnopengl.common.ShaderHelper;
import com.xujiaji.learnopengl.common.TextureHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class LessonFourRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "LessonTwoRenderer";

    private Context mActivityContext;

    /** 每个Float多少字节*/
    private final int mBytePerFloat = 4;

    /** 每个位置数据在尺寸 */
    private final int mPositionDataSize = 3;

    /** 每个颜色数据的尺寸 */
    private final int mColorDataSize = 4;

    /** 每个法线数据的尺寸 */
    private final int mNormalDataSize = 3;

    private final FloatBuffer mCubePositions;
    private final FloatBuffer mCubeColors;
    private final FloatBuffer mCubeNormals;

    private float[] mModelMatrix = new float[16];
    /**
     * 存储view矩阵。可以认为这是一个相机，我们通过相机将世界空间转换为眼睛空间
     * 它定位相对于我们眼睛的东西
     */
    private float[] mViewMatrix = new float[16];

    // 存放投影矩阵，用于将场景投影到2D视角
    private float[] mProjectionMatrix = new float[16];

    private float[] mMVPMatrix = new float[16];

    // 存放光源的model矩阵
    private float[] mLightModelMatrix = new float[16];

    // 用来存放光源在模型空间的初始位置，我们需要第四个坐标
    // 这样我们就可以通过变换矩阵将它们相乘来实现平移
    private final float[] mLightPosInModelSpace = new float[]{0F, 0F, 0F, 1F};

    // 用来存放当前光源在世界空间中的位置
    private final float[] mLightPosInWorldSpace = new float[4];

    // 用来存放当前光源在眼睛空间中的位置
    private final float[] mLightPosInEyeSpace = new float[4];

    private int mProgramHandle;
    private int mPerFragProgramHandle;
    private int mPointProgramHandle;

    private int mMVPMatrixHandle;
    private int mMVMatrixHandle;
    private int mLightPosHandle;
    private int mPositionHandle;
    private int mColorHandle;
    private int mNormalHandle;

    // 存放我们的模型数据在浮点缓冲区
    private final FloatBuffer mCubeTextureCoordinates;

    // 用来传入纹理
    private int mTextureUniformHandle;

    // 用来传入模型纹理坐标
    private int mTextureCoordinateHandle;

    // 每个数据元素的纹理坐标大小
    private final int mTextureCoordinateDataSize = 2;

    // 纹理数据
    private int mTextureDataHandle;

    public LessonFourRenderer(Context context) {
        this.mActivityContext = context;
        //X, Y, Z
        final float[] cubePositionData = {
                // 在OpenGL，逆时针绕组（下面的点事逆时针顺序）是默认的。
                // 这意味着当我们在观察一个三角形时，如果这些电视逆时针的，那么我们正在看"前面"，如果不是我们则正在看背面
                // OpenGL有一个优化，所有背面的三角形都会被剔除，因为它们通常代表一个物体的背面，无论如何都不可见
                // 正面
                -1.0F, 1.0F, 1.0F,
                -1.0F, -1.0F, 1.0F,
                1.0F, 1.0F, 1.0F,
                -1.0F, -1.0F, 1.0F,
                1.0F, -1.0F, 1.0F,
                1.0F, 1.0F, 1.0F,

                // Right face
                1.0f, 1.0f, 1.0f,
                1.0f, -1.0f, 1.0f,
                1.0f, 1.0f, -1.0f,
                1.0f, -1.0f, 1.0f,
                1.0f, -1.0f, -1.0f,
                1.0f, 1.0f, -1.0f,

                // Back face
                1.0f, 1.0f, -1.0f,
                1.0f, -1.0f, -1.0f,
                -1.0f, 1.0f, -1.0f,
                1.0f, -1.0f, -1.0f,
                -1.0f, -1.0f, -1.0f,
                -1.0f, 1.0f, -1.0f,

                // Left face
                -1.0f, 1.0f, -1.0f,
                -1.0f, -1.0f, -1.0f,
                -1.0f, 1.0f, 1.0f,
                -1.0f, -1.0f, -1.0f,
                -1.0f, -1.0f, 1.0f,
                -1.0f, 1.0f, 1.0f,

                // Top face
                -1.0f, 1.0f, -1.0f,
                -1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, -1.0f,
                -1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, -1.0f,

                // Bottom face
                1.0f, -1.0f, -1.0f,
                1.0f, -1.0f, 1.0f,
                -1.0f, -1.0f, -1.0f,
                1.0f, -1.0f, 1.0f,
                -1.0f, -1.0f, 1.0f,
                -1.0f, -1.0f, -1.0f,
        };

        // R，G，B，A
        final float[] cubeColorData = {
                // 正面红色
                1.0F, 0.0F, 0.0F, 1.0F,
                1.0F, 0.0F, 0.0F, 1.0F,
                1.0F, 0.0F, 0.0F, 1.0F,
                1.0F, 0.0F, 0.0F, 1.0F,
                1.0F, 0.0F, 0.0F, 1.0F,
                1.0F, 0.0F, 0.0F, 1.0F,

                // Right face (green)
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,

                // Back face (blue)
                0.0f, 0.0f, 1.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                // Left face (yellow)
                1.0f, 1.0f, 0.0f, 1.0f,
                1.0f, 1.0f, 0.0f, 1.0f,
                1.0f, 1.0f, 0.0f, 1.0f,
                1.0f, 1.0f, 0.0f, 1.0f,
                1.0f, 1.0f, 0.0f, 1.0f,
                1.0f, 1.0f, 0.0f, 1.0f,

                // Top face (cyan)
                0.0f, 1.0f, 1.0f, 1.0f,
                0.0f, 1.0f, 1.0f, 1.0f,
                0.0f, 1.0f, 1.0f, 1.0f,
                0.0f, 1.0f, 1.0f, 1.0f,
                0.0f, 1.0f, 1.0f, 1.0f,
                0.0f, 1.0f, 1.0f, 1.0f,

                // Bottom face (magenta)
                1.0f, 0.0f, 1.0f, 1.0f,
                1.0f, 0.0f, 1.0f, 1.0f,
                1.0f, 0.0f, 1.0f, 1.0f,
                1.0f, 0.0f, 1.0f, 1.0f,
                1.0f, 0.0f, 1.0f, 1.0f,
                1.0f, 0.0f, 1.0f, 1.0f
        };

        final float[] cubeNormalData = {
                // Front face
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                // Right face
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,

                // Back face
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,

                // Left face
                -1.0f, 0.0f, 0.0f,
                -1.0f, 0.0f, 0.0f,
                -1.0f, 0.0f, 0.0f,
                -1.0f, 0.0f, 0.0f,
                -1.0f, 0.0f, 0.0f,
                -1.0f, 0.0f, 0.0f,

                // Top face
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,

                // Bottom face
                0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f
        };

        // S, T （或 X， Y）
        // 纹理坐标数据
        // 因为图像Y轴指向下方（向下移动图片时值会增加），OpenGL的Y轴指向上方
        // 我们通过翻转Y轴来调整它
        // 每个面的纹理坐标都是相同的
        final float[] cubeTextureCoordinateData =
                {
                        // 正面
                        0.0F, 0.0F,
                        0.0F, 1.0F,
                        1.0F, 0.0F,
                        0.0F, 1.0F,
                        1.0F, 1.1F,
                        1.0F, 0.0F,

                        0.0F, 0.0F,
                        0.0F, 1.0F,
                        1.0F, 0.0F,
                        0.0F, 1.0F,
                        1.0F, 1.1F,
                        1.0F, 0.0F,

                        0.0F, 0.0F,
                        0.0F, 1.0F,
                        1.0F, 0.0F,
                        0.0F, 1.0F,
                        1.0F, 1.1F,
                        1.0F, 0.0F,

                        0.0F, 0.0F,
                        0.0F, 1.0F,
                        1.0F, 0.0F,
                        0.0F, 1.0F,
                        1.0F, 1.1F,
                        1.0F, 0.0F,

                        0.0F, 0.0F,
                        0.0F, 1.0F,
                        1.0F, 0.0F,
                        0.0F, 1.0F,
                        1.0F, 1.1F,
                        1.0F, 0.0F,

                        0.0F, 0.0F,
                        0.0F, 1.0F,
                        1.0F, 0.0F,
                        0.0F, 1.0F,
                        1.0F, 1.1F,
                        1.0F, 0.0F
                };

        mCubePositions = ByteBuffer.allocateDirect(cubePositionData.length * mBytePerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();

        mCubeColors = ByteBuffer.allocateDirect(cubeColorData.length * mBytePerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();

        mCubeNormals = ByteBuffer.allocateDirect(cubeNormalData.length * mBytePerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();

        mCubeTextureCoordinates = ByteBuffer.allocateDirect(cubeTextureCoordinateData.length * mBytePerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();

        mCubePositions.put(cubePositionData).position(0);
        mCubeColors.put(cubeColorData).position(0);
        mCubeNormals.put(cubeNormalData).position(0);
        mCubeTextureCoordinates.put(cubeTextureCoordinateData).position(0);
    }

    protected String getVertexShader() {
        return RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.per_pixel_vertex_shader);
    }

    protected String getFragmentShader() {
        return RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.per_pixel_fragment_shader);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0F, 0F, 0F, 0F);
        // 使用剔除去掉背面
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        // 启用深度测试
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

        final int vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, getVertexShader());
        final int fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, getFragmentShader());

        mProgramHandle = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, "a_Position", "a_Color", "a_Normal", "a_TexCoordinate");

        // 加载纹理
        mTextureDataHandle = TextureHelper.loadTexture(mActivityContext, R.drawable.bumpy_bricks_public_domain);

        // 定义一个简单的着色程序
        final String pointVertexShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.point_vertex_shader);
        final String pointFragmentShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.point_fragment_shader);

        final int pointVertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, pointVertexShader);
        final int pointFragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, pointFragmentShader);

        mPointProgramHandle = ShaderHelper.createAndLinkProgram(pointVertexShaderHandle, pointFragmentShaderHandle, "a_Position");

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

        // 告诉OpenGL渲染的时候使用这个程序
        GLES20.glUseProgram(mProgramHandle);

        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVMatrix");
        mLightPosHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_LightPos");
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Color");
        mNormalHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Normal");

        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");

        // 将纹理单元设置为纹理单元0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // 将纹理绑定到这个单元
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);

        // 通过绑定到纹理单元0，告诉纹理统一采样器在着色器中使用此纹理
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        // 计算光源的位置，旋转并移动位置
        Matrix.setIdentityM(mLightModelMatrix, 0);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, -5.0f);
        Matrix.rotateM(mLightModelMatrix, 0, angleDegrees, 0.0f, 1.0f, 0.0f);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, 2.0f);

        Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0);

        // 绘制正方体
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 4.0F, 0.0F, -7.0F);
        Matrix.rotateM(mModelMatrix, 0, angleDegrees, 1.0F, 0.0F, 0.0F);
        drawCube();

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, -4.0f, 0.0f, -7.0f);
        Matrix.rotateM(mModelMatrix, 0, angleDegrees, 0.0f, 1.0f, 0.0f);
        drawCube();

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 4.0f, -7.0f);
        Matrix.rotateM(mModelMatrix, 0, angleDegrees, 0.0f, 0.0f, 1.0f);
        drawCube();

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, -4.0f, -7.0f);
        drawCube();

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -5.0f);
        Matrix.rotateM(mModelMatrix, 0, angleDegrees, 1.0f, 1.0f, 0.0f);
        drawCube();

        // 绘制光点
        GLES20.glUseProgram(mPointProgramHandle);
        drawLight();
    }

    private void drawLight() {
        final int pointMVPMatrixHandle = GLES20.glGetUniformLocation(mPointProgramHandle, "u_MVPMatrix");
        final int pointPositionHandle = GLES20.glGetAttribLocation(mPointProgramHandle, "a_Position");

        // Pass in the position.
        GLES20.glVertexAttrib3f(pointPositionHandle, mLightPosInModelSpace[0], mLightPosInModelSpace[1], mLightPosInModelSpace[2]);

        // Since we are not using a buffer object, disable vertex arrays for this attribute.
        GLES20.glDisableVertexAttribArray(pointPositionHandle);

        // Pass in the transformation matrix.
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mLightModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(pointMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // Draw the point.
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
    }

    private void drawCube() {
        // 传入位置信息
        mCubePositions.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
                0, mCubePositions);
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // 传入颜色信息
        mCubeColors.position(0);
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
                0, mCubeColors);
        GLES20.glEnableVertexAttribArray(mColorHandle);

        // 传入法线信息
        mCubeNormals.position(0);
        GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false,
                0, mCubeNormals);
        GLES20.glEnableVertexAttribArray(mNormalHandle);

        // 传入纹理坐标信息
        mCubeTextureCoordinates.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false,
                0, mCubeTextureCoordinates);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

        // 将视图矩阵乘以模型矩阵，并将结果存放到MVP Matrix（model * view）
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

        // 传入modelview矩阵
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);

        // 将上面计算好的视图模型矩阵乘以投影矩阵，并将结果存放到MVP Matrix（model * view * projection）
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        // 传入组合矩阵
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // 传入相对于眼睛空间的光源位置
        GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);

        // 绘制正方体
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
    }


}
