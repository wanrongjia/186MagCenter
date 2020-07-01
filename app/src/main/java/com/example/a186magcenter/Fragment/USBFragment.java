package com.example.a186magcenter.Fragment;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.a186magcenter.Service.USBService;
import com.example.a186magcenter.R;
import com.example.a186magcenter.UsbDriver.UsbDeviceDriver;


public class USBFragment extends Fragment
{
    private UsbManager   mUsbManager = null;
    private USBService mUSBService;


    ToggleButton mUSBToggleBtn;
    Button mUSBSendBtn;
    EditText mUSBRecData;
    EditText mUSBSendData;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case USBService.USB_MESSAGE_RECEIVED:
                    byte[] recBuf = (byte[]) msg.obj;
                    String recData = new String(recBuf, 0, msg.arg1);
                    mUSBRecData.append(recData);
                    break;
                case USBService.USB_MESSAGE_SEND:
                    break;
            }
        }
    };



    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mUsbManager = (UsbManager)((AppCompatActivity) getActivity()).getSystemService(Context.USB_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction("USB_PERMISSION");
        getActivity().registerReceiver(usbReceiver, filter);


        mUSBService = new USBService(mUsbManager, mHandler, getContext());

        if(mUSBService.getDriver() != null)
        {
            UsbDevice device = mUSBService.getDriver().getDevice();

            if (mUsbManager.hasPermission(device)) {
                mUSBService.getDriver().setPermissionStatus(UsbDeviceDriver.permissionStatusSuccess);
            } else {
                mUSBService.getDriver().setPermissionStatus(UsbDeviceDriver.permissionStatusRequested);
                mUSBService.requestPermission();
            }
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_usb, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mUSBToggleBtn = view.findViewById(R.id.tbtn_usb);
        mUSBSendBtn = view.findViewById(R.id.btn_usb_send);
        mUSBSendData = view.findViewById(R.id.edit_usb_send);
        mUSBRecData = view.findViewById(R.id.edit_usb_rec);

        mUSBToggleBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked){
                        if(mUsbManager != null){
                            mUSBService.openDevice();
                            mUSBService.start();
                        }
                        else {
                            Toast.makeText(getContext(), "没找到支持的 USB 设备", Toast.LENGTH_SHORT).show();
                            mUSBService.findDriver();
                            mUSBToggleBtn.setChecked(false);
                        }
                    }else {
                        closeUSB();
                    }
            }
        });

        mUSBSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                write(mUSBSendData.getText().toString().getBytes());
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
    }


    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (action.equals("USB_PERMISSION")) {
                        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (device != null) {
                            UsbDeviceDriver driver = mUSBService.getDriver();
                            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                                Log.d("","权限获取成功 " + device.getDeviceName());
                                driver.setPermissionStatus(UsbDeviceDriver.permissionStatusSuccess);
                            } else {
                                Log.d("","权限获取失败 " + device.getDeviceName());
                                driver.setPermissionStatus(UsbDeviceDriver.permissionStatusDenied);
                            }
                        }
                }
            }
        };


    @Override
    public void onResume() {
        super.onResume();
    }

    public void closeUSB()
    {
        if (mUSBService == null)
            return;
        mUSBService.stop();
    }


    public void write(byte[] out)
    {
        if (mUSBService != null)
            mUSBService.UsbSendData(out);
    }


    public static USBFragment newInstance() {
        return new USBFragment();
    }

}
