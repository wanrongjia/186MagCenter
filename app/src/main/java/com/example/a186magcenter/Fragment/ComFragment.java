package com.example.a186magcenter.Fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.a186magcenter.R;
import com.example.a186magcenter.SerialAPI.SerialDataBuf;
import com.example.a186magcenter.SerialAPI.SerialFinder;
import com.example.a186magcenter.Service.ComService;

public class ComFragment extends Fragment {


    Button mBtnSend;//发送按钮
    ToggleButton mToggleBtn; //串口开关
    EditText mRecText, mSendText; //收和发文本框
    Spinner mSpinner; //显示设备的下拉菜单
    int recCount = 0;

    SerialFinder finder;
    ComService service;

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ComService.COM_MESSAGE_RECEIVED:
                    //TODO 更新到界面
                    SerialDataBuf dataBuf = (SerialDataBuf) msg.obj;
                    updateReceiveData(dataBuf);
                    break;
                //case COMService.COM_MESSAGE_SEND:
                //TODO 添加发送计数
                //break;
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finder = new SerialFinder();
        service = new ComService(mHandler);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_com,
                container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ArrayAdapter<String> dev =
                new ArrayAdapter<>(getActivity(), R.layout.message);
        dev.add(finder.getAllDevicesPath());
        mSpinner = view.findViewById(R.id.spinner_com);
        mSpinner.setAdapter(dev);

        mToggleBtn = view.findViewById(R.id.tbtn_com);
        mToggleBtn.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton v, boolean isChecked) {
                        if(isChecked){
                            //TODO 打开串口
                            openCOM();
                            setTitle("串口打开");}
                        else{
                            //TODO 关闭串口
                            closeCOM();
                            setTitle("串口关闭");}
                    }
                }
        );

        mBtnSend = view.findViewById(R.id.btn_com_send);
        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { //TODO 发送
                send(mSendText.getText().toString());
            }
        });

        mSendText = view.findViewById(R.id.edit_com_send);
        mRecText = view.findViewById(R.id.edit_com_rec);
    }

    private void openCOM() {
        if(service != null)
            service.start();
    }

    private void closeCOM() {
        if(service != null)
            service.stop();
    }

    private void send(String out){
        if(service.getState() != ComService.STATE_OPEN){
            Toast.makeText(getContext(),
                    "串口没有打开",Toast.LENGTH_SHORT);
        } else {
            if(out.length()>0)
                service.send(out);
        }
    }


    public void updateReceiveData(SerialDataBuf recData){
        StringBuilder sMsg = new StringBuilder();
        sMsg.append(recData.mTime);
        sMsg.append(":");
        sMsg.append(new String(recData.mData));
        sMsg.append("\r\n");
        mRecText.append(sMsg);
        recCount++;
        if (recCount > 20)
        {
            mRecText.setText("");
            recCount = 0;
        }
    }

    private void setTitle(String info) {
        AppCompatActivity a =
                (AppCompatActivity)getActivity();
        if(a != null){
            ActionBar actionBar = a.getSupportActionBar();
            actionBar.setSubtitle(info);
        }
    }
}
