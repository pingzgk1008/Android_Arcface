package com.zgk.arcfacedemo.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Point;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import com.arcsoft.face.AgeInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.Face3DAngle;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.GenderInfo;
import com.arcsoft.face.LivenessInfo;
import com.arcsoft.face.enums.DetectMode;
import com.zgk.arcfacedemo.R;
import com.zgk.arcfacedemo.model.DrawInfo;
import com.zgk.arcfacedemo.util.ConfigUtil;
import com.zgk.arcfacedemo.util.DrawHelper;
import com.zgk.arcfacedemo.util.camera.CameraHelper;
import com.zgk.arcfacedemo.util.camera.CameraListener;
import com.zgk.arcfacedemo.util.face.RecognizeColor;
import com.zgk.arcfacedemo.widget.FaceRectView;

import java.util.ArrayList;
import java.util.List;

public class ShiBieActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shi_bie);


    }


}
