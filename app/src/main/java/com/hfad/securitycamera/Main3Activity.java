package com.hfad.securitycamera;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.hardware.camera2.*;
import android.os.*;
import android.view.*;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.util.Size;
import android.util.SparseIntArray;
import org.jetbrains.annotations.NotNull;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Main3Activity extends AppCompatActivity {

    private Size previewsize;
    private TextureView textureView;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder previewBuilder;
    private CameraCaptureSession previewSession;
    private static final SparseIntArray ORIENTATIONS=new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0,90);
        ORIENTATIONS.append(Surface.ROTATION_90,0);
        ORIENTATIONS.append(Surface.ROTATION_180,270);
        ORIENTATIONS.append(Surface.ROTATION_270,180);
    }

    private TextureView.SurfaceTextureListener surfaceTextureListener=new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);

        textureView = findViewById(R.id.textureview);
        textureView.setSurfaceTextureListener(surfaceTextureListener);
    }

    @SuppressLint("MissingPermission")
    public  void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String camerId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(camerId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            previewsize = map.getOutputSizes(SurfaceTexture.class)[0];
            manager.openCamera(camerId, stateCallback,null);
        } catch (Exception e) {}
    }

    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NotNull CameraDevice camera) {
            cameraDevice = camera;
            startCamera();

        }
        @Override
        public void onDisconnected(@NotNull CameraDevice camera) {
        }
        @Override
        public void onError(@NotNull CameraDevice camera, int error) {
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraDevice!=null) {
            cameraDevice.close();
        }
    }

    private ImageReader.OnImageAvailableListener imageAvailableListener = reader1 -> {

        Image image = null;
        ByteBuffer buffer = null;
        try {
            image = reader1.acquireLatestImage();
            buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];


            System.out.println("IMAGE_READER");


        } catch (Exception ee) {
            System.out.println("NOT_AVAILABLE");
        }

        finally {
            if(image!=null)
                image.close();

            if (buffer != null) {
                buffer.clear();
            }

        }
    };

    void startCamera() {
        if(cameraDevice == null || !textureView.isAvailable() || previewsize == null) {
            return;
        }

        SurfaceTexture texture=textureView.getSurfaceTexture();
        if(texture == null) return;

        texture.setDefaultBufferSize(previewsize.getWidth(), previewsize.getHeight());

        Surface surface = new Surface(texture);
        try {
            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (Exception e) { }

        previewBuilder.addTarget(surface);

        //------------------------------------------

        ImageReader reader = ImageReader.newInstance(previewsize.getWidth(), previewsize.getHeight(), ImageFormat.YUV_420_888,1);
        List<Surface> outputSurfaces = new ArrayList<>(2);
        outputSurfaces.add(reader.getSurface());
        outputSurfaces.add(surface);

        previewBuilder.addTarget(reader.getSurface());
        previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        int rotation =getWindowManager().getDefaultDisplay().getRotation();
        previewBuilder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATIONS.get(rotation));

        HandlerThread handlerThread=new HandlerThread("takepicture");
        handlerThread.start();
        final Handler handler=new Handler(handlerThread.getLooper());

        reader.setOnImageAvailableListener(imageAvailableListener, handler);

        //------------------------------------------

        HandlerThread handlerThread2 = new HandlerThread("2");
        handlerThread2.start();
        final Handler handler2 = new Handler(handlerThread2.getLooper());

        try {
            cameraDevice.createCaptureSession(outputSurfaces , new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NotNull CameraCaptureSession session) {
                    previewSession=session;

                    previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                    try {
                        session.capture(previewBuilder.build(), null, handler);

                        previewSession.setRepeatingRequest(previewBuilder.build(), null, handler2);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(@NotNull CameraCaptureSession session) {
                }
            },null);

        } catch (Exception e) {
            System.out.println(e.toString());
        }

    }

}
