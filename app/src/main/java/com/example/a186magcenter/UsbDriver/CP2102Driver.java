package com.example.a186magcenter.UsbDriver;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;

import java.util.LinkedHashMap;
import java.util.Map;

public class CP2102Driver implements UsbDeviceDriver {
    
    private static final int DEFAULT_BAUD_RATE = 9600;
    
    private static final int USB_WRITE_TIMEOUT_MILLIS = 5000;

    private static final int REQTYPE_HOST_TO_DEVICE = 0x41;

    private static final int SILABSER_IFC_ENABLE_REQUEST_CODE = 0x00;
    private static final int SILABSER_SET_BAUDDIV_REQUEST_CODE = 0x01;
    private static final int SILABSER_SET_MHS_REQUEST_CODE = 0x07;
    private static final int SILABSER_SET_BAUDRATE = 0x1E;


    private static final int UART_ENABLE = 0x0001;
    private static final int UART_DISABLE = 0x0000;

    private static final int BAUD_RATE_GEN_FREQ = 0x384000;
    

    private static final int MCR_ALL = 0x0003;
    
    private static final int CONTROL_WRITE_DTR = 0x0100;
    private static final int CONTROL_WRITE_RTS = 0x0200;    

    private UsbEndpoint mReadEndpoint;
    private UsbEndpoint mWriteEndpoint;

    protected byte[] mReadBuffer;
    protected byte[] mWriteBuffer;
    protected final UsbDevice   mDevice;

    protected final Object      mReadBufferLock = new Object();
    protected final Object      mWriteBufferLock = new Object();

    protected UsbDeviceConnection mConnection = null;

    public static final int DEFAULT_READ_BUFFER_SIZE = 16 * 1024;
    public static final int DEFAULT_WRITE_BUFFER_SIZE = 16 * 1024;

    public CP2102Driver(UsbDevice device) {

        mDevice = device;

        mReadBuffer = new byte[DEFAULT_READ_BUFFER_SIZE];
        mWriteBuffer = new byte[DEFAULT_WRITE_BUFFER_SIZE];
    }
    
    private int setConfigSingle(int request, int value) {
        return mConnection.controlTransfer(REQTYPE_HOST_TO_DEVICE, request, value, 
                0, null, 0, USB_WRITE_TIMEOUT_MILLIS);
    }

    @Override
    public void open() {
        boolean opened = false;
        try {
            for (int i = 0; i < mDevice.getInterfaceCount(); i++) {                
                UsbInterface usbIface = mDevice.getInterface(i);
                mConnection.claimInterface(usbIface, true);
            }                       
            
            UsbInterface dataIface = mDevice.getInterface(mDevice.getInterfaceCount() - 1);
            for (int i = 0; i < dataIface.getEndpointCount(); i++) {
                UsbEndpoint ep = dataIface.getEndpoint(i);
                if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
                        mReadEndpoint = ep;
                    } else {
                        mWriteEndpoint = ep;
                    }
                }
            }
            
            setConfigSingle(SILABSER_IFC_ENABLE_REQUEST_CODE, UART_ENABLE);
            setConfigSingle(SILABSER_SET_MHS_REQUEST_CODE, MCR_ALL | CONTROL_WRITE_DTR | CONTROL_WRITE_RTS);
            setConfigSingle(SILABSER_SET_BAUDDIV_REQUEST_CODE, BAUD_RATE_GEN_FREQ / DEFAULT_BAUD_RATE);            
            opened = true;
        } finally {
            if (!opened) {
                close();
            }
        }        
    }

    @Override
    public void close() {
        setConfigSingle(SILABSER_IFC_ENABLE_REQUEST_CODE, UART_DISABLE);
        mConnection.close();
    }

    @Override
    public int read(byte[] data, int timeoutMillis) {
        final int numBytesRead;
        synchronized (mReadBufferLock) {
            int readAmt = Math.min(data.length, mReadBuffer.length);
            numBytesRead = mConnection.bulkTransfer(mReadEndpoint, mReadBuffer, readAmt,
                    timeoutMillis);
            if (numBytesRead < 0) {
                return 0;
            }
            System.arraycopy(mReadBuffer, 0, data, 0, numBytesRead);
        }
        return numBytesRead;
    }

    @Override
    public int write(byte[] src, int timeoutMillis) {
        int offset = 0;

        while (offset < src.length) {
            final int writeLength;
            final int amtWritten;

            synchronized (mWriteBufferLock) {
                final byte[] writeBuffer;

                writeLength = Math.min(src.length - offset, mWriteBuffer.length);
                if (offset == 0) {
                    writeBuffer = src;
                } else {
                    System.arraycopy(src, offset, mWriteBuffer, 0, writeLength);
                    writeBuffer = mWriteBuffer;
                }

                amtWritten = mConnection.bulkTransfer(mWriteEndpoint, writeBuffer, writeLength,
                        timeoutMillis);
            }
            if (amtWritten <= 0) {
                return amtWritten;
            }

            offset += amtWritten;
        }
        return offset;
    }

    private void setBaudRate(int baudRate) {
        byte[] data = new byte[] {
                (byte) ( baudRate & 0xff),
                (byte) ((baudRate >> 8 ) & 0xff),
                (byte) ((baudRate >> 16) & 0xff),
                (byte) ((baudRate >> 24) & 0xff)
        };
        int ret = mConnection.controlTransfer(REQTYPE_HOST_TO_DEVICE, SILABSER_SET_BAUDRATE, 
                0, 0, data, 4, USB_WRITE_TIMEOUT_MILLIS);
    }


    public static Map<Integer, int[]> getSupportedDevices() {
        final Map<Integer, int[]> supportedDevices = new LinkedHashMap<Integer, int[]>();
        supportedDevices.put(Integer.valueOf(VENDOR_SILAB),
                new int[] {SILAB_CP2102});
        return supportedDevices;
    }


    private int _permissionStatus = permissionStatusRequestRequired;

    public int permissionStatus() {
        return _permissionStatus;
    }


    public void setPermissionStatus(int permissionStatus) {
        _permissionStatus = permissionStatus;
    }

    public void setConnection(UsbDeviceConnection connection) {
        mConnection = connection;
    }
    public final UsbDevice getDevice() {
        return mDevice;
    }
}
