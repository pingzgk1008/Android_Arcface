package com.zgk.arcfacedemo.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.arcsoft.face.FaceEngine;
import com.zgk.arcfacedemo.R;
import com.zgk.arcfacedemo.common.Constants;

import static com.arcsoft.face.ErrorInfo.MERR_ASF_ALREADY_ACTIVATED;
import static com.arcsoft.face.ErrorInfo.MOK;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private FaceEngine mFaceEngine;
    private Context mContext = MainActivity.this;

    //请求权限数组
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermission();

    }

    private void requestPermission() {
        boolean allGranted = true;
        for (String needPermission : NEEDED_PERMISSIONS) {
            allGranted &= ContextCompat.checkSelfPermission(this, needPermission) == PackageManager.PERMISSION_GRANTED;
        }
        if (!allGranted) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, 2);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i(TAG, "onRequestPermissionsResult size : " + permissions.length + grantResults.length);
        if (requestCode == 2 && permissions.length == 4) {
            Log.i(TAG, "权限请求完成！！！" + permissions.length + grantResults.length);
        }
    }


    /**
     * 激活引擎
     *
     * @param view
     */
    public void activeEngine(final View view) {
        mFaceEngine = new FaceEngine();
        int activeOnlineCode = mFaceEngine.activeOnline(mContext, Constants.APP_ID, Constants.SDK_KEY);
        if (activeOnlineCode == MOK) {
            Toast.makeText(mContext, "SDK激活成功", Toast.LENGTH_SHORT).show();
        } else if (activeOnlineCode == MERR_ASF_ALREADY_ACTIVATED) {
            Toast.makeText(mContext, "SDK已激活", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(mContext, "SDK激活失败", Toast.LENGTH_SHORT).show();
        }
    }

    /*
     * 人脸检测
     * */
    public void jianCe(final View view) {
        startActivity(new Intent(mContext, JianCeActivity.class));
    }

    /**
     * 打开相机，显示年龄性别
     *
     * @param view
     */
    public void jumpToPreviewActivity(View view) {
        startActivity(new Intent(mContext, FaceAttrPreviewActivity.class));
    }

    /*
     * 人脸注册
     * */
    public void zhuCe(final View view) {
        startActivity(new Intent(mContext, ZhuCeActivity.class));
    }

    /*
     * 人脸识别
     * */
    public void shiBie(final View view) {
        startActivity(new Intent(mContext, ShiBieActivity.class));
    }

}

