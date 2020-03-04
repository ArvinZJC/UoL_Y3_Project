/*
 * @Description: a class for the activity of permissions
 * @Version: 1.0.0.20200304
 * @Author: Arvin Zhao
 * @Date: 2020-03-04 06:33:31
 * @Last Editors: Arvin Zhao
 * @LastEditTime: 2020-03-04 07:41:12
 */

package com.arvinzjc.xshielder.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.MenuItem;
import android.view.View;

import com.apkfuns.logutils.LogUtils;
import com.arvinzjc.xshielder.R;
import com.arvinzjc.xshielder.databinding.ActivityPermissionsBinding;
import com.arvinzjc.xshielder.utils.SystemBarThemeUtils;
import com.xuexiang.xui.utils.StatusBarUtils;

public class ActivityPermissions extends AppCompatActivity
{
    @SuppressLint("SetTextI18n") // suppress the warning of concatenating strings in the method setText()
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        LogUtils.i("Enter the activity of permissions.");

        Configuration configuration = getResources().getConfiguration();
        SystemBarThemeUtils.changeStatusBarTheme(this, configuration);
        SystemBarThemeUtils.changeNavigationBarTheme(this, configuration, getColor(R.color.translucentNavigationBarColour), true);

        ActivityPermissionsBinding activityPermissionsBinding = ActivityPermissionsBinding.inflate(getLayoutInflater());
        setContentView(activityPermissionsBinding.getRoot());
        setSupportActionBar(activityPermissionsBinding.toolbarPermissions);
        activityPermissionsBinding.nestedScrollViewPermissions.setPadding(0, 0, 0, StatusBarUtils.getNavigationBarHeight(this)); // avoid showing content behind the navigation bar when scrolling to the end

        String hereText = getString(R.string.permissions_textViewSection2_content_part2);
        SpannableString hereLinkText = new SpannableString(hereText);
        hereLinkText.setSpan(
                new ClickableSpan()
                {
                    @Override
                    public void onClick(@NonNull View widget)
                    {
                        LogUtils.i("User chose to go to the app details screen of this app.");
                        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
                    } // end method onClick
                },
                0,
                hereText.length(),
                Spanned.SPAN_MARK_MARK);
        activityPermissionsBinding.textViewSection2Content.setText(new SpannableStringBuilder()
                .append(getString(R.string.permissions_textViewSection2_content_part1))
                .append(hereLinkText)
                .append(getString(R.string.permissions_textViewSection2_content_part3)));
        activityPermissionsBinding.textViewSection2Content.setMovementMethod(LinkMovementMethod.getInstance());

        activityPermissionsBinding.textViewSection3Header.setText(getString(R.string.permissions_textViewSection3_header) + getString(R.string.app_name));
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
} // end class ActivityPermissions