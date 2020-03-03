/*
 * @Description: utilities for supporting some actions on the theme of the status bar or the navigation bar
 * @Version: 1.1.0.20200228
 * @Author: Arvin Zhao
 * @Date: 2020-01-26 13:59:45
 * @Last Editors: Arvin Zhao
 * @LastEditTime : 2020-02-28 14:17:29
 */

package com.arvinzjc.xshielder.utils;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Build;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;

import com.xuexiang.xui.utils.StatusBarUtils;

public class SystemBarThemeUtils
{
    private static final int TRANSLUCENT_NAVIGATION_BAR_BACKGROUND_COLOUR = 0x40000000;

    /**
     * Change the theme of the status bar according to the configuration of the dark theme.
     * @param activity the activity calling this method
     * @param configuration the device configuration info
     */
    public static void changeStatusBarTheme(@NonNull Activity activity, @NonNull Configuration configuration)
    {
        StatusBarUtils.translucent(activity); // make the status bar transparent (not translucent because of the minimum SDK version is 23 - Android M)

        switch (configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK)
        {
            case Configuration.UI_MODE_NIGHT_NO:
            default:
                StatusBarUtils.setStatusBarLightMode(activity); // set the text colour of the status bar to black if the dark theme is off
                break;

            case Configuration.UI_MODE_NIGHT_YES:
                StatusBarUtils.setStatusBarDarkMode(activity); // set the text colour of the status bar to white if the dark theme is on
                break;
        } // end switch-case
    } // end method changeStatusBarTheme

    /**
     * Change the theme of the navigation bar according to the Android version and the configuration of the dark theme.
     * @param activity the activity calling this method
     * @param configuration the device configuration info
     * @param colour the background colour of the navigation bar (please use a bright colour when the dark theme is off, while use a dark colour when the dark theme
     *               is on; using the same colour as the one used by a nearby view is suggested so as to look like a transparent navigation bar)
     */
    public static void changeNavigationBarTheme(
            @NonNull Activity activity,
            @NonNull Configuration configuration,
            int colour)
    {
        Window window = activity.getWindow();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            if ((configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO)
            {
                View windowDecorView = window.getDecorView();
                int flags = windowDecorView.getSystemUiVisibility();

                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR; // enable automatically changing the icon colour of the buttons on the navigation bar
                windowDecorView.setSystemUiVisibility(flags);
            } // end if

            window.setNavigationBarColor(colour);
        }
        else
            window.setNavigationBarColor(TRANSLUCENT_NAVIGATION_BAR_BACKGROUND_COLOUR); // enable the translucent navigation bar
    } // end method changeNavigationBarTheme
} // end class SystemBarThemeUtils