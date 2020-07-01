package com.example.a186magcenter.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.a186magcenter.DevActivity;
import com.example.a186magcenter.R;
import com.example.a186magcenter.Service.BTService;

import java.util.ServiceLoader;


public class BTFragment extends Fragment {

    private BluetoothAdapter  mBluetoothAdapter = null;//管理蓝牙

    private ListView mMsgListView = null;
    private EditText mMsgEditTV = null;
    private Button mSendBtn = null;
    private final int MyResult = 1;
    private ArrayAdapter<String> mMsgAdapter;

    private BTService service = null;
    private String remoteDevName;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what)
            {
                case BTService.BT_MSG_RECEIVED:
                    byte[] readBuf = (byte[])msg.obj;
                    String message = new String(readBuf, 0, msg.arg1);
                    mMsgAdapter.add("接收到:" + message);
                    break;
                case BTService.BT_MSG_STATE_CHANGE:
                    switch (msg.arg1){
                        case BTService.STATE_CONNECTED:
                            setTitle("连接至 "+remoteDevName);
                            break;
                        case BTService.STATE_CONNECTING:
                            setTitle("正在连接。。。");
                            break;
                        case BTService.STATE_LISTEN:
                            setTitle("等待连接。。。");
                            break;
                        case BTService.STATE_NONE:
                            setTitle("未连接");
                            break;
                    }
                    break;
                case BTService.BT_MSG_STATE_CONNECTED:
                    byte[] name = (byte[])msg.obj;
                    remoteDevName = new String(name, 0, msg.arg1);
                    break;
            }
        }
    };
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter ==  null)
            Toast.makeText(getContext(),
                    "蓝牙不可用", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.options, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.secure_connect_scan:
                //TODO 显示已配对的设备列表
                Intent i = new Intent(getActivity(), DevActivity.class);
                startActivityForResult(i, 123);
                return true;
            case R.id.insecure_connect_scan:
                //TODO 显示已配对的设备列表
                Intent ii = new Intent(getActivity(), DevActivity.class);
                startActivityForResult(ii, 456);
                return true;
            case R.id.discoverable:

                if(mBluetoothAdapter.getScanMode()
                        != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
                { //TODO 利用 Intent 设置蓝牙可以被发现
                    Intent iii = new Intent(BluetoothAdapter
                            .ACTION_REQUEST_DISCOVERABLE);
                    iii.putExtra(BluetoothAdapter
                            .EXTRA_DISCOVERABLE_DURATION, 300);
                    startActivity(iii);

                } else
                    Toast.makeText(getActivity(),"可以被发现",
                            Toast.LENGTH_SHORT).show();
                return true;
        }
        return false;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bt_chat, container,
                false);
    }

    @Override
    public void onViewCreated(@NonNull View v,
                              @Nullable Bundle b) {
        mMsgListView =  v.findViewById(R.id.msg_list);
        mMsgEditTV = v.findViewById(R.id.edit_msg);
        mSendBtn = v.findViewById(R.id.btn_send);
    }

    @Override
    public void onStart() {
        super.onStart();
        if( mBluetoothAdapter.isEnabled() == false){
            //TODO 跳转开启蓝牙
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(i, MyResult);
        } else
            setupChat();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 @Nullable Intent data) {
        switch (requestCode) {
            case MyResult:
                if (resultCode == AppCompatActivity.RESULT_OK){
                    //TODO 初始化聊天会话
                    setupChat();
                }else {
                    Toast.makeText(getContext(),
                            "打开蓝牙失败", Toast.LENGTH_SHORT).show();
                }
                break;
            case 123:
                if(resultCode == Activity.RESULT_OK)
                {//TODO 建立安全连接
                    String addr = data.getStringExtra("ADDR");
                    connectToDev(true, addr);
                }
                break;
            case 456:
                if(resultCode == Activity.RESULT_OK)
                {//TODO 建立非安全连接
                    String addr = data.getStringExtra("ADDR");
                    connectToDev(false, addr);
                }
                break;
            default:
                break;
        }
    }

    private void connectToDev(boolean secure, String addr)
    {
        service.connect(secure, mBluetoothAdapter.getRemoteDevice(addr));
    }

    private void setupChat() {
        mMsgAdapter = new ArrayAdapter<>(getActivity(),
                R.layout.message);
        mMsgListView.setAdapter(mMsgAdapter);
        mSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mMsgEditTV != null) {
                    String msg = mMsgEditTV.getText().toString();
                    //TODO 发消息
                    mMsgAdapter.add(msg);
                    sendMsg(msg);
                    Toast.makeText(getContext(),
                            "发消息", Toast.LENGTH_SHORT).show();

                }
            }
        });

        mMsgEditTV.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(event.getAction() == KeyEvent.ACTION_UP)
                {
                    String msg = v.getText().toString();
                    //TODO 发消息
                    mMsgAdapter.add(msg);
                    sendMsg(msg);
                    Toast.makeText(getContext(),
                            "发消息", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
        service = new BTService(mBluetoothAdapter, mHandler);
    }

    private void sendMsg(String message) {
        if(service.getState() != BTService.STATE_CONNECTED){
            Toast.makeText(getContext(),
                    "蓝牙没有连接", Toast.LENGTH_SHORT).show();
        } else {
            if(message.length() > 0)
            {
                service.send(message);
                mMsgEditTV.setText("");
            }
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        if(service != null){
            if(service.getState() == BTService.STATE_NONE)
                service.start();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(service != null)
            service.stop();
    }

    private void setTitle(String info){
        AppCompatActivity a = (AppCompatActivity)getActivity();
        if (a != null){
            ActionBar actionBar = a.getSupportActionBar();
            String ss =stringFromJNI();
            actionBar.setSubtitle(info);
        }
    }
    private native static String stringFromJNI();
    static {
        System.loadLibrary("native-lib");
    }
}
