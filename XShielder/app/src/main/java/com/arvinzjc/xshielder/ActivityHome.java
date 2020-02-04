/*
 * @Description: a class for the home activity
 * @Version: 1.3.5.20200202
 * @Author: Arvin Zhao
 * @Date: 2020-01-16 13:59:45
 * @Last Editors: Arvin Zhao
 * @LastEditTime : 2020-02-02 14:32:10
 */

package com.arvinzjc.xshielder;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.appbar.AppBarLayout;
import com.mikepenz.iconics.IconicsColorInt;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.IconicsSizeDp;
import com.mikepenz.iconics.typeface.library.materialdesigniconic.MaterialDesignIconic;
import com.xuexiang.xui.utils.StatusBarUtils;
import com.xuexiang.xui.widget.textview.supertextview.SuperButton;

public class ActivityHome extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        StatusBarUtils.translucent(this); // enable the translucent status bar
        StatusBarThemeUtils.changeStatusBarTheme(this, getResources().getConfiguration());
        setContentView(R.layout.activity_home);
        ((Toolbar)findViewById(R.id.toolbarHome)).inflateMenu(R.menu.menu_settings);
        ((ActionMenuItemView)findViewById(R.id.menuSettings)).setIcon(new IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_settings)
                .color(new IconicsColorInt(getColor(R.color.primaryTextColour)))
                .size(new IconicsSizeDp(AppInitialiser.TOOLBAR_RIGHT_ICON_SIZE)));
        ((AppBarLayout)findViewById(R.id.appBarLayoutHome)).addOnOffsetChangedListener((AppBarLayout appBarLayoutHome, int verticalOffset) ->
        {
            TextView textViewAppName = findViewById(R.id.textViewAppName);

            if (Math.abs(verticalOffset) >= appBarLayoutHome.getTotalScrollRange())
                textViewAppName.setVisibility(View.VISIBLE);
            else
                textViewAppName.setVisibility(View.INVISIBLE);
        });
        ((SuperButton)findViewById(R.id.superButtonMalware)).setCompoundDrawables(new IconicsDrawable(this)
                        .icon(MaterialDesignIconic.Icon.gmi_shield_security)
                        .color(new IconicsColorInt(getColor(android.R.color.holo_green_light)))
                        .size(new IconicsSizeDp(40)),
                null,
                null,
                null);
        ((SuperButton)findViewById(R.id.superButtonWifi)).setCompoundDrawables(new IconicsDrawable(this)
                        .icon(MaterialDesignIconic.Icon.gmi_wifi_info)
                        .color(new IconicsColorInt(getColor(android.R.color.holo_blue_light)))
                        .size(new IconicsSizeDp(40)),
                null,
                null,
                null);
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
     * Respond to the click event of the super button for turning to the activity of the anti-malware shield.
     * @param view the view of the super button for turning to the activity of the anti-malware shield
     */
    public void onClickSuperButtonMalware(View view)
    {
        Intent intent = new Intent();
        intent.setClass(this, ActivityMalware.class);
        startActivity(intent);
    } // end method onClickSuperButtonMalware

    /**
     * Respond to the click event of the super button for turning to the activity of the Wi-Fi security shield.
     * @param view the view of the super button for turning to the activity of the Wi-Fi security shield
     */
    public void onClickSuperButtonWifi(View view)
    {
        Intent intent = new Intent();
        intent.setClass(this, ActivityWifi.class);
        startActivity(intent);
    } // end method onClickSuperButtonWifi
} // end class ActivityHome