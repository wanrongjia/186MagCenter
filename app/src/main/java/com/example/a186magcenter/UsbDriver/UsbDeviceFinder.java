package com.example.a186magcenter.UsbDriver;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import java.util.ArrayList;
import java.util.List;

public class UsbDeviceFinder {


    private static List<UsbDeviceDriver> getSupportDriver(final UsbDevice usbDevice)
    {
        List<UsbDeviceDriver> result = new ArrayList<UsbDeviceDriver>();
        boolean isSupported = CP2102Driver.getSupportedDevices().containsKey(usbDevice.getVendorId());
        if (!isSupported) {
            return result;
        }
        result.add(new CP2102Driver(usbDevice));
        return result;
    }


    public static List<UsbDeviceDriver> findAllDevices(final UsbManager usbManager) {
        final List<UsbDeviceDriver> result = new ArrayList<UsbDeviceDriver>();
        for (UsbDevice usbDevice : usbManager.getDeviceList().values()) {
            result.addAll(getSupportDriver(usbDevice));
        }
        return result;
    }
}
