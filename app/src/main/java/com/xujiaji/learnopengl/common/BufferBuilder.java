package com.xujiaji.learnopengl.common;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class BufferBuilder {

    // 每个float由4个字节组成
    private final static int BYTE_PER_FLOAT = 4;

    public static FloatBuffer generate(float[] data) {
        FloatBuffer fb = ByteBuffer.allocateDirect(data.length * BYTE_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        fb.put(data).position(0);
        return fb;
    }

}
