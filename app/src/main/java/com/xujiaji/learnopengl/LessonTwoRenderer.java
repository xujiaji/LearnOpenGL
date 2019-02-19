package com.xujiaji.learnopengl;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class LessonTwoRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "LessonTwoRenderer";

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

    private int mPerVertexProgramHandle;
    private int mPerFragProgramHandle;
    private int mPointProgramHandle;

    private int mMVPMatrixHandle;
    private int mMVMatrixHandle;
    private int mLightPosHandle;
    private int mPositionHandle;
    private int mColorHandle;
    private int mNormalHandle;

    public LessonTwoRenderer() {
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

        mCubePositions = ByteBuffer.allocateDirect(cubePositionData.length * mBytePerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();

        mCubeColors = ByteBuffer.allocateDirect(cubeColorData.length * mBytePerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();

        mCubeNormals = ByteBuffer.allocateDirect(cubeNormalData.length * mBytePerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();

        mCubePositions.put(cubePositionData).position(0);
        mCubeColors.put(cubeColorData).position(0);
        mCubeNormals.put(cubeNormalData).position(0);
    }

    protected String getVertexShader() {
        final String vertexShader =
                "uniform mat4 u_MVPMatrix;      \n" + // 一个表示组合model、view、projection矩阵的常量
                        "uniform mat4 u_MVMatrix;       \n" + // 一个表示组合model、view矩阵的常量
                        "uniform vec3 u_LightPos;       \n" + // 光源在眼睛空间的位置

                        "attribute vec4 a_Position;     \n" + // 我们将要传入的每个顶点的位置信息
                        "attribute vec4 a_Color;        \n" + // 我们将要传入的每个顶点的颜色信息
                        "attribute vec3 a_Normal;       \n" + // 我们将要传入的每个顶点的法线信息

                        "varying vec4 v_Color;          \n" + // 这将被传入片段着色器

                        "void main()                    \n" + // 顶点着色器入口
                        "{                              \n" +
                        // 将顶点转换成眼睛空间
                        "   vec3 modelViewVertex = vec3(u_MVMatrix * a_Position);                \n" +
                        // 将法线的方向转换成眼睛空间
                        "   vec3 modelViewNormal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));       \n" +
                        // 将用于哀减
                        "   float distance = length(u_LightPos - modelViewVertex);               \n" +
                        // 获取从光源到顶点方向的光线向量
                        "   vec3 lightVector = normalize(u_LightPos - modelViewVertex);          \n" +
                        // 计算光线矢量和顶点法线的点积，如果法线和光线矢量指向相同的方向，那么它将获得最大的照明
                        "   float diffuse = max(dot(modelViewNormal, lightVector), 0.1);         \n" +
                        // 根据距离哀减光线
                        "   diffuse = diffuse * (1.0 / (1.0 + (0.25 * distance * distance)));    \n" +
                        // 将颜色乘以亮度，它将被插入三角形中
                        "   v_Color = a_Color * diffuse;                                         \n" +
                        // gl_Position是一个特殊的变量用来存储最终的位置
                        // 将顶点乘以矩阵得到标准化屏幕坐标的最终点
                        "   gl_Position = u_MVPMatrix * a_Position;                              \n" +
                        "}                                                                       \n";

        return vertexShader;
    }

    protected String getVertexShader2() {
        final String vertexShader =
                "uniform mat4 u_MVPMatrix;                                   \n" + // 一个表示组合model、view、projection矩阵的常量
                        "uniform mat4 u_MVMatrix;                            \n" + // 一个表示组合model、view矩阵的常量

                        "attribute vec4 a_Position;                          \n" + // 我们将要传入的每个顶点的位置信息
                        "attribute vec4 a_Color;                             \n" + // 我们将要传入的每个顶点的颜色信息
                        "attribute vec3 a_Normal;                            \n" + // 我们将要传入的每个顶点的法线信息

                        "varying vec3 v_Position;                            \n" +
                        "varying vec4 v_Color;                               \n" +
                        "varying vec3 v_Normal;                              \n" +

                        // 顶点着色器入口点
                        "void main() " +
                        "{" +
                            // 将顶点位置转换成眼睛空间的位置
                        "   v_Position = vec3(u_MVMatrix * a_Position);" +
                            // 传入颜色
                        "   v_Color = a_Color;" +
                            // 将法线的方向转换在眼睛空间
                        "   v_Normal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));" +
                            // gl_Position是一个特殊的变量用来存储最终的位置
                            // 将顶点乘以矩阵得到标准化屏幕坐标的最终点
                        "   gl_Position = u_MVPMatrix * a_Position;" +
                        "}";
        return vertexShader;
    }

    protected String getFragmentShader() {
        final String fragmentShader =
                "precision mediump float;               \n" + // 我们将默认精度设置为中等，我们不需要片段着色器中的高精度
                        "varying vec4 v_Color;          \n" + // 这是从三角形每个片段内插的顶点着色器的颜色
                        "void main()                    \n" + // 片段着色器入口
                        "{                              \n" +
                        "   gl_FragColor = v_Color;     \n" + // 直接将颜色传递
                        "}                              \n";

        return fragmentShader;
    }

    protected String getFragmentShader2() {
        final String fragmentShader =
                "precision mediump float;" + //我们将默认精度设置为中等，我们不需要片段着色器中的高精度
                        "uniform vec3 u_LightPos;" + // 光源在眼睛空间的位置
                        "varying vec3 v_Position;" + // 插入的位置
                        "varying vec4 v_Color;" + // 插入的位置颜色
                        "varying vec3 v_Normal;" + // 插入的位置法线
                        "void main() " + // 片段着色器入口
                        "{" +
                            // 将用于哀减
                        "   float distance = length(u_LightPos - v_Position);" +
                            // 获取从光源到顶点方向的光线向量
                        "   vec3 lightVector = normalize(u_LightPos - v_Position);" +
                            // 计算光线矢量和顶点法线的点积，如果法线和光线矢量指向相同的方向，那么它将获得最大的照明
                        "   float diffuse = max(dot(v_Normal, lightVector), 0.1);" +
                            // 根据距离哀减光线
                        "   diffuse = diffuse * (1.0 / (1.0 + (0.25 * distance * distance)));" +
                            // 颜色乘以亮度哀减得到最终的颜色
                        "   gl_FragColor = v_Color * diffuse;" +
                        "}";
        return fragmentShader;
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

        final int vertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, getVertexShader2());
        final int fragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, getFragmentShader2());

        mPerVertexProgramHandle = createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, "a_Position", "a_Color", "a_Normal");
        // 定义一个简单的着色程序
        final String pointVertexShader =
                "uniform mat4 u_MVPMatrix;                  \n" +
                "attribute vec4 a_Position;                 \n" +
                "void main()                                \n" +
                "{                                          \n" +
                "   gl_Position = u_MVPMatrix * a_Position; \n" +
                "   gl_PointSize = 5.0;                     \n" +
                "}                                          \n";
        final String pointFragmentShader =
                "precision mediump float;                   \n" +
                "void main()                                \n" +
                "{                                          \n" +
                "   gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);\n" +
                "}                                          \n";

        final int pointVertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, pointVertexShader);
        final int pointFragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, pointFragmentShader);

        mPointProgramHandle = createAndLinkProgram(pointVertexShaderHandle, pointFragmentShaderHandle, "a_Position");

    }

    private int createAndLinkProgram(int vertexShaderHandle, int fragmentShaderHandle, String ... attributes) {
        // 创建一个OpenGL程序对象并将引用放进去
        int programHandle = GLES20.glCreateProgram();
        if (programHandle != 0) {
            // 绑定顶点着色器到程序对象中
            GLES20.glAttachShader(programHandle, vertexShaderHandle);
            // 绑定片段着色器到程序对象中
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);
            // 绑定属性
            if (attributes != null && attributes.length > 0) {
                for (int i = 0, size = attributes.length; i < size; i++) {
                    GLES20.glBindAttribLocation(programHandle, i, attributes[i]);
                }
            }
            // 将两个着色器连接到程序
            GLES20.glLinkProgram(programHandle);
            // 获取连接状态
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);
            // 如果连接失败，删除这程序
            if (linkStatus[0] == 0) {
                Log.e(TAG, "Error compiling program: " + GLES20.glGetProgramInfoLog(programHandle));
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }

        if (programHandle == 0) {
            throw new RuntimeException("Error creating program.");
        }

        return programHandle;
    }

    private int compileShader(int shaderType, String vertexShader) {
        int shaderHandle = GLES20.glCreateShader(shaderType);
        if (shaderHandle != 0) {
            // 传入顶点着色器源代码
            GLES20.glShaderSource(shaderHandle, vertexShader);
            // 编译顶点着色器
            GLES20.glCompileShader(shaderHandle);

            // 获取编译状态
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // 如果编译失败则删除着色器
            if (compileStatus[0] == 0) {
                Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shaderHandle));
                GLES20.glDeleteShader(shaderHandle);
                shaderHandle = 0;
            }
        }
        if (shaderHandle == 0) {
            throw new RuntimeException("Error creating shader.");
        }
        return shaderHandle;
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
        GLES20.glUseProgram(mPerVertexProgramHandle);

        mMVPMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_MVMatrix");
        mLightPosHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_LightPos");
        mPositionHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Color");
        mNormalHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Normal");

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
