package com.example.a186magcenter.Service;

import android.os.Handler;

import com.example.a186magcenter.SerialAPI.SerialDataBuf;
import com.example.a186magcenter.SerialAPI.SerialPort;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ComService {

    private int mState;
    public static final int STATE_OPEN = 0;
    public static final int STATE_CLOSE = 1;

    public static final int COM_MESSAGE_RECEIVED = 20;

    private  COMRWThread rwThread = null;
    private Handler mHandler;

    private int baud = 9600;
    public ComService(Handler handler)
    {
        mState = STATE_CLOSE;
        mHandler = handler;
    }
    public int getState() { return  mState;}

    private class COMRWThread extends Thread {
        private SerialPort com; //串口对象 实际上是文件
        private OutputStream mOutputStream;
        private InputStream mInputStream;

        public COMRWThread() {
            //TODO 实例化 com 对象
            //TODO 得到 com 的 iN Out Stream 对象
            try {
                com = new SerialPort(new File("/dev/ttySAC4"), baud, 0);
                mOutputStream = com.getOutputStream();
                mInputStream = com.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mState = STATE_OPEN;
        }

        @Override
        public void run() {
            super.run();
            byte[] readBuf = new byte[1024];
            int length;
            while(mState == STATE_OPEN){
                try {
                    length = mInputStream.read(readBuf);
                    if (length > 0) {
                        SerialDataBuf ComRecData = new SerialDataBuf(readBuf, length);
                        mHandler.obtainMessage(COM_MESSAGE_RECEIVED, length, 0, ComRecData).sendToTarget();
                    }
                    Thread.sleep(50);

                    //TODO 把收到的数据发给 UI
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] out) {
            try {
                mOutputStream.write(out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void cancel(){
            //TODO 关闭串口
            if(com != null){
                com.close();
                com = null;
            }
        }
    }

    public void start() {
        if(rwThread != null){
            rwThread.cancel();
            rwThread = null;
        } else {
            rwThread = new COMRWThread();
            rwThread.start();
        }
    }

    public void stop(){
        if(rwThread != null){
            rwThread.cancel();
            rwThread = null;
        }
        mState = STATE_CLOSE;
    }

    public void send(String out){
        if(mState == STATE_OPEN){
            rwThread.write(out.getBytes());
            //TODO 通知UI 发送完毕
        }
    }
}
