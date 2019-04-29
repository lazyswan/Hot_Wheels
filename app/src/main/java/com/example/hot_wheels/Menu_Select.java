package com.example.hot_wheels;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class Menu_Select extends AppCompatActivity {
    private String mConnectedDeviceAddress = null;
    public static String EXTRA_ADDRESS = "device_address";
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu__select);
        Intent internt = getIntent();
        mConnectedDeviceAddress = internt.getStringExtra(EXTRA_ADDRESS);
    }

    public void communicate_click(View view) {
        Intent newintent = new Intent(Menu_Select.this, Communicate.class);
        newintent.putExtra(EXTRA_ADDRESS,mConnectedDeviceAddress);
        startActivity(newintent);
    }

    public void obstacle_avoidance_click(View view) {
        Intent newintent = new Intent(Menu_Select.this, Obstacle_Avoidance.class);
        newintent.putExtra(EXTRA_ADDRESS,mConnectedDeviceAddress);
        startActivity(newintent);
    }

    public void self_drive_click(View view) {
        Intent newintent = new Intent(Menu_Select.this, Navigate.class);
        newintent.putExtra(EXTRA_ADDRESS,mConnectedDeviceAddress);
        startActivity(newintent);
    }

    public void exit_btn(View view) {
        finish();
    }
}
