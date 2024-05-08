package com.example.hello_java.analysis;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;


import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.nnapi.NnApiDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import android.graphics.Bitmap;

public class InferenceInterpreter {
    private Interpreter tflite;
    Interpreter.Options options = new Interpreter.Options();

    private String MODEL_FILE = "quicsr540p.tflite";
    private final Size INPNUT_SIZE = new Size(960, 540);
    private final int[] OUTPUT_SIZE = new int[] {1, 3, 1080, 1920};

    public void initialModel(Context activity) {
        try {
            // 要tflite 2.16.1 版本才支持 Transpose version 6操作
            ByteBuffer tfliteModel = FileUtil.loadMappedFile(activity, MODEL_FILE);
            tflite = new Interpreter(tfliteModel, options);
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
        TensorBuffer chwInputTensorBuffer = TensorBuffer.createFixedSize(new int[]{channel, height, width}, DataType.FLOAT32);
        chwInputTensorBuffer.loadArray(chwData);

        TensorBuffer chwOutputTensorBuffer = TensorBuffer.createFixedSize(OUTPUT_SIZE, DataType.FLOAT32);

        if (tflite != null) {
            tflite.run(chwInputTensorBuffer.getBuffer(), chwOutputTensorBuffer.getBuffer());
        }
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

    public void addNNApiDelegate() {
        NnApiDelegate nnApiDelegate = null;
        // Initialize interpreter with NNAPI delegate for Android Pie or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            NnApiDelegate.Options nnApiOptions = new NnApiDelegate.Options();
//            nnApiOptions.setAllowFp16(true);
//            nnApiOptions.setUseNnapiCpu(true);
            //ANEURALNETWORKS_PREFER_LOW_POWER：倾向于以最大限度减少电池消耗的方式执行。这种设置适合经常执行的编译。
            //ANEURALNETWORKS_PREFER_FAST_SINGLE_ANSWER：倾向于尽快返回单个答案，即使这会耗费更多电量。这是默认值。
            //ANEURALNETWORKS_PREFER_SUSTAINED_SPEED：倾向于最大限度地提高连续帧的吞吐量，例如，在处理来自相机的连续帧时。
//            nnApiOptions.setExecutionPreference(NnApiDelegate.Options.EXECUTION_PREFERENCE_SUSTAINED_SPEED);
//            nnApiDelegate = new NnApiDelegate(nnApiOptions);
            nnApiDelegate = new NnApiDelegate();
            options.addDelegate(nnApiDelegate);
            Log.i("Debug tfliteSupport", "using nnapi delegate.");
        } else {
            addThread(4);
        }
    }

//    public void addGPUDelegate() {
//        CompatibilityList compatibilityList = new CompatibilityList();
//        if(compatibilityList.isDelegateSupportedOnThisDevice()){
//            GpuDelegate.Options delegateOptions = compatibilityList.getBestOptionsForThisDevice();
//            GpuDelegate gpuDelegate = new GpuDelegate(delegateOptions);
//            options.addDelegate(gpuDelegate);
//            Log.i("Debug tfliteSupport", "using gpu delegate.");
//        } else {
//            addThread(4);
//        }
//    }

    public void addThread(int thread) {
        options.setNumThreads(thread);
        Log.i("Debug tfliteSupport", "using addThread: " + thread);
    }
}
