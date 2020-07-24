package com.zgk.arcfacedemo.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.arcsoft.face.AgeInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.Face3DAngle;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.GenderInfo;
import com.arcsoft.face.LivenessInfo;
import com.arcsoft.face.enums.DetectFaceOrientPriority;
import com.arcsoft.face.enums.DetectMode;
import com.arcsoft.imageutil.ArcSoftImageFormat;
import com.arcsoft.imageutil.ArcSoftImageUtil;
import com.arcsoft.imageutil.ArcSoftImageUtilError;
import com.bumptech.glide.Glide;
import com.zgk.arcfacedemo.R;
import com.zgk.arcfacedemo.util.ImageUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class JianCeActivity extends AppCompatActivity {

    private static final int CHOOSE_PHOTO = 1;
    private static final int GET_STORAGE_PERMISSION = 2;

    private static final String TAG = "JianCeActivity";
    private ImageView ivShow;
    private TextView tvNotice;
    private FaceEngine faceEngine;
    private int faceEngineCode = -1;
    private Bitmap mBitmap;

    private Context mContext = this;

    /**
     * 请求选择本地图片文件的请求码
     */
    private static final int ACTION_CHOOSE_IMAGE = 0x201;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jian_ce);

        ivShow = findViewById(R.id.iv_show);
        tvNotice = findViewById(R.id.tv_notice);

        initEngine();

    }

    /**
     * 初始化引擎
     */
    private void initEngine() {
        faceEngine = new FaceEngine();
        int faceEngineCode = faceEngine.init(
                this,
                DetectMode.ASF_DETECT_MODE_IMAGE,
                DetectFaceOrientPriority.ASF_OP_ALL_OUT,
                16, 10,
                FaceEngine.ASF_FACE_RECOGNITION |
                        FaceEngine.ASF_FACE_DETECT |
                        FaceEngine.ASF_AGE | FaceEngine.ASF_GENDER |
                        FaceEngine.ASF_FACE3DANGLE |
                        FaceEngine.ASF_LIVENESS);

        Log.i(TAG, "initEngine: init: " + faceEngineCode);

        if (faceEngineCode != ErrorInfo.MOK) {
            showToast(getString(R.string.init_failed, faceEngineCode));
        }
    }

    /**
     * 销毁引擎
     */
    private void unInitEngine() {
        if (faceEngine != null) {
            faceEngineCode = faceEngine.unInit();
            faceEngine = null;
            Log.i(TAG, "unInitEngine: " + faceEngineCode);
            System.out.println("引擎销毁啦！！！");
        }
    }

    @Override
    protected void onDestroy() {

        unInitEngine();
        super.onDestroy();
    }

    /**
     * 按钮从本地选择文件
     *
     * @param view
     */
    public void chooseLocalImage(final View view) {

        if (ContextCompat.checkSelfPermission(JianCeActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //没有授权进行权限申请
            ActivityCompat.requestPermissions(
                    JianCeActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    GET_STORAGE_PERMISSION);
        } else {
            pickPhoto();
        }

    }

    /*
     * 重写onRequestPermissionsResult()方法，对权限申请结果做处理
     * */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case GET_STORAGE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickPhoto();
                } else {
                    Toast.makeText(this, "You denied the permission", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void pickPhoto() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_PHOTO); // 打开相册
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CHOOSE_PHOTO:
                if (resultCode == RESULT_OK) {

                    //调用ArcSoftImageUtil.getAlignedBitmap()获取宽高符合要求的图
                    mBitmap = ArcSoftImageUtil.getAlignedBitmap(ImageUtil.getBitmapFromUri(data.getData(), this), true);

                    if (mBitmap == null) {
                        showToast("mBitmap是空的");
                        return;
                    }

                    ivShow.setImageBitmap(mBitmap);

                    //使用Glide来加载图片data.getData()得到图片的Uri
                    //Glide.with(this).load(mBitmap).into(ivShow);
                }
        }
    }


    /**
     * 按钮点击响应事件
     *
     * @param view
     */
    public void process(final View view) {
        //图像转化操作和部分引擎调用比较耗时，建议放子线程操作
        Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> emitter) throws Exception {
                processImage();
                emitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Object>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Object o) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        view.setClickable(true);
                    }
                });
    }

    /**
     * 按钮点击响应事件主要操作逻辑部分(子线程中)
     */
    public void processImage() {
        /**
         * 1.准备操作（校验，显示，获取BGR）
         */
        // 图像对齐
        Bitmap bitmap = ArcSoftImageUtil.getAlignedBitmap(mBitmap, true);

        int width = bitmap.getWidth();//宽
        int height = bitmap.getHeight();//高
        final Bitmap finalBitmap = bitmap;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Glide.with(ivShow.getContext())
                        .load(finalBitmap)
                        .into(ivShow);
            }
        });

        /*
         * bitmap转bgr24
         * */
        // 为图像数据分配内存
        byte[] bgr24 = ArcSoftImageUtil.createImageData(width, height, ArcSoftImageFormat.BGR24);
        // 图像格式转换
        int transformCode = ArcSoftImageUtil.bitmapToImageData(bitmap, bgr24, ArcSoftImageFormat.BGR24);
        if (transformCode != ArcSoftImageUtilError.CODE_SUCCESS) {
            Log.i(TAG, "transform failed, code is : " + transformCode);
            return;
        }

        /**
         * 2.成功获取到了BGR24 数据，开始人脸检测
         */
        List<FaceInfo> faceInfoList = new ArrayList<>();
        int code = faceEngine.detectFaces(bgr24, width, height, FaceEngine.CP_PAF_BGR24, faceInfoList);
        if (code == ErrorInfo.MOK && faceInfoList.size() > 0) {
            Log.i(TAG, "detectFaces, face num is : " + faceInfoList.size());
        } else {
            Log.i(TAG, "no face detected, code is : " + code);
        }

        //绘制bitmap
        Bitmap bitmapForDraw = bitmap.copy(Bitmap.Config.RGB_565, true);
        Canvas canvas = new Canvas(bitmapForDraw);
        Paint paint = new Paint();
        /**
         * 3.若检测结果人脸数量大于0，则在bitmap上绘制人脸框并且重新显示到ImageView，若人脸数量为0，则无法进行下一步操作，操作结束
         */
        if (faceInfoList.size() > 0) {
            paint.setAntiAlias(true);
            paint.setStrokeWidth(5);
            paint.setColor(Color.YELLOW);
            for (int i = 0; i < faceInfoList.size(); i++) {
                //绘制人脸框
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawRect(faceInfoList.get(i).getRect(), paint);
                //绘制人脸序号
                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                int textSize = faceInfoList.get(i).getRect().width() / 2;
                paint.setTextSize(textSize);

                canvas.drawText(String.valueOf(i), faceInfoList.get(i).getRect().left, faceInfoList.get(i).getRect().top, paint);
            }
            //显示
            final Bitmap finalBitmapForDraw = bitmapForDraw;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Glide.with(ivShow.getContext())
                            .load(finalBitmapForDraw)
                            .into(ivShow);
                }
            });
        }

        /**
         * 4.上一步已获取到人脸位置和角度信息，传入给process函数，进行年龄、性别、三维角度、活体检测
         */
        int faceProcessCode = faceEngine.process(bgr24, width, height,
                FaceEngine.CP_PAF_BGR24, faceInfoList,
                FaceEngine.ASF_AGE |
                        FaceEngine.ASF_GENDER |
                        FaceEngine.ASF_FACE3DANGLE |
                        FaceEngine.ASF_LIVENESS);
        //年龄信息结果
        List<AgeInfo> ageInfoList = new ArrayList<>();
        //性别信息结果
        List<GenderInfo> genderInfoList = new ArrayList<>();
        //人脸三维角度结果
        List<Face3DAngle> face3DAngleList = new ArrayList<>();
        //活体检测结果
        List<LivenessInfo> livenessInfoList = new ArrayList<>();
        //获取年龄、性别、三维角度、活体结果
        int ageCode = faceEngine.getAge(ageInfoList);
        int genderCode = faceEngine.getGender(genderInfoList);
        int face3DAngleCode = faceEngine.getFace3DAngle(face3DAngleList);
        int livenessCode = faceEngine.getLiveness(livenessInfoList);

        /**
         * 5.年龄、性别、三维角度已获取成功，添加信息到提示文字中
         */
        final SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
        //年龄数据
        if (ageInfoList.size() > 0) {
            stringBuilder.append("年龄：" + "\n");
            for (int i = 0; i < ageInfoList.size(); i++) {
                stringBuilder.append("face[" + String.valueOf(i) + "]:" + String.valueOf(ageInfoList.get(i).getAge()) + "\n");
            }
        }

        //性别数据
        String gender = "";
        if (genderInfoList.size() > 0) {
            stringBuilder.append("性别：" + "\n");
            for (int i = 0; i < genderInfoList.size(); i++) {
                if (genderInfoList.get(i).getGender() == 0) gender = "男"; else gender = "女";
                stringBuilder.append("face[" + String.valueOf(i) + "]:" + gender + "\n");
            }
        }


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvNotice.setText(stringBuilder);
            }
        });


    }


    protected void showToast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    protected void showLongToast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
}

