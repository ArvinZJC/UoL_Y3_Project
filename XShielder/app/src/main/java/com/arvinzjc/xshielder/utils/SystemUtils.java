/*
 * @Description: utilities for supporting some actions on the system configuration
 * @Version: 1.2.0.20200409
 * @Author: Jichen Zhao
 * @Date: 2020-01-26 13:59:45
 * @Last Editors: Jichen Zhao
 * @LastEditTime : 2020-04-09 14:17:29
 */

package com.arvinzjc.xshielder.utils;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.apkfuns.logutils.LogUtils;
import com.xuexiang.xui.utils.StatusBarUtils;

public class SystemUtils
{
    private static final int TRANSLUCENT_NAVIGATION_BAR_BACKGROUND_COLOUR = 0x80000000;

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
     * @param drawBehind a flag indicating whether to draw behind the navigation bar (if true, please use a translucent colour)
     */
    public static void changeNavigationBarTheme(
            @NonNull Activity activity,
            @NonNull Configuration configuration,
            int colour,
            boolean drawBehind)
    {
        Window window = activity.getWindow();
        View windowDecorView = window.getDecorView();
        int flags = windowDecorView.getSystemUiVisibility();

        if (drawBehind)
            flags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            if ((configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO)
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR; // enable automatically changing the icon colour of the buttons on the navigation bar
        }
        else
            colour = TRANSLUCENT_NAVIGATION_BAR_BACKGROUND_COLOUR;

        windowDecorView.setSystemUiVisibility(flags);
        window.setNavigationBarColor(colour);
    } // end method changeNavigationBarTheme

    /**
     * Add the specified flag to keep the screen on, or clear the flag to allow the screen to turn off when the device is left idle.
     * @param activity the activity calling this method
     * @param isScreenKeptOn a flag indicating if the screen should be kept on
     */
    public static void keepScreenOn(@NonNull Activity activity, boolean isScreenKeptOn)
    {
        if (isScreenKeptOn)
        {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            LogUtils.i("Add the specified flag to keep the screen on.");
        }
        else
        {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            LogUtils.i("Clear the specified flag to allow the screen to turn off when the device is left idle.");
        } // end if...else
    } // end method keepScreenOn
} // end class SystemUtils