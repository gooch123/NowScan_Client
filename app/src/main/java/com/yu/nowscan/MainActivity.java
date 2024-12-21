package com.yu.nowscan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.canhub.cropper.CropImageView;

import org.json.JSONObject;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private PreviewView previewView;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private ProgressBar progressBar;
    private CropImageView cropImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        progressBar = findViewById(R.id.progressBar);
        cropImageView = findViewById(R.id.cropImageView);
        progressBar.setVisibility(View.GONE);
        cropImageView.setVisibility(View.GONE);
        cameraExecutor = Executors.newSingleThreadExecutor();

        Button captureButton = findViewById(R.id.captureButton);
        captureButton.setOnClickListener(v -> capturePhoto());

        Button cropButton = findViewById(R.id.cropButton);
        cropButton.setVisibility(View.GONE);
        cropButton.setOnClickListener(v -> {
            Bitmap croppedImage = cropImageView.getCroppedImage();
            if (croppedImage != null) {
                processCroppedImage(croppedImage);
                cropImageView.setVisibility(View.GONE);
                cropButton.setVisibility(View.GONE);
                Button closeButton = findViewById(R.id.closeButton);
                closeButton.setVisibility(View.GONE);
            }
        });

        Button closeButton = findViewById(R.id.closeButton);
        if (closeButton != null) {
            closeButton.setVisibility(View.GONE);
            closeButton.setOnClickListener(v -> {
                cropImageView.setVisibility(View.GONE);
                cropButton.setVisibility(View.GONE);
                closeButton.setVisibility(View.GONE);
            });
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
        }

        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                cameraProvider.unbindAll();

                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageCapture
                );

            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraXApp", "카메라 초기화 중 오류 발생", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    private void capturePhoto() {
        if (imageCapture == null) return;

        File photoFile = new File(getExternalFilesDir(null), "captured_image.jpg");
        Uri photoUri = Uri.fromFile(photoFile);

        ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                options,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        cropImageView.setVisibility(View.VISIBLE);
                        cropImageView.setImageUriAsync(photoUri);

                        Button cropButton = findViewById(R.id.cropButton);
                        cropButton.setVisibility(View.VISIBLE);

                        Button closeButton = findViewById(R.id.closeButton);
                        closeButton.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        exception.printStackTrace();
                    }
                }
        );
    }

    private void processCroppedImage(Bitmap croppedImage) {
        try {
            InputImage image = InputImage.fromBitmap(croppedImage, 0);

            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String extractedText = visionText.getText();
                        Log.d("MLKit", "Extracted Text: " + extractedText);
                        sendToServer(extractedText);
                    })
                    .addOnFailureListener(e -> e.printStackTrace());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendToServer(String extractedText) {
        runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));

        new Thread(() -> {
            try {
                String urlString = "http://goochul.iptime.org:8000/gpt/chat?prompt=" +
                        Uri.encode(extractedText, "UTF-8");
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    Scanner scanner = new Scanner(connection.getInputStream());
                    StringBuilder response = new StringBuilder();
                    while (scanner.hasNextLine()) {
                        response.append(scanner.nextLine());
                    }
                    scanner.close();

                    String responseBody = response.toString();
                    Log.d("ServerResponse", "Response: " + responseBody);

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    String data = jsonResponse.optString("body", "");

                    Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                    intent.putExtra("data", data);
                    startActivity(intent);

                } else {
                    Log.e("ServerError", "Response Code: " + responseCode);
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                runOnUiThread(() -> progressBar.setVisibility(View.GONE));
            }
        }).start();
    }
}
