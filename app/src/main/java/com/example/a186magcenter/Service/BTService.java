package com.example.a186magcenter.Service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;
import java.util.UUID;

public class BTService {

    //建立连接 Thread

    private int mState;//当前状态
    public static final int STATE_NONE = 0;//空闲
    public static final int STATE_LISTEN = 1;//作为server正在监听
    public static final int STATE_CONNECTING = 2;//作为 client 正在发起连接
    public static final int STATE_CONNECTED = 3;// 连接成功

    public static final int BT_MSG_RECEIVED = 10;
    public static final int BT_MSG_STATE_CHANGE = 11;
    public static final int BT_MSG_STATE_CONNECTED = 12;



    private BluetoothAdapter mAdapter;
    private  ListenThread secureListenThread;
    private  ListenThread inSecureListenThread;
    private TRThread trThread;
    private ConnectThread connectThread;

    private Handler mHandler;

    public BTService(BluetoothAdapter ba, Handler h){
        mAdapter = ba;  mState = STATE_NONE;
        mHandler = h;
    }

    public void send(String out) {
        if(mState != STATE_CONNECTED)
            return;
        trThread.write(out.getBytes());
    }

    public int getState()
    { return mState;}
    public void stop() { //终止所有线程
        if(trThread != null)
        {
            trThread.cancel();
            trThread = null;
        }
        if(secureListenThread != null)
        {
            secureListenThread.cancel();
            secureListenThread = null;
        }
        if(inSecureListenThread != null)
        {
            inSecureListenThread.cancel();
            inSecureListenThread = null;
        }
        mState = STATE_NONE;
    }
    public void start() {
        if(trThread != null)
        {
            trThread.cancel();
            trThread = null;
        }
        if (secureListenThread == null) {
            secureListenThread = new ListenThread(true);
            secureListenThread.start();
        }
        if (inSecureListenThread == null) {
            inSecureListenThread = new ListenThread(false);
            inSecureListenThread.start();
        }//TODO 发消息给 UI 开始监听了
        mHandler.obtainMessage(BT_MSG_STATE_CHANGE,mState,0).sendToTarget();
    }
    private class ConnectThread extends Thread
    {
        private BluetoothSocket socket = null;

        public ConnectThread (boolean secure, BluetoothDevice dev) {
            try{
                if(secure)
                    socket = dev.createRfcommSocketToServiceRecord(UUID_SECURE);
                else
                    socket = dev.createInsecureRfcommSocketToServiceRecord(UUID_INSECURE);
            } catch(IOException e) {e.printStackTrace();}
            mState = STATE_CONNECTING;
        }

        @Override
        public void run() {
            mAdapter.cancelDiscovery();
            try {
                socket.connect();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                //TODO 处理连接失败
                connectError();
            }
            connectThread = null;
            startTRThread(socket);
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void connect(boolean s,
                        BluetoothDevice dev)
    {
        if(connectThread != null)
        {
            connectThread.cancel();
            connectThread = null;
        }
        if(trThread != null)
        {
            trThread.cancel();
            trThread = null;
        }
        connectThread = new ConnectThread(s, dev);
        connectThread.start();
        //TODO 通知 UI 连接建立
        mHandler.obtainMessage(BT_MSG_STATE_CHANGE,mState,0).sendToTarget();
    }

    private void connectError()
    {
        mState = STATE_NONE;
        mHandler.obtainMessage(BT_MSG_STATE_CHANGE,mState,0).sendToTarget();
        this.start();
    }

    private final UUID UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private final UUID UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    //监听线程
    private class ListenThread extends Thread {
        BluetoothServerSocket serverSocket = null;



        public ListenThread(boolean secure) {
            try{
                if(secure){ //初始化加密连接
                    serverSocket = mAdapter
                            .listenUsingRfcommWithServiceRecord("s", UUID_SECURE);}
                else{//初始化  非加密连接
                    serverSocket = mAdapter
                            .listenUsingInsecureRfcommWithServiceRecord("u", UUID_INSECURE);}
            }catch (IOException e){e.printStackTrace();}  mState = STATE_LISTEN;
        }

        @Override
        public void run() {
            super.run();
            BluetoothSocket socket = null;
            while(mState != STATE_CONNECTED){
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(socket != null)
                {//如果确认了对方的连接
                    switch (mState) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            //TODO 开启收发线程
                            startTRThread(socket);
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            try {
                                socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                    }

                }
            }
        }

        public void cancel(){
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class TRThread extends Thread {
        private BluetoothSocket mSocket;

        public TRThread (BluetoothSocket s) {
            mSocket = s;
            mState = STATE_CONNECTED;
        }

        @Override
        public void run() {
            super.run();
            byte[] buf = new byte[1024*10];
            while(mState == STATE_CONNECTED)
            {
                try {
                    int len = mSocket.getInputStream().read(buf);
                    if(len>0){  //TODO 送到 UI 显示出来
                        mHandler.obtainMessage(BT_MSG_RECEIVED,
                                len, 0, buf);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    //TODO 通知 UI 连接有问题
                }
            }
        }

        public void write(byte[] buf) {
            try{
                mSocket.getOutputStream().write(buf);
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void cancel() {
            try{
                mSocket.close();
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
    }





    private void startTRThread(BluetoothSocket socket) {
        //TODO 关闭 Listening 线程
        //TODO 关闭已有的连接线程     开启新的收发线程

        if(secureListenThread != null){
            secureListenThread.cancel();
            secureListenThread = null;
        }
        if(inSecureListenThread != null){
            inSecureListenThread.cancel();
            inSecureListenThread = null;
        }
        trThread = new TRThread(socket);
        trThread.start(); //TODO 发消息给 UI

        String devName = socket.getRemoteDevice().getName();
        mHandler.obtainMessage(BT_MSG_STATE_CONNECTED,devName.getBytes().length,0,devName.getBytes()).sendToTarget();
        mHandler.obtainMessage(BT_MSG_STATE_CHANGE,mState,0).sendToTarget();
    }
}
