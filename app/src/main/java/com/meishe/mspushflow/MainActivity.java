package com.meishe.mspushflow;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Camera;
import android.os.Bundle;
import android.widget.TextView;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.meishe.mspushflow.databinding.ActivityMainBinding;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("native-lib");
    }

    private ActivityMainBinding binding;
    private CameraWrapper mCameraWrapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

         mCameraWrapper = new CameraWrapper(this, Camera.CameraInfo.CAMERA_FACING_BACK, 640, 480);
        mCameraWrapper.setPreviewDisplay(binding.surfaceView.getHolder());

        requestPermission();
    }

    private void requestPermission() {
        XXPermissions.with(this).permission(Permission.CAMERA)
                .permission(Permission.RECORD_AUDIO)
                .permission(Permission.READ_EXTERNAL_STORAGE)
                .permission(Permission.WRITE_EXTERNAL_STORAGE)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(List<String> permissions, boolean all) {
                       if (all){
                           mCameraWrapper.switchCamera();
                       }
                    }
                    @Override
                    public void onDenied(List<String> permissions, boolean never) {

                    }
                });

    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}