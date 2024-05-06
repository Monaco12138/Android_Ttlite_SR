package com.example.hello_java;

// 开始自带的
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.hello_java.analysis.ImageAnalyse;
import com.example.hello_java.utils.CameraProcess;
import com.google.common.util.concurrent.ListenableFuture;

import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
// 摄像头模组
//import androidx.camera.view.PreviewView;

public class MainActivity extends AppCompatActivity {

    private PreviewView cameraPreviewWrap;
    private ImageView imageView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private CameraProcess cameraProcess = new CameraProcess();

    private boolean Is_Super_Resolution = false;
    private Switch immersive;

    public int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 打开app时候隐藏顶部状态栏
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        // 全图画面
        cameraPreviewWrap = findViewById(R.id.camera_preview_wrap);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        imageView = findViewById(R.id.box_label_canvas);

        //sr botton
        immersive = findViewById(R.id.immersive);

        // 申请摄像头权限
        if (!cameraProcess.allPermissionsGranted(this)) {
            cameraProcess.requestPermissions(this);
        }

        // 获取手机摄像头拍照旋转参数
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Log.i("image rotation", "rotation: " + rotation);


        ImageAnalyse imageAnalyse = new ImageAnalyse(cameraPreviewWrap, imageView);

        cameraProcess.showCameraSupportSize(MainActivity.this);

        cameraProcess.startCamera(MainActivity.this, imageAnalyse, cameraPreviewWrap);
//        //监听botton
//        immersive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                Is_Super_Resolution = isChecked;
//                ImageAnalyse imageAnalyse = new ImageAnalyse(imageView);
//
//                cameraProcess.startCamera(MainActivity.this, imageAnalyse, cameraPreviewWrap);
//
////                if (!isChecked) { // 普通模式，放一个摄像头预览画面
////                    cameraProcess.startCamera(MainActivity.this, cameraPreviewWrap);
////                }
//            }
//        });
    }


}