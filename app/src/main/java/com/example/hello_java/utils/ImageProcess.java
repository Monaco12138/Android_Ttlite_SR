package com.example.hello_java.utils;

import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

public class ImageProcess {
    private int kMaxChannelValue = 262143;

    /**
     * cameraX planes数据处理成yuv字节数组
     * @param planes
     * @param yuvBytes
     */
    public void fillBytes(final ImageProxy.PlaneProxy[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    /**
     * YUV转RGB
     * @param y
     * @param u
     * @param v
     * @return
     */
    public int YUV2RGB(int y, int u, int v) {
        // Adjust and check YUV values
        y = (y - 16) < 0 ? 0 : (y - 16);
        u -= 128;
        v -= 128;

        // This is the floating point equivalent. We do the conversion in integer
        // because some Android devices do not have floating point in hardware.
        // nR = (int)(1.164 * nY + 1.596 * nV);
        // nG = (int)(1.164 * nY - 0.813 * nV - 0.391 * nU);
        // nB = (int)(1.164 * nY + 2.018 * nU);
        // 这里取的系数为1024，两边都同时乘上1024
        int y1192 = 1192 * y;
        int r = (y1192 + 1634 * v);
        int g = (y1192 - 833 * v - 400 * u);
        int b = (y1192 + 2066 * u);

        // Clipping RGB values to be inside boundaries [ 0 , kMaxChannelValue ]
        // KMaxChannelValue = 262143 即 2^18，原始范围应该限制在[0,255]之间，由于换成整数乘了1024故在[0,2^18]
        r = r > kMaxChannelValue ? kMaxChannelValue : (r < 0 ? 0 : r);
        g = g > kMaxChannelValue ? kMaxChannelValue : (g < 0 ? 0 : g);
        b = b > kMaxChannelValue ? kMaxChannelValue : (b < 0 ? 0 : b);

        // 本来应该是 int rgb = (0xFF << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF); ARGB
        // 但是由于转换时乘了1024，需要除以1024，所以是如下的表达式
        return 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
    }

    /**
     * YUV420T转ARGB8888
     * @param yData
     * @param uData
     * @param vData
     * @param width
     * @param height
     * @param yRowStride
     * @param uvRowStride
     * @param uvPixelStride
     * @param out
     */
    public void YUV420ToARGB8888(
            byte[] yData,
            byte[] uData,
            byte[] vData,
            int width,
            int height,
            int yRowStride, // 近似weight
            int uvRowStride, // 近似weight/2
            int uvPixelStride, // 近似为1
            int[] out) {

        /*             <------------Y-linesize----------->
         *             <-------------width------------>
         *             -----------------------------------
         *             |                              |  |
         *             |                              |  |
         *   height    |              Y               |  |
         *             |                              |  |
         *             |                              |  |
         *             |                              |  |
         *             -----------------------------------
         *             |             |  |             |  |
         * height / 2  |      U      |  |      V      |  |
         *             |             |  |             |  |
         *             -----------------------------------
         *             <---U-linesize--> <--V-linesize--->
         *             <---U-width--->   <--V-width--->
         in_memory:
         yData: <-------- Y --------->
                   height * width
         uData:  <---- U ---->
                   h/2 * w/2
         vData:  <---- V ---->
                   h/2 * w/ 2
        */
        int yp = 0;
        for (int j = 0; j < height; j++) {
            int pY = yRowStride * j;
            int pUV = uvRowStride * (j >> 1);

            for (int i = 0; i < width; i++) {
                int uv_offset = pUV + (i >> 1) * uvPixelStride;

                // oxff & yData 是把有符号的数据转换为无符合的数据，java中byte是有符号的范围在-128~127之间，我们处理成像素值需要转换到无符号0-255
                out[yp++] = YUV2RGB(0xff & yData[pY + i], 0xff & uData[uv_offset], 0xff & vData[uv_offset]);
            }
        }
    }
}
