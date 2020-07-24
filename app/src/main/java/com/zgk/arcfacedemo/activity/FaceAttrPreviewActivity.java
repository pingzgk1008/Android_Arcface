package com.zgk.arcfacedemo.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

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


public class FaceAttrPreviewActivity extends AppCompatActivity implements ViewTreeObserver.OnGlobalLayoutListener {
    private static final String TAG = "FaceAttrPreviewActivity";
    private CameraHelper cameraHelper;//相机辅助类
    private DrawHelper drawHelper;//绘制人脸框帮助类
    private Camera.Size previewSize;
    //CAMERA_FACING_BACK：照相机的正面与屏幕的正面相对。
    private Integer rgbCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private FaceEngine faceEngine;
    private int afCode = -1;
    private int processMask = FaceEngine.ASF_AGE |
            FaceEngine.ASF_GENDER |
            FaceEngine.ASF_FACE3DANGLE |
            FaceEngine.ASF_LIVENESS;
    /**
     * 相机预览显示的控件，可为SurfaceView或TextureView
     */
    private View previewView;
    private FaceRectView faceRectView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_attr_preview);

        previewView = findViewById(R.id.texture_preview);
        faceRectView = findViewById(R.id.face_rect_view);
        //在布局结束后才做初始化操作
        previewView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        initEngine();
        initCamera();
        if (cameraHelper != null) {
            cameraHelper.start();
        }
    }

    /**
     * 初始化图片识别引擎（视频、拍照）
     * 调用FaceEngine的init方法初始化SDK，初始化成功后才能进一步使用SDK的功能。
     */
    private void initEngine() {
        faceEngine = new FaceEngine();
        afCode = faceEngine.init(this,
                DetectMode.ASF_DETECT_MODE_VIDEO,
                ConfigUtil.getFtOrient(this),
                16, 20,
                FaceEngine.ASF_FACE_RECOGNITION |
                        FaceEngine.ASF_FACE_DETECT |
                        FaceEngine.ASF_AGE |
                        FaceEngine.ASF_FACE3DANGLE |
                        FaceEngine.ASF_GENDER |
                        FaceEngine.ASF_LIVENESS);
        Log.i(TAG, "initEngine:  init: " + afCode);
        if (afCode != ErrorInfo.MOK) {
            Log.i(TAG, "引擎初始化失败！！！");
        } else {
            Log.i(TAG, "引擎初始化成功！！！");
        }
    }

    /*
     * 销毁引擎
     * */
    private void unInitEngine() {
        if (afCode == 0) {
            afCode = faceEngine.unInit();
            Log.i(TAG, "unInitEngine: " + afCode);
        }
    }

    @Override
    protected void onDestroy() {
        if (cameraHelper != null) {
            cameraHelper.release();
            cameraHelper = null;
        }
        unInitEngine();
        super.onDestroy();
    }

    private void initCamera() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        CameraListener cameraListener = new CameraListener() {
            @Override
            public void onCameraOpened(Camera camera, int cameraId, int displayOrientation, boolean isMirror) {
                Log.i(TAG, "onCameraOpened: " + cameraId + "  " + displayOrientation + " " + isMirror);
                previewSize = camera.getParameters().getPreviewSize();
                drawHelper = new DrawHelper(
                        previewSize.width,
                        previewSize.height,
                        previewView.getWidth(),
                        previewView.getHeight(),
                        displayOrientation, cameraId, isMirror,
                        false, false);
            }

            @Override
            public void onPreview(byte[] nv21, Camera camera) {
                if (faceRectView != null) {
                    faceRectView.clearFaceInfo();
                }

                List<FaceInfo> faceInfoList = new ArrayList<>();

                /**
                 * 开始人脸检测
                 */
                int code = faceEngine.detectFaces(nv21,
                        previewSize.width, previewSize.height,
                        FaceEngine.CP_PAF_NV21, faceInfoList);

                if (code == ErrorInfo.MOK && faceInfoList.size() > 0) {
                    code = faceEngine.process(
                            nv21, previewSize.width, previewSize.height,
                            FaceEngine.CP_PAF_NV21, faceInfoList, processMask);
                    if (code != ErrorInfo.MOK) {
                        return;
                    }
                } else {
                    Log.i(TAG, "processImage 获取年龄、性别等信息成功 : ");
                }

                List<AgeInfo> ageInfoList = new ArrayList<>();
                List<GenderInfo> genderInfoList = new ArrayList<>();
                List<Face3DAngle> face3DAngleList = new ArrayList<>();
                List<LivenessInfo> faceLivenessInfoList = new ArrayList<>();
                int ageCode = faceEngine.getAge(ageInfoList);
                int genderCode = faceEngine.getGender(genderInfoList);
                int face3DAngleCode = faceEngine.getFace3DAngle(face3DAngleList);
                int livenessCode = faceEngine.getLiveness(faceLivenessInfoList);

                // 有其中一个的错误码不为ErrorInfo.MOK，return
                if ((ageCode | genderCode | face3DAngleCode | livenessCode) != ErrorInfo.MOK) {
                    Log.i(TAG, "detectFaceInfo 获取部分信息失败: ageCode: " + ageCode + " genderCode:" + genderCode + " face3DAngleCode:" + face3DAngleCode + " livenessCode:" + livenessCode);
                    return;
                }

                if (faceRectView != null && drawHelper != null) {
                    List<DrawInfo> drawInfoList = new ArrayList<>();
                    for (int i = 0; i < faceInfoList.size(); i++) {
                        drawInfoList.add(new DrawInfo(
                                drawHelper.adjustRect(faceInfoList.get(i).getRect()),
                                genderInfoList.get(i).getGender(),
                                ageInfoList.get(i).getAge(),
                                faceLivenessInfoList.get(i).getLiveness(),
                                RecognizeColor.COLOR_UNKNOWN,
                                null));
                    }
                    drawHelper.draw(faceRectView, drawInfoList);
                }
            }

            @Override
            public void onCameraClosed() {
                Log.i(TAG, "onCameraClosed: ");
            }

            @Override
            public void onCameraError(Exception e) {
                Log.i(TAG, "onCameraError: " + e.getMessage());
            }

            @Override
            public void onCameraConfigurationChanged(int cameraID, int displayOrientation) {
                if (drawHelper != null) {
                    drawHelper.setCameraDisplayOrientation(displayOrientation);
                }
                Log.i(TAG, "onCameraConfigurationChanged: " + cameraID + "  " + displayOrientation);
            }
        };
        cameraHelper = new CameraHelper.Builder()
                .previewViewSize(new Point(previewView.getMeasuredWidth(), previewView.getMeasuredHeight()))
                .rotation(getWindowManager().getDefaultDisplay().getRotation())
                .specificCameraId(rgbCameraId != null ? rgbCameraId : Camera.CameraInfo.CAMERA_FACING_FRONT)
                .isMirror(false)
                .previewOn(previewView)
                .cameraListener(cameraListener)
                .build();
        cameraHelper.init();
    }

    /**
     * 在{@link #previewView}第一次布局完成后，去除该监听，并且进行引擎和相机的初始化
     */
    @Override
    public void onGlobalLayout() {
        previewView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        initCamera();
    }



}
