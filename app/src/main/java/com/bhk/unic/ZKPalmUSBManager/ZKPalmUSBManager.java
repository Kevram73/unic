package com.bhk.unic.ZKPalmUSBManager;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import androidx.annotation.NonNull;

import com.zkteco.android.biometric.core.utils.LogHelper;
import com.zkteco.zkinfraredservice.irpalm.ZKPalmService12;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class ZKPalmUSBManager {
    //usb's vendor id for zkteco
    private int VID_ZK = 0x1b55;
    //usb's product id for PAR200
    private int PID_FA10M = 1792;
    //application context
    private Context mContext = null;

    /////////////////////////////////////////////
    //for usb permission
    private static final String SOURCE_STRING = "0123456789-_abcdefghigklmnopqrstuvwxyzABCDEFGHIGKLMNOPQRSTUVWXYZ";
    private static final int DEFAULT_LENGTH = 16;
    private String ACTION_USB_PERMISSION;
    private boolean mbRegisterFilter = false;
    private CountDownLatch countDownLatchPermission = null;
    private ZKPalmUSBManagerListener zkPalmUSBManagerListener = null;

    private BroadcastReceiver usbMgrReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action))
            {
                if (null != countDownLatchPermission)
                {
                    countDownLatchPermission.countDown();
                }
            }
            else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action))
            {
                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                zkPalmUSBManagerListener.onUSBArrived(device);
            }
            else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
            {
                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                zkPalmUSBManagerListener.onUSBRemoved(device);
            }
        }
    };


    private boolean isNullOrEmpty(String target) {
        if (null == target || "".equals(target) || target.isEmpty()) {
            return true;
        }
        return false;
    }

    private String createRandomString(String source, int length) {
        if (this.isNullOrEmpty(source)) {
            return "";
        }

        StringBuffer result = new StringBuffer();
        Random random = new Random();

        for(int index = 0; index < length; index++) {
            result.append(source.charAt(random.nextInt(source.length())));
        }
        return result.toString();
    }

    public boolean registerUSBPermissionReceiver()
    {
        if (null == mContext || mbRegisterFilter)
        {
            return false;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        mContext.registerReceiver(usbMgrReceiver, filter);
        mbRegisterFilter = true;
        return true;
    }

    public void unRegisterUSBPermissionReceiver()
    {
        if (null == mContext || !mbRegisterFilter)
        {
            return;
        }
        mContext.unregisterReceiver(usbMgrReceiver);
        mbRegisterFilter = false;
    }


    //End USB Permission
    /////////////////////////////////////////////

    public ZKPalmUSBManager(@NonNull Context context, @NonNull ZKPalmUSBManagerListener listener)
    {
        super();
        if (null == context || null == listener)
        {
            throw new NullPointerException("context or listener is null");
        }
        zkPalmUSBManagerListener = listener;
        ACTION_USB_PERMISSION = createRandomString(SOURCE_STRING, DEFAULT_LENGTH);
        mContext = context;
    }

    //0 means success
    //-1 means device no found
    //-2 means device no permission
    //cann't run on ui thread
    private int checkUSBPermission()
    {
        UsbManager usbManager = null;
        UsbDevice usbKeyDeviceSelect = null;
        UsbDevice fa10mDeviceSelect = null;
        long nTickstart = System.currentTimeMillis();
        while (System.currentTimeMillis()-nTickstart <= 3*1000)
        {
            usbKeyDeviceSelect = null;
            fa10mDeviceSelect = null;
            usbManager = (UsbManager)mContext.getSystemService(Context.USB_SERVICE);
            for (UsbDevice device : usbManager.getDeviceList().values()) {
                int device_vid = device.getVendorId();
                int device_pid = device.getProductId();
                if (device_vid == ZKPalmService12.getUKeyVendorID() && device_pid == ZKPalmService12.getUKeyProductID())
                {
                    usbKeyDeviceSelect = device;
                }
                else if (device_vid == VID_ZK && device_pid == PID_FA10M)
                {
                    fa10mDeviceSelect = device;
                }
                if (null != usbKeyDeviceSelect && null != fa10mDeviceSelect)
                {
                    break;
                }
            }
            if (null != usbKeyDeviceSelect && null != fa10mDeviceSelect)
            {
                break;
            }
        }

        if (null == usbKeyDeviceSelect || null == fa10mDeviceSelect)
        {
            LogHelper.e("usbKeyDeviceSelect=" + usbKeyDeviceSelect + ",fa10mDeviceSelect=" + fa10mDeviceSelect);
            return -1;
        }
        if (!usbManager.hasPermission(usbKeyDeviceSelect))
        {
            countDownLatchPermission = new CountDownLatch(1);
            Intent intent = new Intent(this.ACTION_USB_PERMISSION);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            usbManager.requestPermission(usbKeyDeviceSelect, pendingIntent);
            try {
                countDownLatchPermission.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            countDownLatchPermission = null;
            if (!usbManager.hasPermission(usbKeyDeviceSelect))
            {
                LogHelper.e("usb key not permission");
                return -2;
            }
        }
        if (!usbManager.hasPermission(fa10mDeviceSelect))
        {
            countDownLatchPermission = new CountDownLatch(1);
            Intent intent = new Intent(this.ACTION_USB_PERMISSION);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
            usbManager.requestPermission(fa10mDeviceSelect, pendingIntent);
            try {
                countDownLatchPermission.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            countDownLatchPermission = null;
            if (!usbManager.hasPermission(fa10mDeviceSelect))
            {
                LogHelper.e("palm sensor not permission");
                return -2;
            }
        }
        return 0;
    }

    public void initUSBPermission(){
        LogHelper.d("initUSBPermission beign");
        new Thread(new Runnable() { @Override public void run() {
            int retVal = checkUSBPermission();
            LogHelper.d("initUSBPermission ret=" + retVal);
            zkPalmUSBManagerListener.onCheckPermission(retVal);
        } }).start();
    }

}
