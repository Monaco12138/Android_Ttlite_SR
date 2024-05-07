package com.example.hello_java.analysis;
import com.example.hello_java.ml.Quicsr540p;
import com.example.hello_java.ml.QuicsrSmallx2;
import com.example.hello_java.utils.ImageProcess;

import android.os.FileUtils;
import android.content.Context;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.tensorflow.lite.DataType;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ImageProcessor;


import android.graphics.Bitmap;
import java.io.IOException;
import java.nio.ByteBuffer;

// 有两种方法加载使用模型，一种是把模型放入ml中，新版本的Android studio会自动帮你解析甚至生成一个封装好的类
// 第二种方法就是直接使用原始格式，放到Assets文件夹里面加载
public class Inference {
    // 这个tflite的输入输出要 和 真实的对应上
    Quicsr540p tfmodel;
    private final Size INPNUT_SIZE = new Size(960, 540);

    // output size: [b, c, h, w]
    private final int[] OUTPUT_SIZE = new int[] {3, 3840, 2160};
    public void initialModel(Context activity) {
        try {
            // 要tflite 2.16.1 版本才支持 Transpose version 6操作
            tfmodel = Quicsr540p.newInstance(activity);
            Log.i("tflite Support", "Success loading model");
        } catch (IOException e){
            Log.e("tflite Support", "Error loading model: ", e);
            Toast.makeText(activity, "load model error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public int[] superResolution(Bitmap bitmap) {

        // 这所有的操作都是针对[b, h, w, c]格式的
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(INPNUT_SIZE.getHeight(), INPNUT_SIZE.getWidth(), ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(0, 255))
                .build();

        // bitmap格式是[b,h,w,c]的
        TensorImage modelInput = new TensorImage(DataType.FLOAT32);
        modelInput.load(bitmap);
        modelInput = imageProcessor.process(modelInput);

        TensorBuffer hwcTensorBuffer = modelInput.getTensorBuffer();
        int[] shape = hwcTensorBuffer.getShape();
        int height = shape[0];
        int width = shape[1];
        int channel = shape[2];
        // [h,w,c] = [1920, 1080, 3]
        for (int i = 0; i < shape.length; i++) {
            Log.i("Debug input TensorBuffer shape", i + " " + shape[i]);
        }

        float[] hwcData = hwcTensorBuffer.getFloatArray();
        float[] chwData = new float[channel * height * width];
        for (int c = 0; c < channel; c++) {
            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    int hwcIndex = h * width * channel + w * channel + c;
                    int chwIndex = c * height * width + h * width + w;
                    chwData[chwIndex] = hwcData[hwcIndex];
                }
            }
        }

        TensorBuffer chwTensorBuffer = TensorBuffer.createFixedSize(new int[]{channel, height, width}, DataType.FLOAT32);
        chwTensorBuffer.loadArray(chwData);


        Quicsr540p.Outputs outputs = tfmodel.process(chwTensorBuffer);
        TensorBuffer chwOutputTensorBuffer = outputs.getOutputFeature0AsTensorBuffer();
        int[] outshape = chwOutputTensorBuffer.getShape();
        int outHeight = outshape[2];
        int outWidth = outshape[3];
        //outshape: [b, c, h, w] = [1, 3, 2160, 3840]
        for (int i = 0; i < outshape.length; i++) {
            Log.i("Debug output TensorBuffer shape", i + " " + outshape[i]);
        }

        //TensorBuffer to Bitmap
        float[] chwOutputData = chwOutputTensorBuffer.getFloatArray();
        int[] pixels = new int[outHeight * outWidth];
        int yp = 0;
        for (int h = 0; h < outHeight; h++) {
            for (int w = 0; w < outWidth; w++) {
                int r = (int) (chwOutputData[h * outWidth + w] * 255);
                int g = (int) (chwOutputData[outHeight * outWidth + h * outWidth + w] * 255);
                int b = (int) (chwOutputData[2 * outHeight * outWidth + h * outWidth + w] * 255);
                r = r > 255 ? 255 : (Math.max(r, 0));
                g = g > 255 ? 255 : (Math.max(g, 0));
                b = b > 255 ? 255 : (Math.max(b, 0));
                pixels[yp++] = 0xff000000 | (r << 16 & 0xff0000) | (g << 8 & 0xff00) | (b & 0xff);
            }
        }
        return pixels;
    }
}
