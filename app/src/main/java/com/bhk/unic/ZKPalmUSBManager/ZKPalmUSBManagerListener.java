package com.bhk.unic.ZKPalmUSBManager;

import android.hardware.usb.UsbDevice;

public interface ZKPalmUSBManagerListener
{
    //0 means success
    //-1 means device no found
    //-2 means device no permission
    void onCheckPermission(int result);

    void onUSBArrived(UsbDevice device);

    void onUSBRemoved(UsbDevice device);
}
