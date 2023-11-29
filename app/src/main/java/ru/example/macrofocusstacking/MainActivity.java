package ru.example.macrofocusstacking;

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
import android.util.SparseArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.example.macrofocusstacking.Database.MacroFocusStackingDao;
import ru.example.macrofocusstacking.Database.MacroFocusStackingDatabase;
import ru.example.macrofocusstacking.Database.Model.RawImage;
import ru.example.macrofocusstacking.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    private MacroFocusStackingDao macroFocusStackingDao;

    private Surface previewSurface;
    private boolean surfaceTextureAvailable = false;
    private List<Surface> surfaceList = new ArrayList<>();

    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder previewRequestBuilder;


    private ImageReader imageReaderJpeg;
    private ImageReader imageReaderRawSensor;
    private CaptureResult captureResult;

    private List<Pair<byte[], Integer>> jpgImages = new ArrayList<>();
    private List<Pair<byte[], Integer>> rawImages = new ArrayList<>();
    private int currentJpgImgNumber = 0;
    private int currentRawImgNumber = 0;
    private int currentImgNumber = 0;

    private float nearFocusValue = 25.0f;
    private float farFocusValue = 10.0f;
    private final float[] focusValueRange = {10.0f, 25.0f};
    private int frameCount = 100;

    private int curTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_MacroFocusStacking);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        super.onCreate(savedInstanceState);
        setContentView(binding.getRoot());

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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 0) {
            checkAndRequestPermissions();
        }
    }


    private void continueLoadApp() {
        macroFocusStackingDao = ((App) getApplicationContext()).getMacroFocusStackingDatabase().macroFocusStackingDao();
        AsyncTask.execute(() -> macroFocusStackingDao.deleteAllRawImage());

        imageReaderJpeg = ImageReader.newInstance(2592, 1944, ImageFormat.JPEG, 50);
        imageReaderJpeg.setOnImageAvailableListener(new ImageAvailableListener(this, ImageFormat.JPEG), null);

        imageReaderRawSensor = ImageReader.newInstance(2592, 1944, ImageFormat.RAW_SENSOR, 50);
        imageReaderRawSensor.setOnImageAvailableListener(new ImageAvailableListener(this, ImageFormat.RAW_SENSOR), null);

//        RadioGroup radioGroup = findViewById(R.id.radioGroup);
//        String imagesFormat = sharedPreferences.getString("imagesFormat", "RawJpg");
//        switch (imagesFormat) {
//            case "Jpg":
//                radioGroup.check(R.id.radioButtonJpg);
//                surfaceList = Arrays.asList(previewSurface, imageReaderJpeg.getSurface());
//                break;
//
//            case "Raw":
//                radioGroup.check(R.id.radioButtonRaw);
//                surfaceList = Arrays.asList(previewSurface, imageReaderRawSensor.getSurface());
//                break;
//
//            case "RawJpg":
//                radioGroup.check(R.id.radioButtonRawJpg);
//                surfaceList = Arrays.asList(previewSurface, imageReaderJpeg.getSurface(), imageReaderRawSensor.getSurface());
//                break;
//        }

//        SharedPreferences sharedPreferences = getSharedPreferences("MacroFocusStacking", MODE_PRIVATE);
//        frameCount = sharedPreferences.getInt("frameCount", 50);
//        focusValueStep = (maxFocusValue - minFocusValue) / (frameCount - 1);
//        editTextFrameCount.setText(String.valueOf(frameCount));radioButtonJpg

        surfaceList = Arrays.asList(previewSurface, imageReaderJpeg.getSurface());
        binding.textViewInfoFormat.setText("Снимков: " + frameCount + " Формат: Jpg");

        binding.radioButtonJpg.setOnClickListener(view -> {
            surfaceList = Arrays.asList(previewSurface, imageReaderJpeg.getSurface());
            createPreviewSession();
            binding.textViewInfoFormat.setText("Снимков: " + frameCount + " Формат: Jpg");
        });
        binding.radioButtonRaw.setOnClickListener(view -> {
            surfaceList = Arrays.asList(previewSurface, imageReaderRawSensor.getSurface());
            createPreviewSession();
            binding.textViewInfoFormat.setText("Снимков: " + frameCount + " Формат: Raw");
        });
        binding.radioButtonRawJpg.setOnClickListener(view -> {
            surfaceList = Arrays.asList(previewSurface, imageReaderJpeg.getSurface(), imageReaderRawSensor.getSurface());
            createPreviewSession();
            binding.textViewInfoFormat.setText("Снимков: " + frameCount + " Формат: Raw+Jpg");
        });
//        binding.radioGroupImageFormat.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(RadioGroup radioGroup, int i) {
//
//            }
//        });


        binding.buttonTakePhoto.setOnClickListener(view -> {
            binding.buttonSettings.setEnabled(false);
            binding.buttonTakePhoto.setEnabled(false);
            takePhoto();
        });

        binding.buttonSettings.setOnClickListener(view -> {
            binding.textViewInfoExp.setVisibility(View.GONE);
            binding.textViewInfoFocus.setVisibility(View.GONE);
            binding.textViewInfoFormat.setVisibility(View.GONE);
            binding.constraintLayoutSectorView.setVisibility(View.GONE);
            binding.constraintLayoutSettings.setVisibility(View.VISIBLE);

            changeTab(curTab);
        });

        binding.buttonSettingsBack.setOnClickListener(view -> {
            startPreview(nearFocusValue);

            binding.textViewInfoFormat.setVisibility(View.VISIBLE);
            binding.textViewInfoFocus.setVisibility(View.VISIBLE);
            binding.textViewInfoExp.setVisibility(View.VISIBLE);
            binding.constraintLayoutSectorView.setVisibility(View.VISIBLE);
            binding.constraintLayoutSettings.setVisibility(View.GONE);
        });


        binding.buttonNearFocusSettings.setOnClickListener(view -> {
            curTab = 0;
            changeTab(curTab);
        });

        binding.seekBarNearFocus.setProgress(999 - (int) ((nearFocusValue - focusValueRange[0]) / (focusValueRange[1] - focusValueRange[0]) * 999.0));
        binding.textViewNearFocus.setText("Ближняя точка фокусировки " + String.format("%1$,.2f", nearFocusValue));
        binding.textViewInfoFocus.setText("Фокусировка с " + String.format("%1$,.2f", nearFocusValue) + " до " + String.format("%1$,.2f", farFocusValue));
        binding.seekBarNearFocus.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                nearFocusValue = (float) (focusValueRange[0] + (999 - seekBar.getProgress()) / 999.0 * (focusValueRange[1] - focusValueRange[0]));
                binding.textViewNearFocus.setText("Ближняя точка фокусировки " + String.format("%1$,.2f", nearFocusValue));
                binding.textViewInfoFocus.setText("Фокусировка с " + String.format("%1$,.2f", nearFocusValue) + " до " + String.format("%1$,.2f", farFocusValue));
                startPreview(nearFocusValue);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        binding.buttonFarFocusSettings.setOnClickListener(view -> {
            curTab = 1;
            changeTab(curTab);
        });

        binding.seekBarFarFocus.setProgress(999 - (int) ((farFocusValue - focusValueRange[0]) / (focusValueRange[1] - focusValueRange[0]) * 999.0));
        binding.textViewFarFocus.setText("Дальняя точка фокусировки " + String.format("%1$,.2f", farFocusValue));
        binding.seekBarFarFocus.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                farFocusValue = (float) (focusValueRange[0] + (999 - seekBar.getProgress()) / 999.0 * (focusValueRange[1] - focusValueRange[0]));
                binding.textViewFarFocus.setText("Дальняя точка фокусировки " + String.format("%1$,.2f", farFocusValue));
                binding.textViewInfoFocus.setText("Фокусировка с " + String.format("%1$,.2f", nearFocusValue) + " до " + String.format("%1$,.2f", farFocusValue));
                startPreview(farFocusValue);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        binding.buttonExpSettings.setOnClickListener(view -> {
            curTab = 2;
            changeTab(curTab);
        });

        binding.buttonImageSettings.setOnClickListener(view -> {
            curTab = 3;
            changeTab(curTab);
        });

        binding.textureView.setSurfaceTextureListener(new TextureViewSurfaceTextureListener());
    }

    private void changeTab(int tabNum) {
        switch (tabNum) {
            case 0:
            case 2:
            case 3:
                startPreview(nearFocusValue);
                break;
            case 1:
                startPreview(farFocusValue);
                break;
        }

        binding.buttonNearFocusSettings.setStrokeColor(tabNum == 0 ? ColorStateList.valueOf(Color.WHITE) : ColorStateList.valueOf(Color.BLACK));
        binding.buttonFarFocusSettings.setStrokeColor(tabNum == 1 ? ColorStateList.valueOf(Color.WHITE) : ColorStateList.valueOf(Color.BLACK));
        binding.buttonExpSettings.setStrokeColor(tabNum == 2 ? ColorStateList.valueOf(Color.WHITE) : ColorStateList.valueOf(Color.BLACK));
        binding.buttonImageSettings.setStrokeColor(tabNum == 3 ? ColorStateList.valueOf(Color.WHITE) : ColorStateList.valueOf(Color.BLACK));

        binding.constraintLayoutNearFocusSettings.setVisibility(tabNum == 0 ? View.VISIBLE : View.GONE);
        binding.constraintLayoutFarFocusSettings.setVisibility(tabNum == 1 ? View.VISIBLE : View.GONE);
        binding.constraintLayoutExpSettings.setVisibility(tabNum == 2 ? View.VISIBLE : View.GONE);
        binding.constraintLayoutImageSettings.setVisibility(tabNum == 3 ? View.VISIBLE : View.GONE);
    }

    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        private final MainActivity mainActivity;
        private final int imageFormatType;

        public ImageAvailableListener(MainActivity mainActivity, int imageFormatType) {
            this.mainActivity = mainActivity;
            this.imageFormatType = imageFormatType;
        }

        @Override
        public void onImageAvailable(ImageReader reader) {
            currentImgNumber += 1;

            if (currentImgNumber == frameCount * (surfaceList.size() - 1)) {
                currentImgNumber = 0;
                binding.sectorView.setSector(0.0f);
                binding.sectorView.invisibleSector();
                binding.progressBar.setVisibility(View.VISIBLE);
                startPreview(nearFocusValue);

                AsyncTask.execute(() -> {
                    currentJpgImgNumber = 0;
                    saveImages(jpgImages, mainActivity);
                    jpgImages.clear();

                    currentRawImgNumber = 0;
                    rawImages.clear();
                    AsyncTask.execute(() -> macroFocusStackingDao.deleteAllRawImage());

                    runOnUiThread(() -> {
                        startPreview(nearFocusValue);
                        binding.progressBar.setVisibility(View.GONE);
                        binding.buttonTakePhoto.setEnabled(true);
                        binding.buttonSettings.setEnabled(true);
                    });
                });

            } else {
                float sectorValue = 360.0f * currentImgNumber / (frameCount * (surfaceList.size() - 1));
                binding.sectorView.setSector(sectorValue);
            }


            switch (imageFormatType) {
                case ImageFormat.JPEG:
                    currentJpgImgNumber += 1;

                    Image image = reader.acquireNextImage();
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytesBuffer = new byte[buffer.capacity()];
                    buffer.get(bytesBuffer);
                    image.close();

                    jpgImages.add(new Pair<>(bytesBuffer, currentJpgImgNumber));

                    break;

                case ImageFormat.RAW_SENSOR:
                    currentRawImgNumber += 1;
//
//                    Image imageRaw = reader.acquireNextImage();
//                    ByteBuffer bufferRaw = imageRaw.getPlanes()[0].getBuffer();
//                    byte[] bytesBufferRaw = new byte[bufferRaw.capacity()];
//                    bufferRaw.get(bytesBufferRaw);
//                    imageRaw.close();
//                    rawImages.add(new Pair<>(bytesBufferRaw, currentRawImgNumber));

                    AsyncTask.execute(() -> {
                        Image imageRaw = reader.acquireNextImage();
                        ByteBuffer bufferRaw = imageRaw.getPlanes()[0].getBuffer();
                        byte[] bytesBufferRaw = new byte[bufferRaw.capacity()];
                        bufferRaw.get(bytesBufferRaw);
                        imageRaw.close();

                        macroFocusStackingDao.insertRawImage(new RawImage(macroFocusStackingDao.getNextRawImageId(), "" + currentRawImgNumber, bytesBufferRaw));

//                            rawImages.add(new Pair<>(bytesBufferRaw, currentRawImgNumber));


                    });

                    break;
            }
        }

        public void saveImages(List<Pair<byte[], Integer>> images, AppCompatActivity activity) {
            String timeStamp = String.valueOf(System.currentTimeMillis());

            File picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File appDirectory = new File(picturesDirectory, "MacroFocusStacking");
            File subDirectory = new File(appDirectory, timeStamp + "_jpg");
            if (!subDirectory.exists()) {
                subDirectory.mkdirs();
            }

            if (!appDirectory.exists()) {
                appDirectory.mkdirs();
            }

            for (Pair<byte[], Integer> image : images) {
                try {
                    File file = new File(subDirectory, "Macro_" + String.format("%03d", image.second) + "_" + timeStamp + ".jpg");

                    FileOutputStream output = new FileOutputStream(file);
                    output.write(image.first);
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            MediaScannerConnection.scanFile(activity, new String[]{appDirectory.getAbsolutePath()}, null, null);
            MediaScannerConnection.scanFile(activity, new String[]{subDirectory.getAbsolutePath()}, null, null);
        }

//        public  void saveRawImages(List<Pair<Image, Integer>> images, AppCompatActivity activity) {
//            String timeStamp = String.valueOf(System.currentTimeMillis());
//
//            File picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
//            File appDirectory = new File(picturesDirectory, "MacroFocusStacking");
//            File subDirectory = new File(appDirectory, timeStamp + "_dng");
//            if (!subDirectory.exists()) {
//                subDirectory.mkdirs();
//            }
//
//            if (!appDirectory.exists()) {
//                appDirectory.mkdirs();
//            }
//
//            Size imageResolution = new Size(2592, 1944);
//            // Assuming you have a DngCreator instance initialized elsewhere
//            DngCreator dngCreator = new DngCreator(/*pass necessary parameters*/);
//
//            for (Pair<Image, Integer> img : images) {
//                try {
//                    File file = new File(subDirectory, "Macro_" + String.format("%03d", img.second) + "_" + timeStamp + ".dng");
//
//                    FileOutputStream output = new FileOutputStream(file);
//                    dngCreator.writeImage(output, img.first);
//                    output.close();
//
//                    img.first.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            MediaScannerConnection.scanFile(activity, new String[]{appDirectory.getAbsolutePath()}, null, null);
//            MediaScannerConnection.scanFile(activity, new String[]{subDirectory.getAbsolutePath()}, null, null);
//        }
    }


    private class TextureViewSurfaceTextureListener implements TextureView.SurfaceTextureListener {
        public TextureViewSurfaceTextureListener() {
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            surfaceTextureAvailable = true;
            openCamera();
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        }
    }

    private void openCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

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
            createPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            mainActivity.cameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            mainActivity.cameraDevice = null;
        }
    }

    private void createPreviewSession() {
        previewSurface = new Surface(binding.textureView.getSurfaceTexture());

        try {
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(previewSurface);

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
        private final MainActivity mainActivity;

        public CameraCaptureSessionStateCallback(MainActivity mainActivity) {
            this.mainActivity = mainActivity;
        }

        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            mainActivity.cameraCaptureSession = cameraCaptureSession;
            startPreview(nearFocusValue);
//
//            if (mainActivity.jpgImages.isEmpty() && mainActivity.rawImages.isEmpty()) {
//                mainActivity.takePhotoButton.setEnabled(true);
//            }

            binding.buttonTakePhoto.setEnabled(true);
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
        }
    }

    private void startPreview(float focusValue) {
        if (previewRequestBuilder == null || cameraCaptureSession == null) return;

        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_LOCK, false);
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            previewRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, focusValue);

            cameraCaptureSession.setRepeatingRequest(previewRequestBuilder.build(), new CameraCaptureSessionCaptureCallback(), null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private class CameraCaptureSessionCaptureCallback extends CameraCaptureSession.CaptureCallback {
        public CameraCaptureSessionCaptureCallback() {
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, TotalCaptureResult result) {
            captureResult = result;

            binding.textViewInfoExp.setText("iso: " + result.get(TotalCaptureResult.SENSOR_SENSITIVITY) + " exp: 1/" + (int) Math.round(1D / (result.get(TotalCaptureResult.SENSOR_EXPOSURE_TIME) / 1000000000.0)));
        }
    }


    private void takePhoto() {
        if (cameraCaptureSession == null || cameraDevice == null || previewRequestBuilder == null)
            return;

        float focusValue = nearFocusValue;
        float focusValueStep = (nearFocusValue - farFocusValue) / (frameCount - 1);
        List<CaptureRequest> captureRequests = new ArrayList<>();
        for (int i = 0; i < frameCount; i++) {
            try {
                CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
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