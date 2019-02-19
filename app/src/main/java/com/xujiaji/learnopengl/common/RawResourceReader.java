package com.xujiaji.learnopengl.common;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RawResourceReader {
    public static String readTextFileFromRawResource(final Context context, final int resurceId) {
        final InputStream inputStream = context.getResources().openRawResource(resurceId);
        final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        String nextLine;

        final StringBuilder body = new StringBuilder();

        try {
            while ((nextLine = bufferedReader.readLine()) != null) {
                body.append(nextLine).append('\n');
            }
        } catch (IOException e) {
            return null;
        } finally {
            try {
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return body.toString();
    }
}
