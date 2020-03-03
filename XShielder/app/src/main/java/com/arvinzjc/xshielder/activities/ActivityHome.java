/*
 * @Description: a class for the home activity
 * @Version: 1.3.9.20200228
 * @Author: Arvin Zhao
 * @Date: 2020-01-16 13:59:45
 * @Last Editors: Arvin Zhao
 * @LastEditTime : 2020-02-28 14:32:10
 */

package com.arvinzjc.xshielder.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.appcompat.widget.Toolbar;

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
import com.xuexiang.xui.widget.textview.supertextview.SuperButton;

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
        SystemBarThemeUtils.changeNavigationBarTheme(this, configuration, getColor(R.color.app_themeColour));

        //setContentView(R.layout.activity_home);
        mActivityHomeBinding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(mActivityHomeBinding.getRoot());
        //((Toolbar)findViewById(R.id.toolbarHome)).inflateMenu(R.menu.menu_settings);
        mActivityHomeBinding.toolbarHome.inflateMenu(R.menu.menu_settings);

        ActionMenuItemView menuSettings = findViewById(R.id.menuSettings);
        menuSettings.setIcon(new IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_settings)
                .color(new IconicsColorInt(getColor(R.color.primaryTextColour)))
                .size(new IconicsSizeDp(AppInitialiser.TOOLBAR_RIGHT_ICON_SIZE)));
        menuSettings.setOnClickListener(view -> startActivity(new Intent().setClass(this, ActivitySettings.class)));

        mActivityHomeBinding.appBarLayoutHome.addOnOffsetChangedListener((AppBarLayout appBarLayoutHome, int verticalOffset) ->
        //((AppBarLayout)findViewById(R.id.appBarLayoutHome)).addOnOffsetChangedListener((AppBarLayout appBarLayoutHome, int verticalOffset) ->
        {
            //TextView textViewAppName = findViewById(R.id.textViewAppName);

            if (Math.abs(verticalOffset) >= appBarLayoutHome.getTotalScrollRange())
                mActivityHomeBinding.textViewAppName.setVisibility(View.VISIBLE);
            else
                mActivityHomeBinding.textViewAppName.setVisibility(View.INVISIBLE);
        });

        //SuperButton superButtonMalware = findViewById(R.id.superButtonMalware);
        mActivityHomeBinding.superButtonMalware.setCompoundDrawables(new IconicsDrawable(this)
                        .icon(MaterialDesignIconic.Icon.gmi_shield_security)
                        .color(new IconicsColorInt(getColor(android.R.color.holo_green_light)))
                        .size(new IconicsSizeDp(40)),
                null,
                null,
                null);
        mActivityHomeBinding.superButtonMalware.setOnClickListener(view -> startActivity(new Intent().setClass(this, ActivityMalware.class)));

        //SuperButton superButtonWifi = findViewById(R.id.superButtonWifi);
        mActivityHomeBinding.superButtonWifi.setCompoundDrawables(new IconicsDrawable(this)
                        .icon(MaterialDesignIconic.Icon.gmi_wifi_info)
                        .color(new IconicsColorInt(getColor(android.R.color.holo_blue_light)))
                        .size(new IconicsSizeDp(40)),
                null,
                null,
                null);
        mActivityHomeBinding.superButtonWifi.setOnClickListener(view -> startActivity(new Intent().setClass(this, ActivityWifi.class)));
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
} // end class ActivityHome