package ru.example.macrofocusstacking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.DngCreator;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Pair;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.example.macrofocusstacking.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private MainActivity mainActivity;

    private SurfaceTexture surfaceTexture;
    private Surface previewSurface;
    private boolean surfaceTextureAvailable = false;
    private List<Surface> surfaceList = new ArrayList<>();

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder previewRequestBuilder;


    private ImageReader imageReaderJpeg;
    private ImageReader imageReaderRawSensor;
    private CaptureResult captureResult;

    private List<Pair<byte[], Integer>> jpgImages = new ArrayList<>();
    private List<Pair<byte[], Integer>> rawImages = new ArrayList<>();
    private List<Pair<Image, Integer>> rawImages2 = new ArrayList<>();
    private int currentJpgImgNumber = 0;
    private int currentRawImgNumber = 0;
    private int currentImgNumber = 0;

    private float focusValue = 25.0f;
    private float minFocusValue = 10.0f;
    private float maxFocusValue = 25.0f;
    private int frameCount = 50;
    private float focusValueStep = (maxFocusValue - minFocusValue) / (frameCount - 1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_MacroFocusStacking);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        super.onCreate(savedInstanceState);
        setContentView(binding.getRoot());

        mainActivity = this;

        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            continueLoadApp();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CAMERA)) {
                requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 0);
            } else {
                Toast.makeText(this, "Предоставьте разрешение на доступ к камере", Toast.LENGTH_SHORT).show();
                try {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                    startActivityForResult(intent, 0);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    startActivityForResult(intent, 0);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0) {
            checkAndRequestPermissions();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 0) {
            checkAndRequestPermissions();
        }
    }


    private void continueLoadApp() {
        imageReaderJpeg = ImageReader.newInstance(2592, 1944, ImageFormat.JPEG, 50);
        imageReaderJpeg.setOnImageAvailableListener(new ImageAvailableListener(this, ImageFormat.JPEG), null);

        imageReaderRawSensor = ImageReader.newInstance(2592, 1944, ImageFormat.RAW_SENSOR, 50);
        imageReaderRawSensor.setOnImageAvailableListener(new ImageAvailableListener(this, ImageFormat.RAW_SENSOR), null);

//        surfaceList = Arrays.asList(previewSurface, imageReaderJpeg.getSurface(), imageReaderRawSensor.getSurface());
        surfaceList = Arrays.asList(previewSurface, imageReaderJpeg.getSurface());
//
//        takePhotoButton = findViewById(R.id.takePhotoButton);
//        takePhotoButton.setOnClickListener(view -> {
//            takePhotoButton.setEnabled(false);
//            settingsButton.setEnabled(false);
//            try {
//                cameraCaptureSession.stopRepeating();
//                takePhoto();
//            } catch (CameraAccessException e) {
//                e.printStackTrace();
//            }
//        });

//        settingsButton = findViewById(R.id.settingsSave);
//        settingsButton.setOnClickListener(view -> {
//            findViewById(R.id.linearLayout).setVisibility(View.GONE);
//            findViewById(R.id.scrollView).setVisibility(View.VISIBLE);
//        });

//        sectorView = findViewById(R.id.sectorView);
//        progressBar = findViewById(R.id.progressBar);

//        EditText editTextFrameCount = findViewById(R.id.editTextFrameCount);
//        editTextFrameCount.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//                String frameCountText = editTextFrameCount.getText().toString();
//                if (!TextUtils.isEmpty(frameCountText)) {
//                    int newFrameCount = -1;
//                    try {
//                        newFrameCount = Integer.parseInt(frameCountText);
//                        if (newFrameCount >= 2 && newFrameCount <= 300) {
//                            editTextFrameCount.setBackgroundColor(Color.TRANSPARENT);
//                            frameCount = newFrameCount;
//                            focusValueStep = (maxFocusValue - minFocusValue) / (frameCount - 1);
//
//                            SharedPreferences.Editor editor = getSharedPreferences("MacroFocusStacking", MODE_PRIVATE).edit();
//                            editor.putInt("frameCount", frameCount);
//                            editor.apply();
//                        } else {
//                            editTextFrameCount.setBackgroundColor(Color.RED);
//                        }
//                    } catch (NumberFormatException e) {
//                        editTextFrameCount.setBackgroundColor(Color.RED);
//                    }
//                } else {
//                    editTextFrameCount.setBackgroundColor(Color.RED);
//                }
//            }
//        });


//        SharedPreferences sharedPreferences = getSharedPreferences("MacroFocusStacking", MODE_PRIVATE);
//        frameCount = sharedPreferences.getInt("frameCount", 50);
//        focusValueStep = (maxFocusValue - minFocusValue) / (frameCount - 1);
//        editTextFrameCount.setText(String.valueOf(frameCount));

//        RadioGroup radioGroup = findViewById(R.id.radioGroup);
//        String imagesFormat = sharedPreferences.getString("imagesFormat", "RawJpg");
//        switch (imagesFormat) {
//            case "Jpg":
//                radioGroup.check(R.id.radioButtonJpg);
//                surfaceList = Arrays.asList(previewSurface, imageReaderJpeg.getSurface());
//                break;

//            case "Raw":
//                radioGroup.check(R.id.radioButtonRaw);
//                surfaceList = Arrays.asList(previewSurface, imageReaderRawSensor.getSurface());
//                break;

//            case "RawJpg":
//                radioGroup.check(R.id.radioButtonRawJpg);
//                surfaceList = Arrays.asList(previewSurface, imageReaderJpeg.getSurface(), imageReaderRawSensor.getSurface());
//                break;
//        }

        binding.buttonTakePhoto.setOnClickListener(view -> {
            binding.buttonSettings.setEnabled(false);
            binding.buttonTakePhoto.setEnabled(false);
            takePhoto();
        });

        binding.buttonSettings.setOnClickListener(view -> {
            binding.textViewInfo.setVisibility(View.GONE);
            binding.constraintLayoutSectorView.setVisibility(View.GONE);
            binding.constraintLayoutSettings.setVisibility(View.VISIBLE);
        });

        binding.buttonSettingsBack.setOnClickListener(view -> {
            binding.textViewInfo.setVisibility(View.VISIBLE);
            binding.constraintLayoutSectorView.setVisibility(View.VISIBLE);
            binding.constraintLayoutSettings.setVisibility(View.GONE);
        });

        binding.buttonFocusSettings.setOnClickListener(view -> {
            binding.buttonFocusSettings.setStrokeColor(ColorStateList.valueOf(Color.WHITE));
            binding.buttonExpSettings.setStrokeColor(ColorStateList.valueOf(Color.BLACK));
            binding.buttonImageSettings.setStrokeColor(ColorStateList.valueOf(Color.BLACK));

            binding.constraintLayoutFocusSettings.setVisibility(View.VISIBLE);
            binding.constraintLayoutExpSettings.setVisibility(View.GONE);
            binding.constraintLayoutImageSettings.setVisibility(View.GONE);
        });

        binding.buttonExpSettings.setOnClickListener(view -> {
            binding.buttonFocusSettings.setStrokeColor(ColorStateList.valueOf(Color.BLACK));
            binding.buttonExpSettings.setStrokeColor(ColorStateList.valueOf(Color.WHITE));
            binding.buttonImageSettings.setStrokeColor(ColorStateList.valueOf(Color.BLACK));

            binding.constraintLayoutFocusSettings.setVisibility(View.GONE);
            binding.constraintLayoutExpSettings.setVisibility(View.VISIBLE);
            binding.constraintLayoutImageSettings.setVisibility(View.GONE);
        });

        binding.buttonImageSettings.setOnClickListener(view -> {
            binding.buttonFocusSettings.setStrokeColor(ColorStateList.valueOf(Color.BLACK));
            binding.buttonExpSettings.setStrokeColor(ColorStateList.valueOf(Color.BLACK));
            binding.buttonImageSettings.setStrokeColor(ColorStateList.valueOf(Color.WHITE));

            binding.constraintLayoutFocusSettings.setVisibility(View.GONE);
            binding.constraintLayoutExpSettings.setVisibility(View.GONE);
            binding.constraintLayoutImageSettings.setVisibility(View.VISIBLE);
        });

        binding.textureView.setSurfaceTextureListener(new TextureViewSurfaceTextureListener(this));
    }

    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        private MainActivity mainActivity;
        private final int imageFormatType;

        public ImageAvailableListener(MainActivity mainActivity, int imageFormatType) {
            this.mainActivity = mainActivity;
            this.imageFormatType = imageFormatType;
        }

        @Override
        public void onImageAvailable(ImageReader reader) {
            mainActivity.currentImgNumber += 1;

            if (mainActivity.currentImgNumber == mainActivity.frameCount * (mainActivity.surfaceList.size() - 1)) {
                mainActivity.currentImgNumber = 0;
                binding.sectorView.setSector(0.0f);
                binding.sectorView.invisibleSector();
                binding.progressBar.setVisibility(View.VISIBLE);
                mainActivity.startPreview();
            } else {
                float sectorValue = 360.0f * mainActivity.currentImgNumber / (mainActivity.frameCount * (mainActivity.surfaceList.size() - 1));
                binding.sectorView.setSector(sectorValue);
            }

            switch (imageFormatType) {
                case ImageFormat.JPEG:
                    AsyncTask.execute(() -> {
                        Image image = reader.acquireNextImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytesBuffer = new byte[buffer.capacity()];
                        buffer.get(bytesBuffer);
                        image.close();

                        mainActivity.currentJpgImgNumber += 1;
                        mainActivity.jpgImages.add(new Pair<>(bytesBuffer, mainActivity.currentJpgImgNumber));

                        if (mainActivity.currentJpgImgNumber == mainActivity.frameCount) {
                            mainActivity.currentJpgImgNumber = 0;
                            saveImages(mainActivity.jpgImages, "jpg", mainActivity);
                            mainActivity.jpgImages.clear();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mainActivity.focusValue = mainActivity.maxFocusValue;
                                    mainActivity.startPreview();
                                    binding.progressBar.setVisibility(View.GONE);
                                    binding.buttonTakePhoto.setEnabled(true);
                                    binding.buttonSettings.setEnabled(true);
                                }
                            });
                        }


                    });
                    break;
            }
        }

        public void saveImages(List<Pair<byte[], Integer>> images, String ext, AppCompatActivity activity) {
            String timeStamp = String.valueOf(System.currentTimeMillis());

            File picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File appDirectory = new File(picturesDirectory, "MacroFocusStacking");
            File subDirectory = new File(appDirectory, timeStamp + "_" + ext);
            if (!subDirectory.exists()) {
                subDirectory.mkdirs();
            }

            if (!appDirectory.exists()) {
                appDirectory.mkdirs();
            }

            for (Pair<byte[], Integer> img : images) {
                try {
                    File file = new File(subDirectory, "Macro_" + String.format("%03d", img.second) + "_" + timeStamp + "." + ext);

                    FileOutputStream output = new FileOutputStream(file);
                    output.write(img.first);
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            MediaScannerConnection.scanFile(activity, new String[]{appDirectory.getAbsolutePath()}, null, null);
            MediaScannerConnection.scanFile(activity, new String[]{subDirectory.getAbsolutePath()}, null, null);
        }
    }


    private class TextureViewSurfaceTextureListener implements TextureView.SurfaceTextureListener {
        private final MainActivity mainActivity;

        public TextureViewSurfaceTextureListener(MainActivity mainActivity) {
            this.mainActivity = mainActivity;
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            mainActivity.surfaceTexture = surfaceTexture;
            mainActivity.surfaceTextureAvailable = true;

            mainActivity.openCamera();
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    }

    private void openCamera() {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            cameraManager.openCamera("3", new CameraDeviceStateCallback(this), null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private class CameraDeviceStateCallback extends CameraDevice.StateCallback {
        private final MainActivity mainActivity;

        public CameraDeviceStateCallback(MainActivity mainActivity) {
            this.mainActivity = mainActivity;
        }

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mainActivity.cameraDevice = cameraDevice;
            mainActivity.createPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mainActivity.cameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mainActivity.cameraDevice = null;
        }
    }

    private void createPreviewSession() {
        previewSurface = new Surface(binding.textureView.getSurfaceTexture());

        try {
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            if (surfaceList.size() == 0) {
                surfaceList.add(previewSurface);
            } else {
                surfaceList.set(0, previewSurface);
            }
            cameraDevice.createCaptureSession(surfaceList, new CameraCaptureSessionStateCallback(this), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class CameraCaptureSessionStateCallback extends CameraCaptureSession.StateCallback {
        private MainActivity mainActivity;

        public CameraCaptureSessionStateCallback(MainActivity mainActivity) {
            this.mainActivity = mainActivity;
        }

        @Override
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
            mainActivity.cameraCaptureSession = cameraCaptureSession;
            mainActivity.startPreview();
//
//            if (mainActivity.jpgImages.isEmpty() && mainActivity.rawImages.isEmpty()) {
//                mainActivity.takePhotoButton.setEnabled(true);
//            }

            binding.buttonTakePhoto.setEnabled(true);
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
        }
    }

    private void startPreview() {
        if (previewRequestBuilder == null || cameraCaptureSession == null)
            return;

        try {
            previewRequestBuilder.addTarget(previewSurface);
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_LOCK, false);
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            previewRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, focusValue);

            cameraCaptureSession.setRepeatingRequest(previewRequestBuilder.build(), new CameraCaptureSessionCaptureCallback(this), null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private class CameraCaptureSessionCaptureCallback extends CameraCaptureSession.CaptureCallback {
        private final MainActivity mainActivity;

        public CameraCaptureSessionCaptureCallback(MainActivity mainActivity) {
            this.mainActivity = mainActivity;
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            mainActivity.captureResult = result;
        }
    }


    private void takePhoto() {
        if (cameraCaptureSession == null || cameraDevice == null || previewRequestBuilder == null)
            return;

        focusValue = maxFocusValue;
        focusValueStep = (maxFocusValue - minFocusValue) / (frameCount - 1);
        List<CaptureRequest> captureRequests = new ArrayList<>();
        for (int i = 0; i < frameCount; i++) {
            try {
                CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG);
                for (Surface surface : surfaceList) {
                    captureBuilder.addTarget(surface);
                }
                captureBuilder.set(CaptureRequest.CONTROL_AE_LOCK, true);
                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
                captureBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, focusValue);
                captureRequests.add(captureBuilder.build());
                focusValue -= focusValueStep;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            cameraCaptureSession.captureBurst(captureRequests, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        binding.sectorView.setSector(0.0f);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (surfaceTextureAvailable) {
            openCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCamera();
    }

    private void stopCamera() {
        if (cameraCaptureSession != null) {
            try {
                cameraCaptureSession.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            cameraCaptureSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }
}