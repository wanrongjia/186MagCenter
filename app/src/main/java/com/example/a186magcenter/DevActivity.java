package com.example.a186magcenter;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;

public class DevActivity extends AppCompatActivity {

    private ArrayAdapter<String> mpairedAdapter;
    private BluetoothAdapter mAdapter;

    private AdapterView.OnItemClickListener clickListener
        = new AdapterView.OnItemClickListener(){
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //TODO 设备地址 和 设备名称 传给 Fragment
            String info = ((TextView)view).getText().toString();
            String addr = info.split("\n")[1];
            Intent intent = new Intent();
            intent.putExtra("ADDR",addr);
            setResult(Activity.RESULT_OK,intent);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dev);
        setResult(Activity.RESULT_CANCELED);

        mpairedAdapter = new ArrayAdapter<>(this,
                R.layout.message);
        ListView pairedListView = findViewById(
                R.id.paired_list);
        pairedListView.setAdapter(mpairedAdapter);
        pairedListView.setOnItemClickListener(clickListener);
        mAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> pairedDevs = mAdapter.getBondedDevices();
        if (pairedDevs.size()>0){
            for (BluetoothDevice d : pairedDevs){
                mpairedAdapter.add(d.getName() + "\n"
                +d.getAddress());
            }
        }else{
            mpairedAdapter.add("没有已配对的设备");
        }
    }
}
