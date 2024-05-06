package com.example.hello_java.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;
import android.util.Size;

import com.example.hello_java.analysis.ImageAnalyse;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import androidx.camera.core.Preview;
import androidx.camera.core.CameraSelector;
import androidx.lifecycle.LifecycleOwner;
import androidx.camera.view.PreviewView;
public class CameraProcess {
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE"};

    /**
     * 判断摄像头权限
     * @param context
     * @return
     */
    public boolean allPermissionsGranted(Context context) {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 申请摄像头权限
     * @param activity
     */
    public void requestPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
    }

    /**
     *  打开摄像头获取预览画面
     */
    public void startCamera(Context context, ImageAnalysis.Analyzer analyzer, PreviewView previewView) {
        cameraProviderFuture = ProcessCameraProvider.getInstance(context);

        // 传递一个Runnable()给监听器，通过(ContextCompat.getMainExecutor(context))指定应该在主线程（UI线程）上运行
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                            //.setTargetAspectRatio(AspectRatio.RATIO_4_3)
                            .setTargetResolution(new Size(1080, 1920))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build();
                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer);
                    bindPreview(context, cameraProvider, previewView, imageAnalysis);
                } catch (ExecutionException | InterruptedException e) {
                    // Handle any errors (in this case, an exception thrown if the camera is in use or not available)
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void bindPreview(@NonNull Context context, ProcessCameraProvider cameraProvider, PreviewView previewView, ImageAnalysis imageAnalysis) {

        //创建Preview对象，Preview对象为CameraX的一个用于显示相机视图的组件，如果不想摄像头显示预览画面，可以去掉
        //Preview previewBuilder = new Preview.Builder().build();

        //配置摄像头，LENS_FACING_BACK 表示后置摄像头
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // 这行代码是将Preview 对象的输出 连接到 PreviewView视图上，PreviewView是我们创建的一个专用的视图，用于显示来自相机的实时图像
        //previewBuilder.setSurfaceProvider(previewView.createSurfaceProvider());

        // 将摄像头的生命周期绑定到 我们的 MainActivity 里面，确保了相机在 MainActivity 不活动时不会继续运行，有效的管理资源
        cameraProvider.bindToLifecycle((LifecycleOwner) context, cameraSelector, imageAnalysis);

    }

    /**
     * 打印输出摄像头支持的宽和高
     * @param activity
     */
    public void showCameraSupportSize(Activity activity) {
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics cc = manager.getCameraCharacteristics(id);
                if (cc.get(CameraCharacteristics.LENS_FACING) == 1) {
                    Size[] previewSizes = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                            .getOutputSizes(SurfaceTexture.class);
                    for (Size s : previewSizes){
                        Log.i("camera supprot size", s.getHeight()+"/"+s.getWidth());
                    }
                    break;

                }
            }
        } catch (Exception e) {
            Log.e("image", "can not open camera", e);
        }
    }

}
