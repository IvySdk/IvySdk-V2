package com.ivy.base.util;

import android.util.Base64;

import com.ivy.base.ILog;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.brotli.dec.BrotliInputStream;

public class BrotliDecompressor {

    // 私有构造防止实例化
    private BrotliDecompressor() {
    }

    public static String decompress(boolean enableBrotli, String data) {
        try {
            if (!enableBrotli) {
                return data;
            }
            byte[] deBase64 = Base64.decode(data, Base64.URL_SAFE);
            InputStream inputStream = new ByteArrayInputStream(deBase64);
            byte[] byteArray = decompress(inputStream);
            return new String(byteArray, "UTF-8");
        } catch (Exception e) {
            ILog.e("BrotliDecompressor", "decompress err:" + e.getMessage());
        }
        return null;
    }

    private static byte[] decompress(InputStream inputStream) throws Exception {
        BrotliInputStream brotliInput = new BrotliInputStream(inputStream);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = brotliInput.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        brotliInput.close();
        return outputStream.toByteArray();
    }
}
