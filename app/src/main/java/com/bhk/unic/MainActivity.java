package com.bhk.unic;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Base64;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bhk.unic.ZKPalmUSBManager.ZKPalmUSBManager;
import com.bhk.unic.ZKPalmUSBManager.ZKPalmUSBManagerListener;
import com.zkteco.android.biometric.core.utils.LogHelper;
import com.zkteco.android.biometric.core.utils.ToolUtils;
import com.zkteco.android.biometric.easyapi.ZKPalmApi;
import com.zkteco.android.biometric.easyapi.ZKPalmApiListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private final static int VID_ZKTECO   =   6997;
    private final static int PID_PAR200   =   1792;
    private TextView textView = null;
    private SurfaceView surfaceView = null;
    private EditText editText = null;
    private boolean bstart = false;
    private final static int ENROLL_CNT = 5;
    private ZKPalmApi zkPalmApi = new ZKPalmApi();
    private String regId = "";
    private final static int IDENTIFY_THRESOLD = 576;
    private int[] palmRect = new int[8];

    private ZKPalmUSBManager zkPalmUSBManager = null;

    private DBManager dbManager = new DBManager();

    private ZKPalmUSBManagerListener zkPalmUSBManagerListener = new ZKPalmUSBManagerListener(){
        public void onCheckPermission(int result) {
            LogHelper.d("ZKPalmUSBManagerListener onCheckPermission result" + result);
            if (0 == result)
            {
                afterGetUsbPermission();
            }
            else
            {
                setResult("init usb-permission failed, ret" + result);
            }
        }

        @Override
        public void onUSBArrived(UsbDevice device) {
            LogHelper.d("ZKPalmUSBManagerListener usb-arrived, usb" + device.toString());
            if (device.getVendorId() == VID_ZKTECO && device.getProductId() == PID_PAR200) {
                closeDevice();
                tryGetUSBPermission();
            }
        }

        @Override
        public void onUSBRemoved(UsbDevice device) {
            LogHelper.d("ZKPalmUSBManagerListener usb-removed, usb" + device.toString());
            if (device.getVendorId() == VID_ZKTECO && device.getProductId() == PID_PAR200) {
                closeDevice();
            }
        }
    };

    private void checkStoragePermission()
    {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) { ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                8);}
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) { ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                8);}
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
        checkStoragePermission();
        zkPalmUSBManager = new ZKPalmUSBManager(this.getApplicationContext(), zkPalmUSBManagerListener);
        zkPalmUSBManager.registerUSBPermissionReceiver();
        dbManager.opendb();
        //try open after app launch
        //tryGetUSBPermission();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        closeDevice();
        zkPalmUSBManager.unRegisterUSBPermissionReceiver();
    }

    private void initUI()
    {
        textView = (TextView)findViewById(R.id.txtResult);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        editText = (EditText)findViewById(R.id.editText);
    }

    private void tryGetUSBPermission()
    {
        zkPalmUSBManager.initUSBPermission();
    }

    private void afterGetUsbPermission()
    {
        openDevice();
    }


    public static void saveBinraryData(byte[] template, int size, String strPath, String fileName) {
        String filePathName = strPath  + "/" + fileName;
        File dirFile = new File(strPath);  //目录转化成文件夹
        if (!dirFile.exists()) {              //如果不存在，那就建立这个文件夹
            dirFile.mkdirs();
        }
        File file = new File(filePathName);
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream out;
        try {
            out = new FileOutputStream(file, true);
            out.write(template, 0, size);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return;
    }

    private void setResult(String strText)
    {
        final String flstrText = strText;
        runOnUiThread(new Runnable() {
            public void run() {
                textView.setText(flstrText);
            }
        });

    }

    private void drawFaceRectCorner(Canvas canvas, int[] rect) {
        Paint mFacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFacePaint.setStrokeWidth(4);
        mFacePaint.setStyle(Paint.Style.STROKE);
        mFacePaint.setColor(getResources().getColor(R.color.colorRect));

        canvas.drawLine(rect[0], rect[1], rect[2], rect[3], mFacePaint);
        canvas.drawLine(rect[2], rect[3], rect[4], rect[5], mFacePaint);
        canvas.drawLine(rect[4], rect[5], rect[6], rect[7], mFacePaint);
        canvas.drawLine(rect[6], rect[7], rect[0], rect[1], mFacePaint);
    }

    private void doDraw(SurfaceView surfaceView, Bitmap bitmap) {
        SurfaceHolder holder = surfaceView.getHolder();
        if (null == holder) {
            return;
        }
        Canvas canvas = holder.lockCanvas();
        if (null == canvas) {
            return;
        }
        Paint painter = new Paint();
        painter.setStyle(Paint.Style.FILL);
        painter.setAntiAlias(true);
        painter.setFilterBitmap(true);
        canvas.drawBitmap(bitmap, 0, 0, painter);
        drawFaceRectCorner(canvas, palmRect);
        holder.unlockCanvasAndPost(canvas);
    }

    private void openDevice()
    {
        int retVal = zkPalmApi.initalize(this.getApplicationContext(), VID_ZKTECO, PID_PAR200);
        if (0 != retVal)
        {
            // TODO: reset
            setResult("start capture failed, ret=" + retVal);
            return;
        }
        final ZKPalmApiListener listener = new ZKPalmApiListener() {
            @Override
            public void onCapture(int actionResult, byte[] palmImage) {
                if (0 == actionResult) {
                    final int width = zkPalmApi.getImageWidth();
                    final int height = zkPalmApi.getImageHeight();
                    Bitmap bitmapPalm = ToolUtils.renderCroppedGreyScaleBitmap(palmImage, width, height);
                    doDraw(surfaceView, bitmapPalm);
                }
                else {
                    //失败不做处理
                }
            }

            @Override
            public void onException() {
                LogHelper.e("deviceException");
                zkPalmApi.resetEx();
            }

            @Override
            public void onMatch(int actionResult, byte[] verTemplate) {
                if (0 == actionResult)
                {
                    String[] idRet = new String[1];
                    int retVal = zkPalmApi.dbIdentify(verTemplate, idRet);
                    if (retVal >= IDENTIFY_THRESOLD) {
                        setResult("identify succ, userid:" + idRet[0] + ", score:" + retVal);
                    } else {
                        setResult("identify fail, score=" + retVal);
                    }
                }
                else
                {
                    // 不做处理
                }
            }

            @Override
            public void onEnroll(int actionResult, int times, byte[] verTemplate, byte[] regTemplate) {
                int retVal = 0;
                if (0 == actionResult)
                {
                    String[] idRet = new String[1];
                    retVal = zkPalmApi.dbIdentify(verTemplate, idRet);
                    if (retVal >= IDENTIFY_THRESOLD)
                    {
                        setResult("the palm already enroll by " + idRet[0] + ",cancel enroll");
                        zkPalmApi.cancelEnroll();
                        return;
                    }
                    else
                    {
                        setResult("We need to capture " + (ENROLL_CNT - times) + "times the plam template");
                    }
                }
                else if (1 == actionResult)
                {
                    retVal = zkPalmApi.dbAdd(regId, regTemplate);
                    if (0 == retVal)
                    {
                        String strFeature = Base64.encodeToString(regTemplate, Base64.NO_WRAP);
                        dbManager.insertUser(regId, strFeature);
                        setResult("enroll succ， retVal=" + retVal);
                    }
                    else
                    {
                        setResult("enroll fail, ret=" + retVal);
                    }
                }
                else
                {
                    setResult("enroll failed, ret=" + actionResult);
                }
            }

            @Override
            public void onFeatureInfo(int actionResult, int imageQuality, int templateQuality, int[] rect) {
                if (0 == actionResult)
                {
                    System.arraycopy(rect, 0, palmRect, 0, rect.length);
                }
                else
                {
                    Arrays.fill(palmRect, 0x0);
                }
            }
        };
        zkPalmApi.setZKPalmApiListener(listener);
        zkPalmApi.startCapture();
        bstart = true;
        setResult("start capture succ");
    }

    public void OnBnBegin(View view) {
        if (bstart) return;
        tryGetUSBPermission();
    }

    private void closeDevice()
    {
        LogHelper.d("close device");
        if (bstart) {
            zkPalmApi.stopCapture();
            bstart = false;
            zkPalmApi.unInitialize();
        }
    }

    public void OnBnStop(View view)
    {
        if (bstart)
        {
            closeDevice();
            textView.setText("stop capture succ");
        }
        else
        {
            textView.setText("already stop");
        }
    }

    public void OnBnEnroll(View view) {
        if (bstart) {
            regId = editText.getText().toString();
            if (null == regId || regId.isEmpty())
            {
                textView.setText("please input your plamid");
                return;
            }
            if (dbManager.isPalmExited(regId))
            {
                textView.setText("the palm[" + regId + "] had registered!");
                return;
            }
            zkPalmApi.startEnroll();
            textView.setText("You need to put your palm 5 times above the sensor");
        }
        else
        {
            textView.setText("please begin capture first");
        }
    }

    public void OnBnVerify(View view) {
        if (bstart) {
            zkPalmApi.cancelEnroll();
        }else {
            textView.setText("please begin capture first");
        }
    }

    public void OnBnDel(View view)
    {
        if (bstart) {
            regId = editText.getText().toString();
            if (null == regId || regId.isEmpty())
            {
                textView.setText("please input your plamid");
                return;
            }
            if (!dbManager.isPalmExited(regId))
            {
                textView.setText("the plam no registered");
                return;
            }
            new  AlertDialog.Builder(this)
                    .setTitle("do you want to delete the plam?" )
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (dbManager.deleteUser(regId))
                            {
                                zkPalmApi.dbDel(regId);
                                setResult("delete success！");
                            }
                            else {
                                setResult("Open db fail！");
                            }
                        }
                    })
                    .setNegativeButton("no", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .show();
        }
    }

    public void OnBnClear(View view)
    {
        if (bstart) {
            new  AlertDialog.Builder(this)
                    .setTitle("do you want to delete all the plam?" )
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (dbManager.clear())
                            {
                                zkPalmApi.dbClear();
                                setResult("Clear success！");
                            }
                            else {
                                setResult("Open db fail！");
                            }
                        }
                    })
                    .setNegativeButton("no", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .show();
        }
    }

}

