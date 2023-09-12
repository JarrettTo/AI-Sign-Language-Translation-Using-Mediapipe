package com.example.sign;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.sign.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private ConstraintLayout container;
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private LifecycleRegistry lifecycleRegistry = new LifecycleRegistry(this);
    private android.hardware.camera2.CameraManager CameraManager;
    private android.hardware.camera2.CameraCaptureSession CameraCaptureSession;
    private ActivityResultLauncher<String[]> activityResultLauncher;
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        container = findViewById(R.id.cameraContainer);
        previewView = new PreviewView(this);


        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                new ActivityResultCallback<Map<String, Boolean>>() {
                    @Override
                    public void onActivityResult(Map<String, Boolean> permissions) {
                        // Handle Permission granted/rejected
                        boolean permissionGranted = true;
                        for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
                            if (Arrays.asList(REQUIRED_PERMISSIONS).contains(entry.getKey()) && !entry.getValue()) {
                                permissionGranted = false;
                                break;
                            }
                        }
                        if (!permissionGranted) {
                            Toast.makeText(getApplicationContext(),
                                    "Permission request denied",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            startCamera();
                        }
                    }
                }
        );
        getPermissions();
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
        setContentView(binding.getRoot());


    }
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void startCamera() {
        System.out.println("FUCK IS GOING ON");
        runOnUiThread(() -> {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            cameraProviderFuture = ProcessCameraProvider.getInstance(MainActivity.this);
            cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    Preview preview = new Preview.Builder().build();
                    CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                    preview.setSurfaceProvider(previewView.getSurfaceProvider());

                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle(MainActivity.this, cameraSelector, preview);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }, executor);
        });
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview);
    }
    private void getPermissions(){
        ArrayList<String> permList = new ArrayList<String>();
        if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            permList.add(Manifest.permission.CAMERA);
        }
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            permList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            permList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if(permList.size() > 0){
            requestPermissions(permList.toArray(new String[0]), 101);
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                getPermissions();
                Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

}

