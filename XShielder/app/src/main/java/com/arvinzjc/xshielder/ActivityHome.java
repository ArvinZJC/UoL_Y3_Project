/*
 * @Description: a class for the home activity
 * @Version: 1.1.1.20200126
 * @Author: Arvin Zhao
 * @Date: 2020-01-16 13:59:45
 * @Last Editors: Arvin Zhao
 * @LastEditTime : 2020-01-26 14:20:35
 */

package com.arvinzjc.xshielder;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.xuexiang.xui.utils.StatusBarUtils;

public class ActivityHome extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        StatusBarUtils.translucent(this); // enable the translucent status bar
        StatusBarThemeUtils.changeStatusBarTheme(this, getBaseContext().getResources().getConfiguration());
        setContentView(R.layout.activity_home);
    } // end method onCreate

    /**
     * Change the text colour (black/white) of the status bar when the configuration of the dark theme is changed.
     * @param configuration the device configuration info
     */
    @Override
    public void onConfigurationChanged(@NonNull Configuration configuration)
    {
        super.onConfigurationChanged(configuration);
        StatusBarThemeUtils.changeStatusBarTheme(this, configuration);
    } // end method onConfigurationChanged

    /**
     * Respond to the click event of the round button for turning to the activity of the Wi-Fi security detector.
     * @param view the view of the round button for turning to the activity of the Wi-Fi security detector
     */
    public void onClickRoundButtonWifi(View view)
    {
        Intent intent = new Intent();
        intent.setClass(this, ActivityWifi.class);
        startActivity(intent);
    } // end method onClickRoundButtonWifi
} // end class ActivityHome