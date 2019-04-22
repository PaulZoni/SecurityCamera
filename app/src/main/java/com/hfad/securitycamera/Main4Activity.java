package com.hfad.securitycamera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.google.android.cameraview.CameraView;
import java.io.ByteArrayOutputStream;


public class Main4Activity extends AppCompatActivity {

    private CameraView cameraView;
    private Manager networkManager;
    private static final int MY_CAMERA_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main4);

        cameraView = findViewById(R.id.camera_view);
        cameraView.start();
        new Thread(() -> {

            networkManager = Manager.create();
            networkManager.construct();

            cameraView.setOnFrameListener((data, width, height, rotationDegrees) -> {

                YuvImage image = new YuvImage(data, ImageFormat.NV21, width, height, null);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                Rect area = new Rect(0, 0, width, height);
                image.compressToJpeg(area, 50, out);
                Bitmap bm = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size());
                bm.getNinePatchChunk();

                networkManager.send(out.toByteArray());
                bm.recycle();

            });
        }).start();

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        super.onResume();

        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
        } else {
            cameraView.start();
        }

    }

    @Override
    protected void onPause() {
        cameraView.stop();
        super.onPause();
    }
}
