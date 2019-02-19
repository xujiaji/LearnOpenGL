package com.xujiaji.learnopengl.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

public class TextureHelper {

    public static int loadTexture(final Context context, final int resourceId) {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);
        if (textureHandle[0] == 0) {
            throw new RuntimeException("Error loading texture.");
        }


        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false; // 没有预先缩放

        // 得到图片资源
        final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

        // 在OpenGL中绑定纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

        // 设置过滤
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        // 将位图加载到已绑定的纹理中
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        // 回收位图，因为它的数据已加载到OpenGL中
        bitmap.recycle();

        return textureHandle[0];
    }
}
