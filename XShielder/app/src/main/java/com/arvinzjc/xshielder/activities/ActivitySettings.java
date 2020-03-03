/*
 * @Description: a class for the activity of settings
 * @Version: 1.0.2.20200228
 * @Author: Arvin Zhao
 * @Date: 2020-02-14 20:42:39
 * @Last Editors: Arvin Zhao
 * @LastEditTime : 2020-02-28 20:48:04
 */

package com.arvinzjc.xshielder.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;

import com.apkfuns.logutils.LogUtils;
import com.arvinzjc.xshielder.FragmentSettings;
import com.arvinzjc.xshielder.R;
import com.arvinzjc.xshielder.utils.SystemBarThemeUtils;

public class ActivitySettings extends AppCompatActivity
{
    private FragmentSettings mFragmentSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        LogUtils.i("Enter the activity of settings.");

        Configuration configuration = getResources().getConfiguration();
        SystemBarThemeUtils.changeStatusBarTheme(this, configuration);
        SystemBarThemeUtils.changeNavigationBarTheme(this, configuration, getColor(R.color.app_themeColour));

        setContentView(R.layout.activity_settings);
        setSupportActionBar(findViewById(R.id.toolbarSettings));

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // enable the Up button for this activity whose parent activity is the home activity
        else
            LogUtils.w("Failed to get this activity's action bar. Some errors might occur.");

        mFragmentSettings = new FragmentSettings();
        getSupportFragmentManager().beginTransaction().replace(R.id.nestedScrollViewSettings, mFragmentSettings).commit();
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
     * Perform some necessary tasks when destroying this activity.
     */
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        LogUtils.getLog2FileConfig().flushAsync(); // flush log cache to record logs in log files
    } // end method onDestroy

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

    /**
     * Respond to the decision on the permission request appeared on this activity.
     * @param requestCode the permission request code
     * @param permissionArray an array of the permission requested
     * @param grantResultArray the grant result for the corresponding permission
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissionArray, @NonNull int[] grantResultArray)
    {
        if (requestCode == FragmentSettings.PERMISSION_REQUEST_SETTINGS)
        {
            if (grantResultArray.length > 0 && grantResultArray[0] == PackageManager.PERMISSION_GRANTED)
            {
                LogUtils.i("Accepted to grant the permission. Viewing log files is allowed.");

                if (mFragmentSettings.mFilePickerDialogueViewingLogFiles != null)
                    mFragmentSettings.mFilePickerDialogueViewingLogFiles.show();
                else
                    LogUtils.w("Failed to show the log file picker dialogue. Some errors might occur.");
            }
            else
                LogUtils.w("Refused to grant the permission to view log files. The log file list is not shown.");
        } // end if
    } // end method onRequestPermissionsResult
} // end class ActivitySettings