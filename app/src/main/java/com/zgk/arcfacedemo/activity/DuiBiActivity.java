package com.zgk.arcfacedemo.activity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.enums.DetectFaceOrientPriority;
import com.arcsoft.face.enums.DetectMode;
import com.arcsoft.imageutil.ArcSoftImageFormat;
import com.arcsoft.imageutil.ArcSoftImageUtil;
import com.zgk.arcfacedemo.R;
import com.zgk.arcfacedemo.util.ImageUtil;

import java.util.ArrayList;
import java.util.List;

public class DuiBiActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "DuiBiActivity";
    private static final int CHOOSE_PHOTO1 = 0x001;
    private static final int CHOOSE_PHOTO2 = 0x002;

    private Button btn_one, btn_two;
    private ImageView img_one, img_two;
    private TextView txt_one, txt_two;

    private Bitmap bitmap1, bitmap2;

    private FaceEngine faceEngine = null;
    private FaceFeature
            faceFeature1 = new FaceFeature(),
            faceFeature2 = new FaceFeature();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dui_bi);

        init();
        initEngine();
    }

    private void init() {
        btn_one = findViewById(R.id.bt_choose_main_image1);
        btn_two = findViewById(R.id.bt_choose_main_image2);
        img_one = findViewById(R.id.iv_main_image1);
        img_two = findViewById(R.id.iv_main_image2);
        txt_one = findViewById(R.id.tv_main_image_info1);
        txt_two = findViewById(R.id.tv_main_image_info2);

        btn_one.setOnClickListener(this);
        btn_two.setOnClickListener(this);
    }

    private void initEngine() {
        faceEngine = new FaceEngine();
        int faceEngineCode = faceEngine.init(this,
                DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_0_ONLY,
                16, 1,
                FaceEngine.ASF_FACE_RECOGNITION | FaceEngine.ASF_FACE_DETECT);
        if (faceEngineCode == 0)
            System.out.println("引擎初始化成功");
        else
            System.out.println("引擎初始化失败");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_choose_main_image1:
                pickPhoto(CHOOSE_PHOTO1);
                break;
            case R.id.bt_choose_main_image2:
                pickPhoto(CHOOSE_PHOTO2);
                break;
            default:
                break;
        }
    }

    private void pickPhoto(int CHOOSE_PHOTO) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_PHOTO); // 打开相册
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CHOOSE_PHOTO1:
                if (resultCode == RESULT_OK) {
                    //调用ArcSoftImageUtil.getAlignedBitmap()获取宽高符合要求的图
                    bitmap1 = ArcSoftImageUtil.getAlignedBitmap(ImageUtil.getBitmapFromUri(data.getData(), this), true);
                    if (bitmap1 == null) {
                        //showToast("mBitmap是空的");
                        return;
                    }
                    processImage(bitmap1, img_one, faceFeature1);
                    //使用Glide来加载图片data.getData()得到图片的Uri
                    //Glide.with(this).load(mBitmap).into(ivShow);
                }
                break;
            case CHOOSE_PHOTO2:
                if (resultCode == RESULT_OK) {
                    //调用ArcSoftImageUtil.getAlignedBitmap()获取宽高符合要求的图
                    bitmap2 = ArcSoftImageUtil.getAlignedBitmap(ImageUtil.getBitmapFromUri(data.getData(), this), true);
                    if (bitmap2 == null) {
                        //showToast("mBitmap是空的");
                        return;
                    }
                    processImage(bitmap2, img_two, faceFeature2);
                    //使用Glide来加载图片data.getData()得到图片的Uri
                    //Glide.with(this).load(mBitmap).into(ivShow);
                }
                break;
            default:
                break;
        }
    }

    public void processImage(Bitmap bitmap, ImageView img, FaceFeature faceFeature) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap mBitmap = ArcSoftImageUtil.getAlignedBitmap(bitmap, true);
                int width = mBitmap.getWidth();
                int heigt = mBitmap.getHeight();
                final Bitmap finalBitmap = mBitmap;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        img.setImageBitmap(finalBitmap);
                    }
                });

                /*
                 * mBitmap转bgr24
                 * */
                byte[] brg24 = ArcSoftImageUtil.createImageData(width, heigt, ArcSoftImageFormat.BGR24);
                int brgCode = ArcSoftImageUtil.bitmapToImageData(mBitmap, brg24, ArcSoftImageFormat.BGR24);
                if (brgCode == 0)
                    System.out.println("转换成功");
                else
                    System.out.println("转换失败");

                List<FaceInfo> faceInfoList = new ArrayList<>();
                int detectFacesCode = faceEngine.detectFaces(brg24, width, heigt, FaceEngine.CP_PAF_BGR24, faceInfoList);
                if (detectFacesCode == ErrorInfo.MOK && faceInfoList.size() > 0) {
                    Log.i(TAG, "detectFaces, face num is : " + faceInfoList.size());

                    int extractCode = faceEngine.extractFaceFeature(brg24, width, heigt,
                            FaceEngine.CP_PAF_BGR24, faceInfoList.get(0), faceFeature);
                    if (extractCode == ErrorInfo.MOK) {
                        Log.i(TAG, "成功提取人脸特征");
                    } else {
                        Log.i(TAG, "提取人脸特征失败，错误码：" + extractCode);
                    }

                } else {
                    Log.i(TAG, "no face detected, code is : " + detectFacesCode);
                }
            }
        }).start();

    }


}
