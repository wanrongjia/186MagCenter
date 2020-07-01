package com.example.a186magcenter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;

import com.example.a186magcenter.Fragment.ComFragment;
import com.example.a186magcenter.Fragment.USBFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentTransaction transaction =
                getSupportFragmentManager().beginTransaction();
//        BTFragment fragment = new BTFragment();
//        ComFragment comFragment = new ComFragment();
        USBFragment usbFragment = new USBFragment();
        transaction.replace(R.id.content_fragment, usbFragment);
        transaction.commit();
    }
}
