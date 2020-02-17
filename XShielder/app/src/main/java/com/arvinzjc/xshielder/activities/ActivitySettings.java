/*
 * @Description: a class for the activity of app settings
 * @Version: 1.0.0.20200214
 * @Author: Arvin Zhao
 * @Date: 2020-02-14 20:42:39
 * @Last Editors: Arvin Zhao
 * @LastEditTime : 2020-02-14 20:48:04
 */

package com.arvinzjc.xshielder.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;

import com.apkfuns.logutils.LogUtils;
import com.arvinzjc.xshielder.R;
import com.arvinzjc.xshielder.utils.StatusBarThemeUtils;
import com.xuexiang.xui.utils.StatusBarUtils;

public class ActivitySettings extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        LogUtils.i("Enter the activity of app settings.");
        StatusBarUtils.translucent(this); // enable the translucent status bar
        StatusBarThemeUtils.changeStatusBarTheme(this, getResources().getConfiguration()); // call the specified method to change the text colour (black/white) of the status bar according to the configuration of the dark theme
        setContentView(R.layout.activity_settings);
        setSupportActionBar(findViewById(R.id.toolbarSettings));

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // enable the Up button for this activity whose parent activity is the home activity
        else
            LogUtils.w("Failed to get this activity's action bar. Some errors might occur.");
    } // end method onCreate

    /**
     * Recreate the activity when the configuration of the dark theme is changed.
     * @param configuration the device configuration info
     */
    @Override
    public void onConfigurationChanged(@NonNull Configuration configuration)
    {
        super.onConfigurationChanged(configuration);
        recreate();
    } // end method onConfigurationChanged

    /**
     * Respond to the selected event of the menu item on this activity's tool bar.
     * @param menuItem the menu item selected
     * @return  true to consume it here, or false to allow normal menu processing to proceed
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem)
    {
        if (menuItem.getItemId() == android.R.id.home)
        {
            finish();
            return true;
        } // end if

        return super.onOptionsItemSelected(menuItem);
    } // end method onOptionsItemSelected
} // end class ActivitySettings