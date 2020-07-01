package com.example.a186magcenter.Service;


import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Handler;


import com.example.a186magcenter.UsbDriver.UsbDeviceDriver;
import com.example.a186magcenter.UsbDriver.UsbDeviceFinder;

import java.nio.ByteBuffer;
import java.util.List;


public class USBService {

    private static final int READ_WAIT_MILLIS = 1000;
    private static final int BUFSIZ = 512;

    private UsbDeviceDriver mDriver;
    private UsbReadWriteThread mReadWriteThread;

    private int mState = STATE_STOPPED;

    public static final int STATE_STOPPED = 0;
    public static final int STATE_RUNNING = 1;

    public static final int USB_MESSAGE_SEND = 30;
    public static final int USB_MESSAGE_RECEIVED = 31;

    private final Handler mHandler;
    private final UsbManager mUsbManager;


    private static PendingIntent  mUSBPermissionIntent = null;

    private Context mContext;

    public USBService(UsbManager manager, Handler handler, Context context)
    {
        mUsbManager = manager;
        mHandler = handler;
        mContext = context;
        findDriver();
    }

    public void openDevice()
    {
        if (mDriver.permissionStatus() != UsbDeviceDriver.permissionStatusSuccess) {
            requestPermission();
        }
        mDriver.setConnection(mUsbManager.openDevice(mDriver.getDevice()));
        mDriver.open();
        mDriver.setPermissionStatus(UsbDeviceDriver.permissionStatusOpen);
    }

    public void requestPermission(){
        mUSBPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent("USB_PERMISSION"), 0);
        getDriver().setPermissionStatus(UsbDeviceDriver.permissionStatusRequested);
        mUsbManager.requestPermission(getDriver().getDevice(), mUSBPermissionIntent);
    }

    public void findDriver()
    {
        List<UsbDeviceDriver> currentDrivers = UsbDeviceFinder.findAllDevices(mUsbManager);
        if(currentDrivers.size() == 0)
            return;

        mDriver = currentDrivers.get(0); //只操作第一个 CP2102
    }

    public void start()
    {
        if (mReadWriteThread != null) {
            mReadWriteThread.cancel();
            mReadWriteThread = null;
        }

        if(mReadWriteThread == null)
        {
            mReadWriteThread = new UsbReadWriteThread();
            mReadWriteThread.start();
        }
    }

    public void stop()
    {
        if (mReadWriteThread != null) {
            mReadWriteThread.cancel();
            mReadWriteThread = null;
        }
        if(mDriver != null) {
            mDriver.setPermissionStatus(UsbDeviceDriver.permissionStatusRequestRequired);
            mDriver.close();
        }
    }

    public void UsbSendData(byte[] out) {
        synchronized (this) {
            if (mState != STATE_RUNNING)
                return;
            mReadWriteThread.write(out);
            mHandler.obtainMessage(USB_MESSAGE_SEND, out.length, -1, out).sendToTarget();
        }
    }


    public UsbDeviceDriver getDriver()
    {
        return mDriver;
    }


    private class UsbReadWriteThread extends Thread {


        public UsbReadWriteThread() {

            mState = STATE_RUNNING;
        }

        @Override
        public void run()
        {
            super.run();

            final ByteBuffer mReadBuffer = ByteBuffer.allocate(BUFSIZ);

            while (mState ==  STATE_RUNNING) {

                int len = mDriver.read(mReadBuffer.array(), READ_WAIT_MILLIS);

                if (len > 0)
                {

                    final byte[] data = new byte[len];
                    mReadBuffer.get(data, 0, len);
                    mHandler.obtainMessage(USB_MESSAGE_RECEIVED, data.length, -1, data).sendToTarget();
                    mReadBuffer.clear();
                }
            }
        }



        public synchronized void write(byte[] out){
            if (out != null)
                mDriver.write(out, READ_WAIT_MILLIS);
        }

        public void cancel(){
            mState = STATE_STOPPED;
        }


    }

}


