package com.example.hello_java.analysis;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.Image;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;
import android.util.Log;
import com.example.hello_java.utils.ImageProcess;

public class ImageAnalyse implements ImageAnalysis.Analyzer{
    ImageView imageView;
    ImageProcess imageProcess;
    PreviewView previewView;
    public ImageAnalyse(PreviewView previewView, ImageView imageView) {
        this.imageView = imageView;
        this.previewView = previewView;
        this.imageProcess = new ImageProcess();
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        int previewHeight = previewView.getHeight();
        int previewWidth = previewView.getWidth();

        byte[][] yuvBytes = new byte[3][];
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        int imageHeight = image.getHeight();
        int imageWidth = image.getWidth();

        // (480, 640)
        Log.i("ImageProxy image size & Preview size:", imageHeight + " " + imageWidth + " & " + previewHeight + " " + previewWidth);
//        int rotationDegrees = image.getImageInfo().getRotationDegrees();
//        Log.i("image rotation2", "rotation: " + rotationDegrees);

        // 先将拿到的 ImageProxy 格式的图像转换为 字节数组
        imageProcess.fillBytes(planes, yuvBytes);

        int yRowStride = planes[0].getRowStride(); // Y通道一行数据所占的字节数目，由于可能有填充，大小接近weight
        final int uvRowStride = planes[1].getRowStride(); //U通道一行数据所占的字节数，由于可能有填充，大小接近weight/2
        final int uvPixelStride = planes[1].getPixelStride(); //U通道相邻两个像素之间的距离，为1表示紧密排序

        // 这里用int做存储，int是4个字节，所以表示的是ARGB四个属性了，每个属性只占一个字节
        int[] rgbBytes = new int[imageHeight * imageWidth];
        imageProcess.YUV420ToARGB8888(
                yuvBytes[0],
                yuvBytes[1],
                yuvBytes[2],
                imageWidth,
                imageHeight,
                yRowStride,
                uvRowStride,
                uvPixelStride,
                rgbBytes
        );

        // 原图bitmap，转成Bitmap格式方便在Android上进行处理和显示图像
        Bitmap imageBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
        imageBitmap.setPixels(rgbBytes, 0, imageWidth, 0, 0, imageWidth, imageHeight);

        // 旋转图像，Analyse 出来的每一帧图像都是默认相机传感器方向（横向的），在竖屏模式下图像会自动旋转90度，需要手动调整
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap fullImageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageWidth, imageHeight, matrix, false);

        imageView.setImageBitmap(fullImageBitmap);
        image.close();
    }
}
