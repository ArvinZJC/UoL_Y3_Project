/*
 * @Description: a class for the home activity
 * @Version: 1.4.5.20200318
 * @Author: Arvin Zhao
 * @Date: 2020-01-16 13:59:45
 * @Last Editors: Arvin Zhao
 * @LastEditTime : 2020-03-18 14:32:10
 */

package com.arvinzjc.xshielder.activities;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.ActionMenuItemView;

import com.apkfuns.logutils.LogUtils;
import com.arvinzjc.xshielder.AppInitialiser;
import com.arvinzjc.xshielder.R;
import com.arvinzjc.xshielder.databinding.ActivityHomeBinding;
import com.arvinzjc.xshielder.utils.SystemBarThemeUtils;
import com.google.android.material.appbar.AppBarLayout;
import com.mikepenz.iconics.IconicsColorInt;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.IconicsSizeDp;
import com.mikepenz.iconics.typeface.library.materialdesigniconic.MaterialDesignIconic;
import com.xuexiang.xui.utils.DeviceUtils;
import com.xuexiang.xui.utils.StatusBarUtils;

public class ActivityHome extends AppCompatActivity
{
    private ActivityHomeBinding mActivityHomeBinding;

    @SuppressLint("RestrictedApi") // suppress the warning of "menuSettings.setIcon()"
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        LogUtils.i("Enter the home activity.");

        Configuration configuration = getResources().getConfiguration();
        SystemBarThemeUtils.changeStatusBarTheme(this, configuration);
        SystemBarThemeUtils.changeNavigationBarTheme(this, configuration, getColor(R.color.translucentNavigationBarColour), true);

        mActivityHomeBinding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(mActivityHomeBinding.getRoot());
        mActivityHomeBinding.toolbarHome.inflateMenu(R.menu.menu_settings);

        ActionMenuItemView menuSettings = findViewById(R.id.menuSettings);
        menuSettings.setIcon(new IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_settings)
                .color(new IconicsColorInt(getColor(R.color.primaryTextColour)))
                .size(new IconicsSizeDp(AppInitialiser.TOOLBAR_RIGHT_ICON_SIZE)));
        menuSettings.setOnClickListener(view -> startActivity(new Intent().setClass(this, ActivitySettings.class)));

        mActivityHomeBinding.appBarLayoutHome.addOnOffsetChangedListener((AppBarLayout appBarLayoutHome, int verticalOffset) ->
        {
            // "total scroll range × 2 ÷ 3" can make the switch between the logo and the app name more smooth
            if (Math.abs(verticalOffset) >= appBarLayoutHome.getTotalScrollRange() * 2 / 3)
                mActivityHomeBinding.textViewAppName.setVisibility(View.VISIBLE);
            else
                mActivityHomeBinding.textViewAppName.setVisibility(View.INVISIBLE);
        });

        mActivityHomeBinding.constraintLayoutHome.setPadding(0, 0, 0, StatusBarUtils.getNavigationBarHeight(this)); // avoid showing content behind the navigation bar when scrolling to the end

        mActivityHomeBinding.superButtonMalware.setCompoundDrawables(new IconicsDrawable(this)
                        .icon(MaterialDesignIconic.Icon.gmi_shield_security)
                        .color(new IconicsColorInt(getColor(android.R.color.holo_green_light)))
                        .size(new IconicsSizeDp(AppInitialiser.HOME_BUTTON_ICON_SIZE)),
                null,
                null,
                null);
        mActivityHomeBinding.superButtonMalware.setOnClickListener(view -> startActivity(new Intent().setClass(this, ActivityMalware.class)));

        mActivityHomeBinding.superButtonWifi.setCompoundDrawables(new IconicsDrawable(this)
                        .icon(MaterialDesignIconic.Icon.gmi_wifi_info)
                        .color(new IconicsColorInt(getColor(android.R.color.holo_blue_light)))
                        .size(new IconicsSizeDp(AppInitialiser.HOME_BUTTON_ICON_SIZE)),
                null,
                null,
                null);
        mActivityHomeBinding.superButtonWifi.setOnClickListener(view -> startActivity(new Intent().setClass(this, ActivityWifi.class)));

        boolean isMiui = DeviceUtils.isMIUI(); // indicate if the third-party ROM used is MIUI

        mActivityHomeBinding.superButtonAppManager.setCompoundDrawables(new IconicsDrawable(this)
                        .icon(MaterialDesignIconic.Icon.gmi_android_alt)
                        .color(new IconicsColorInt(getColor(android.R.color.holo_orange_light)))
                        .size(new IconicsSizeDp(AppInitialiser.HOME_BUTTON_ICON_SIZE)),
                null,
                null,
                null);
        mActivityHomeBinding.superButtonAppManager.setOnClickListener(view ->
        {
            LogUtils.i("User chose to go to the app list screen.");

            if (isMiui)
            {
                try
                {
                    startActivity(new Intent().setClassName("com.miui.securitycenter", "com.miui.appmanager.AppManagerMainActivity"));
                }
                catch (ActivityNotFoundException e)
                {
                    LogUtils.e("Failed to go to the app list screen of MIUI. An exception occurred (" + e.getMessage() + ").");
                    LogUtils.e(e);
                    startActivity(new Intent(Settings.ACTION_APPLICATION_SETTINGS));
                } // end try...catch
            }
            else
                startActivity(new Intent(Settings.ACTION_APPLICATION_SETTINGS));
        });

        mActivityHomeBinding.superButtonSystemUpdate.setCompoundDrawables(new IconicsDrawable(this)
                        .icon(MaterialDesignIconic.Icon.gmi_smartphone_android)
                        .color(new IconicsColorInt(getColor(android.R.color.holo_purple)))
                        .size(new IconicsSizeDp(AppInitialiser.HOME_BUTTON_ICON_SIZE)),
                null,
                null,
                null);
        mActivityHomeBinding.superButtonSystemUpdate.setOnClickListener(view ->
        {
            LogUtils.i("User chose to go to the system update screen.");

            if (isMiui)
            {
                try
                {
                    startActivity(new Intent().setClassName("com.android.updater", "com.android.updater.MainActivity"));
                }
                catch (ActivityNotFoundException e)
                {
                    LogUtils.e("Failed to go to the system update screen of MIUI. An exception occurred (" + e.getMessage() + ").");
                    LogUtils.e(e);
                    goToAndroidSystemUpdateScreen();
                } // end try...catch
            }
            else
                goToAndroidSystemUpdateScreen();
        });
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

    // attempt to go to the system update screen of Android
    private void goToAndroidSystemUpdateScreen()
    {
        try
        {
            startActivity(new Intent("android.settings.SYSTEM_UPDATE_SETTINGS"));
        }
        catch (ActivityNotFoundException e)
        {
            LogUtils.e("Failed to go to the system update screen of Android. An exception occurred (" + e.getMessage() + ").");
            LogUtils.e(e);
            Toast.makeText(getApplicationContext(), R.string.home_toastSystemUpdate, Toast.LENGTH_SHORT).show(); // the application context is required to avoid any abnormal toast styles
        } // end try...catch
    } // end method goToAndroidSystemUpdateScreen
} // end class ActivityHome