package com.xujiaji.learnopengl.common;

import android.opengl.GLES20;
import android.util.Log;

public class ShaderHelper {

    private static final String TAG = "ShaderHelper";

    public static int compileShader(int shaderType, String vertexShader) {
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



    public static int createAndLinkProgram(int vertexShaderHandle, int fragmentShaderHandle, String ... attributes) {
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
}
