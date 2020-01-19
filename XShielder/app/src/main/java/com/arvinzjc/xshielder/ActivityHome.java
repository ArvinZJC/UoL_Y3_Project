package com.arvinzjc.xshielder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class ActivityHome extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
    } // end method onCreate

    /**
     *
     * @param view
     */
    public void onClickButtonWifi(View view)
    {
        Intent intent = new Intent();
        intent.setClass(this, ActivityWifi.class);
        startActivity(intent);
    } // end method onClickButtonWifi
} // end class ActivityHome