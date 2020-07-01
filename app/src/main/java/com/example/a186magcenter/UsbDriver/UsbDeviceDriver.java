package com.example.a186magcenter.UsbDriver;


import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;


public interface UsbDeviceDriver {

    public static final int VENDOR_SILAB = 0x10c4;
    public static final int SILAB_CP2102 = 0xea60;

    public static final int permissionStatusSuccess =           0;
    public static final int permissionStatusDenied =            1;
    public static final int permissionStatusRequested =         2;
    public static final int permissionStatusRequestRequired =   3;
    public static final int permissionStatusOpen =              4;

    public int permissionStatus();
    public void setPermissionStatus(int permissionStatus);
    public void setConnection(UsbDeviceConnection connection);
    public UsbDevice getDevice();


    public void open();
    public void close();
    public int read(final byte[] dest, final int timeoutMillis);
    public int write(final byte[] src, final int timeoutMillis);

}
