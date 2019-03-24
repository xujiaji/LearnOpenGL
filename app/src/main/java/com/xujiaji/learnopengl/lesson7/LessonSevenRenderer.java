package com.xujiaji.learnopengl.lesson7;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.widget.Toast;

import com.xujiaji.learnopengl.R;
import com.xujiaji.learnopengl.common.RawResourceReader;
import com.xujiaji.learnopengl.common.ShaderHelper;
import com.xujiaji.learnopengl.common.ShapeBuilder;
import com.xujiaji.learnopengl.common.TextureHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class LessonSevenRenderer implements GLSurfaceView.Renderer {

    private LessonSevenActivity mLessonSevenActivity;
    private GLSurfaceView mGLSurfaceView;

    // 添加立方体生成信息
    private int mLastRequestedCubeFactor;
    private int mActualCubeFactor;

    private boolean mUseVBOs = true;
    /** 控制是否使用跨度 */
    private boolean mUseStride = true;

    /** 一个位置数据的大小 */
    static final int POSITION_DATA_SIZE = 3;

    /** 每个法线数据的大小 */
    static final int NORMAL_DATA_SIZE = 3;

    /** 每个坐标数据的尺寸 */
    static final int TEXTURE_COORDINATE_DATA_SIZE = 2;

    /** 每float多少字节 */
    static final int BYTES_PER_FLOAT = 4;

    private float[] mModelMatrix = new float[16];
    // 存储累积的旋转值
    private final float[] mAccumulatedRotation = new float[16];

    // 存放当前的旋转
    private final float[] mCurrentRotation = new float[16];

    // 临时矩阵
    private float[] mTemporaryMatrix = new float[16];

    // 存储view矩阵。可以认为这是一个相机，我们通过相机将世界空间转换为眼睛空间
    // 它定位相对于我们眼睛的东西
    private float[] mViewMatrix = new float[16];

    // 存放投影矩阵，用于将场景投影到2D视角
    private float[] mProjectionMatrix = new float[16];

    // 存放光源位置的特别矩阵副本
    private float[] mLightModelMatrix = new float[16];

    private float[] mMVPMatrix = new float[16];

    // 用来存放光源在模型空间的初始位置，我们需要第四个坐标
    // 这样我们就可以通过变换矩阵将它们相乘来实现平移
    private final float[] mLightPosInModelSpace = new float[] {0.0f, 0.0f, 0.0f, 1.0f};

    // 用来存放当前光源在世界空间中的位置
    private final float[] mLightPosInWorldSpace = new float[4];

    // 用来存放当前光源在眼睛空间中的位置
    private final float[] mLightPosInEyeSpace = new float[4];


    // 程序
    private int mProgramHandle;
    // Android图标
    private int mAndroidDataHandle;
    private int mMVPMatrixHandle;
    private int mMVMatrixHandle;
    private int mLightPosHandle;
    private int mPositionHandle;
    private int mNormalHandle;
    private int mTextureUniformHandle;
    private int mTextureCoordinateHandle;

    // 没有volatile仍然工作，但是不保证能刷新
    public volatile float mDeltaX;
    public volatile float mDeltaY;

    // 在后台生成立方体数据的线程池
    private final ExecutorService mSingleThreadedExecutor = Executors.newSingleThreadExecutor();

    // 当前的所有立方体对象
    private Cubes mCubes;

    public LessonSevenRenderer(final LessonSevenActivity lessonSevenActivity, final GLSurfaceView glSurfaceView) {
        this.mLessonSevenActivity = lessonSevenActivity;
        this.mGLSurfaceView = glSurfaceView;
    }

    private void generateCubes(int cubeFactor, boolean toggleVbos, boolean toggleStride) {
        mSingleThreadedExecutor.submit(new GenDataRunnable(cubeFactor, toggleVbos, toggleStride));
    }

    class GenDataRunnable implements Runnable {

        final int mRequestedCubeFactor;
        final boolean mToggleVbos;
        final boolean mToggleStride;

        GenDataRunnable(int requestedCubeFactor, boolean toggleVbos, boolean toggleStride) {
            this.mRequestedCubeFactor = requestedCubeFactor;
            this.mToggleVbos = toggleVbos;
            this.mToggleStride = toggleStride;
        }

        @Override
        public void run() {
            try {

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

                final float[] cubePositionData = new float[108 * mRequestedCubeFactor * mRequestedCubeFactor * mRequestedCubeFactor];
                int cubePositionDataOffset = 0;

                final int segments = mRequestedCubeFactor + (mRequestedCubeFactor - 1);
                final float minPosition = -1.0f;
                final float maxPosition = 1.0f;
                final float positionRange = maxPosition - minPosition;

                for (int x = 0; x < mRequestedCubeFactor; x++) {
                    for (int y = 0; y < mRequestedCubeFactor; y++) {
                        for (int z = 0; z < mRequestedCubeFactor; z++) {
                            final float x1 = minPosition + ((positionRange / segments) * (x * 2));
                            final float x2 = minPosition + ((positionRange / segments) * ((x * 2) + 1));

                            final float y1 = minPosition + ((positionRange / segments) * (y * 2));
                            final float y2 = minPosition + ((positionRange / segments) * ((y * 2) + 1));

                            final float z1 = minPosition + ((positionRange / segments) * (z * 2));
                            final float z2 = minPosition + ((positionRange / segments) * ((z * 2) + 1));

                            // Define points for a cube.
                            // X, Y, Z
                            final float[] p1p = { x1, y2, z2 };
                            final float[] p2p = { x2, y2, z2 };
                            final float[] p3p = { x1, y1, z2 };
                            final float[] p4p = { x2, y1, z2 };
                            final float[] p5p = { x1, y2, z1 };
                            final float[] p6p = { x2, y2, z1 };
                            final float[] p7p = { x1, y1, z1 };
                            final float[] p8p = { x2, y1, z1 };

                            final float[] thisCubePositionData = ShapeBuilder.generateCubeData(p1p, p2p, p3p, p4p, p5p, p6p, p7p, p8p,
                                    p1p.length);

                            System.arraycopy(thisCubePositionData, 0, cubePositionData, cubePositionDataOffset, thisCubePositionData.length);
                            cubePositionDataOffset += thisCubePositionData.length;
                        }
                    }
                }

                // 在OpenGL 线程运行 -- 其他渲染器
                mGLSurfaceView.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        if (mCubes != null) {
                            mCubes.release();
                            mCubes = null;
                        }

                        // 不因手动调用，但Dalvik有时需要一些额外的刺激来清理堆
                        System.gc();

                        try {
                            boolean useVbos = mUseVBOs;
                            boolean useStride = mUseStride;

                            if (mToggleVbos) {
                                useVbos = !useVbos;
                            }

                            if (mToggleStride) {
                                useStride = !useStride;
                            }

                            if (useStride) {
                                if (useVbos) {
                                    mCubes = new CubesWithVboWithStride(cubePositionData, cubeNormalData, cubeTextureCoordinateData, mRequestedCubeFactor);
                                } else {
                                    mCubes = new CubesClientSideWithStride(cubePositionData, cubeNormalData, cubeTextureCoordinateData, mRequestedCubeFactor);
                                }
                            } else {
                                if (useVbos) {
                                    mCubes = new CubesWithVbo(cubePositionData, cubeNormalData, cubeTextureCoordinateData, mRequestedCubeFactor);
                                } else {
                                    mCubes = new CubesClientSide(cubePositionData, cubeNormalData, cubeTextureCoordinateData, mRequestedCubeFactor);
                                }
                            }

                            mUseVBOs = useVbos;
                            mLessonSevenActivity.updateVboStatus(mUseVBOs);

                            mUseStride = useStride;
                            mLessonSevenActivity.updateStrideStatus(mUseStride);

                            mActualCubeFactor = mRequestedCubeFactor;
                        } catch (OutOfMemoryError err) {
                            if (mCubes != null) {
                                mCubes.release();
                                mCubes = null;
                            }

                            // 不因手动调用，但Dalvik有时需要一些额外的刺激来清理堆
                            System.gc();

                            mLessonSevenActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
//									Toast.makeText(mLessonSevenActivity, "Out of memory; Dalvik takes a while to clean up the memory. Please try again.\nExternal bytes allocated=" + dalvik.system.VMRuntime.getRuntime().getExternalBytesAllocated(), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                });
            } catch (OutOfMemoryError error) {
                System.gc();
                mLessonSevenActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mLessonSevenActivity, "OOM", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }

    public void decreaseCubeCount() {
        if (mLastRequestedCubeFactor > 1) {
            generateCubes(--mLastRequestedCubeFactor, false, false);
        }
    }

    public void increaseCubeCount() {
        if (mLastRequestedCubeFactor < 16) {
            generateCubes(++mLastRequestedCubeFactor, false, false);
        }
    }

    public void toggleVBOs() {
        generateCubes(mLastRequestedCubeFactor, true, false);
    }

    public void toggleStride() {
        generateCubes(mLastRequestedCubeFactor, false, true);
    }


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mLastRequestedCubeFactor = mActualCubeFactor = 3;
        generateCubes(mActualCubeFactor, false, false);

        // 设置背景清理颜色
        GLES20.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);

        // 开启剔除背面
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // 开启深度测试
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // 眼睛的位置在原点前方
        final float eyeX = 0.0F;
        final float eyeY = 0.0F;
        final float eyeZ = -0.5F;

        // 我们朝哪里看
        final float lookX = 0.0F;
        final float lookY = 0.0F;
        final float lookZ = -5.0F;

        // 设置我们的向量，这是我们拿着相机时头指向的方向
        final float upX = 0.0F;
        final float upY = 1.0F;
        final float upZ = 0.0F;

        // 设置view矩阵，可以说这个矩阵代表了相机的位置
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

        final String vertexShader = RawResourceReader.readTextFileFromRawResource(mLessonSevenActivity, R.raw.lesson_seven_vertex_shader);
        final String fragmentShader = RawResourceReader.readTextFileFromRawResource(mLessonSevenActivity, R.raw.lesson_seven_fragment_shader);

        final int vertexShaderHandler = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        final int fragmentShaderHandler = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        mProgramHandle = ShaderHelper.createAndLinkProgram(vertexShaderHandler, fragmentShaderHandler,
                "a_Position", "a_Normal", "a_TexCoordinate");

        // 加载纹理
        mAndroidDataHandle = TextureHelper.loadTexture(mLessonSevenActivity, R.drawable.usb_android);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mAndroidDataHandle);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mAndroidDataHandle);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);

        // 初始化累积的选择值
        Matrix.setIdentityM(mAccumulatedRotation, 0);
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
        final float far = 1000.0F;

        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // 设置每顶点照明程序
        GLES20.glUseProgram(mProgramHandle);

        // 立方体绘制的handle
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVMatrix");
        mLightPosHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_LightPos");
        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        mNormalHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Normal");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");

        Matrix.setIdentityM(mLightModelMatrix, 0);
        Matrix.translateM(mLightModelMatrix, 0, 0.0F, 0.0F, -1.0F);

        Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0);

        // 绘制一个立方体
        // 移动这个立方体到屏幕中
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0F, 0.0F, -3.5F);

        // 设置矩阵当前的旋转
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

        // view矩阵乘以model矩阵，结果存放到MVP矩阵
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

        // 传入modelview矩阵
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);

        // modelview矩阵乘以projection矩阵，结果放到MVP矩阵
        Matrix.multiplyMM(mTemporaryMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        System.arraycopy(mTemporaryMatrix, 0, mMVPMatrix, 0, 16);

        // 传入组合矩阵
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // 传入眼睛空间中的光源位置
        GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0],  mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);

        // 传入纹理信息
        // 使用纹理单元0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // 绑定纹理到这个单元
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mAndroidDataHandle);

        // 通过绑定到纹理单元0，告诉纹理统一采样器在着色器中使用此纹理
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        if (mCubes != null) {
            mCubes.render();
        }
    }

    abstract class Cubes {
        abstract void render();

        abstract void release();

        FloatBuffer[] getBuffers(float[] cubePositions, float[] cubeNormals, float[] cubeTextureCoordinates, int generatedCubeFactor) {
            // 首先，拷贝立方体信息到客户端浮点缓冲区
            final FloatBuffer cubePositionsBuffer;
            final FloatBuffer cubeNormalsBuffer;
            final FloatBuffer cubeTextureCoordinatesBuffer;

            cubePositionsBuffer = ByteBuffer.allocateDirect(cubePositions.length * BYTES_PER_FLOAT)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            cubePositionsBuffer.put(cubePositions).position(0);

            cubeNormalsBuffer = ByteBuffer.allocateDirect(cubeNormals.length * BYTES_PER_FLOAT * generatedCubeFactor * generatedCubeFactor * generatedCubeFactor)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            cubeNormalsBuffer.put(cubeNormals).position(0);

            for (int i = 0; i < (generatedCubeFactor * generatedCubeFactor * generatedCubeFactor); i++) {
                cubeNormalsBuffer.put(cubeNormals);
            }
            cubeNormalsBuffer.position(0);

            cubeTextureCoordinatesBuffer = ByteBuffer.allocateDirect(cubeTextureCoordinates.length * BYTES_PER_FLOAT * generatedCubeFactor * generatedCubeFactor * generatedCubeFactor)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            for (int i = 0; i < (generatedCubeFactor * generatedCubeFactor * generatedCubeFactor); i++) {
                cubeTextureCoordinatesBuffer.put(cubeTextureCoordinates);
            }

            cubeTextureCoordinatesBuffer.position(0);
            return new FloatBuffer[] {cubePositionsBuffer, cubeNormalsBuffer, cubeTextureCoordinatesBuffer};
        }

        FloatBuffer getInterleavedBuffer(float[] cubePositions, float[] cubeNormals, float[] cubeTextureCoordinates, int generatedCubeFactor) {
            final int cubeDataLength = cubePositions.length
                    + (cubeNormals.length * generatedCubeFactor * generatedCubeFactor * generatedCubeFactor)
                    + (cubeTextureCoordinates.length * generatedCubeFactor * generatedCubeFactor * generatedCubeFactor);
            int cubePositionOffset = 0;
            int cubeNormalOffset = 0;
            int cubeTextureOffset = 0;


            final FloatBuffer cubeBuffer = ByteBuffer.allocateDirect(cubeDataLength * BYTES_PER_FLOAT)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();

            for (int i = 0; i < generatedCubeFactor * generatedCubeFactor * generatedCubeFactor; i++) {
                for (int v = 0; v < 36; v++) {
                    cubeBuffer.put(cubePositions, cubePositionOffset, POSITION_DATA_SIZE);
                    cubePositionOffset += POSITION_DATA_SIZE;
                    cubeBuffer.put(cubeNormals, cubeNormalOffset, NORMAL_DATA_SIZE);
                    cubeNormalOffset += NORMAL_DATA_SIZE;
                    cubeBuffer.put(cubeTextureCoordinates, cubeTextureOffset, TEXTURE_COORDINATE_DATA_SIZE);
                    cubeTextureOffset += TEXTURE_COORDINATE_DATA_SIZE;
                }

                // 为每个立方体重置纹理和法线数据
                cubeNormalOffset = 0;
                cubeTextureOffset = 0;
            }

            cubeBuffer.position(0);

            return cubeBuffer;
        }
    }

    class CubesClientSide extends Cubes {
        private FloatBuffer mCubePositions;
        private FloatBuffer mCubeNormals;
        private FloatBuffer mCubeTextureCoordinates;

        CubesClientSide(float[] cubePositions, float[] cubeNormals, float[] cubeTextureCoordinates, int generatedCubeFactor) {
            FloatBuffer[] buffers = getBuffers(cubePositions, cubeNormals, cubeTextureCoordinates, generatedCubeFactor);

            mCubePositions = buffers[0];
            mCubeNormals = buffers[1];
            mCubeTextureCoordinates = buffers[2];
        }

        @Override
        void render() {
            // 传入位置信息
            GLES20.glEnableVertexAttribArray(mPositionHandle);
            GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, mCubePositions);

            // 传入法线信息
            GLES20.glEnableVertexAttribArray(mNormalHandle);
            GLES20.glVertexAttribPointer(mNormalHandle, NORMAL_DATA_SIZE, GLES20.GL_FLOAT, false, 0, mCubeNormals);

            // 传入纹理信息
            GLES20.glEnableVertexAttribArray(mTextureUniformHandle);
            GLES20.glVertexAttribPointer(mTextureCoordinateHandle, TEXTURE_COORDINATE_DATA_SIZE, GLES20.GL_FLOAT, false,
                    0, mCubeTextureCoordinates);

            // 绘制立方体
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mActualCubeFactor * mActualCubeFactor * mActualCubeFactor * 36);
        }

        @Override
        void release() {
            mCubePositions.limit(0);
            mCubePositions = null;
            mCubeNormals.limit(0);
            mCubeNormals = null;
            mCubeTextureCoordinates.limit(0);
            mCubeTextureCoordinates = null;
        }
    }


    class CubesClientSideWithStride extends Cubes {
        private FloatBuffer mCubeBuffer;

        CubesClientSideWithStride(float[] cubePositions, float[] cubeNormals, float[] cubeTextureCoordinates, int generatedCubeFactor) {
            mCubeBuffer = getInterleavedBuffer(cubePositions, cubeNormals, cubeTextureCoordinates, generatedCubeFactor);
        }

        @Override
        public void render() {
            final int stride = (POSITION_DATA_SIZE + NORMAL_DATA_SIZE + TEXTURE_COORDINATE_DATA_SIZE) * BYTES_PER_FLOAT;

            // 传入位置信息
            mCubeBuffer.position(0);
            GLES20.glEnableVertexAttribArray(mPositionHandle);
            GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, stride, mCubeBuffer);

            // 传入法线信息
            mCubeBuffer.position(POSITION_DATA_SIZE);
            GLES20.glEnableVertexAttribArray(mNormalHandle);
            GLES20.glVertexAttribPointer(mNormalHandle, NORMAL_DATA_SIZE, GLES20.GL_FLOAT, false, stride, mCubeBuffer);

            // 传入纹理信息
            mCubeBuffer.position(POSITION_DATA_SIZE + NORMAL_DATA_SIZE);
            GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
            GLES20.glVertexAttribPointer(mTextureCoordinateHandle, TEXTURE_COORDINATE_DATA_SIZE, GLES20.GL_FLOAT, false,
                    stride, mCubeBuffer);

            // 绘制立方体
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mActualCubeFactor * mActualCubeFactor * mActualCubeFactor * 36);
        }

        @Override
        public void release() {
            mCubeBuffer.limit(0);
            mCubeBuffer = null;
        }
    }

    class CubesWithVbo extends Cubes {
        final int mCubePositionsBufferIdx;
        final int mCubeNormalsBufferIdx;
        final int mCubeTexCoordsBufferIdx;

        CubesWithVbo(float[] cubePositions, float[] cubeNormals, float[] cubeTextureCoordinates, int generatedCubeFactor) {
            FloatBuffer[] floatBuffers = getBuffers(cubePositions, cubeNormals, cubeTextureCoordinates, generatedCubeFactor);

            FloatBuffer cubePositionsBuffer = floatBuffers[0];
            FloatBuffer cubeNormalsBuffer = floatBuffers[1];
            FloatBuffer cubeTextureCoordinatesBuffer = floatBuffers[2];

            // 第二， 拷贝这些缓冲到OpenGL的内存。然后，我们不需要再保留客户端缓冲区
            final int buffers[] = new int[3];
            GLES20.glGenBuffers(3, buffers, 0);

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, cubePositionsBuffer.capacity() * BYTES_PER_FLOAT, cubePositionsBuffer, GLES20.GL_STATIC_DRAW);

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[1]);
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, cubeNormalsBuffer.capacity() * BYTES_PER_FLOAT, cubeNormalsBuffer, GLES20.GL_STATIC_DRAW);

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[2]);
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, cubeTextureCoordinatesBuffer.capacity() * BYTES_PER_FLOAT, cubeTextureCoordinatesBuffer,
                    GLES20.GL_STATIC_DRAW);

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

            mCubePositionsBufferIdx = buffers[0];
            mCubeNormalsBufferIdx = buffers[1];
            mCubeTexCoordsBufferIdx = buffers[2];

            cubePositionsBuffer.limit(0);
            cubePositionsBuffer = null;
            cubeNormalsBuffer.limit(0);
            cubeNormalsBuffer = null;
            cubeTextureCoordinatesBuffer.limit(0);
            cubeTextureCoordinatesBuffer = null;
        }

        @Override
        public void render() {
            // 传入位置信息
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mCubePositionsBufferIdx);
            GLES20.glEnableVertexAttribArray(mPositionHandle);
            GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, 0);

            // 传入法线信息
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mCubeNormalsBufferIdx);
            GLES20.glEnableVertexAttribArray(mNormalHandle);
            GLES20.glVertexAttribPointer(mNormalHandle, NORMAL_DATA_SIZE, GLES20.GL_FLOAT, false, 0, 0);

            // 传入纹理信息
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mCubeTexCoordsBufferIdx);
            GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
            GLES20.glVertexAttribPointer(mTextureCoordinateHandle, TEXTURE_COORDINATE_DATA_SIZE, GLES20.GL_FLOAT, false,
                    0, 0);

            // 清理当前绑定的缓冲（因此以后的OpenGL调用不在使用此缓冲区）
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

            // 绘制立方体
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mActualCubeFactor * mActualCubeFactor * mActualCubeFactor * 36);
        }

        @Override
        public void release() {
            // 从OpenGL的内存中删除缓冲区
            final int[] buffersToDelete = new int[] { mCubePositionsBufferIdx, mCubeNormalsBufferIdx,
                    mCubeTexCoordsBufferIdx };
            GLES20.glDeleteBuffers(buffersToDelete.length, buffersToDelete, 0);
        }
    }

    class CubesWithVboWithStride extends Cubes {
        final int mCubeBufferIdx;

        CubesWithVboWithStride(float[] cubePositions, float[] cubeNormals, float[] cubeTextureCoordinates, int generatedCubeFactor) {
            FloatBuffer cubeBuffer = getInterleavedBuffer(cubePositions, cubeNormals, cubeTextureCoordinates, generatedCubeFactor);

            // 第二， 拷贝这些缓冲到OpenGL的内存。然后，我们不需要再保留客户端缓冲区
            final int buffers[] = new int[1];
            GLES20.glGenBuffers(1, buffers, 0);

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, cubeBuffer.capacity() * BYTES_PER_FLOAT, cubeBuffer, GLES20.GL_STATIC_DRAW);

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

            mCubeBufferIdx = buffers[0];

            cubeBuffer.limit(0);
            cubeBuffer = null;
        }

        @Override
        public void render() {
            final int stride = (POSITION_DATA_SIZE + NORMAL_DATA_SIZE + TEXTURE_COORDINATE_DATA_SIZE) * BYTES_PER_FLOAT;

            // 传入位置信息
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mCubeBufferIdx);
            GLES20.glEnableVertexAttribArray(mPositionHandle);
            GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, stride, 0);

            // 传入法线信息
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mCubeBufferIdx);
            GLES20.glEnableVertexAttribArray(mNormalHandle);
            GLES20.glVertexAttribPointer(mNormalHandle, NORMAL_DATA_SIZE, GLES20.GL_FLOAT, false, stride, POSITION_DATA_SIZE * BYTES_PER_FLOAT);

            // 传入纹理信息
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mCubeBufferIdx);
            GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
            GLES20.glVertexAttribPointer(mTextureCoordinateHandle, TEXTURE_COORDINATE_DATA_SIZE, GLES20.GL_FLOAT, false,
                    stride, (POSITION_DATA_SIZE + NORMAL_DATA_SIZE) * BYTES_PER_FLOAT);

            // 清理当前绑定的缓冲（因此以后的OpenGL调用不在使用此缓冲区）
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

            // 绘制立方体
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mActualCubeFactor * mActualCubeFactor * mActualCubeFactor * 36);
        }

        @Override
        public void release() {
            // 从OpenGL的内存中删除缓冲区
            final int[] buffersToDelete = new int[] { mCubeBufferIdx };
            GLES20.glDeleteBuffers(buffersToDelete.length, buffersToDelete, 0);
        }
    }
}
