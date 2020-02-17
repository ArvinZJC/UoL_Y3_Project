/*
 * @Description: a class containing a method for changing the theme of the status bar
 * @Version: 1.0.2.20200210
 * @Author: Arvin Zhao
 * @Date: 2020-01-26 13:59:45
 * @Last Editors: Arvin Zhao
 * @LastEditTime : 2020-02-10 14:17:29
 */

package com.arvinzjc.xshielder.utils;

import android.app.Activity;
import android.content.res.Configuration;

import androidx.annotation.NonNull;

import com.xuexiang.xui.utils.StatusBarUtils;

public class StatusBarThemeUtils
{
    /**
     * Change the text colour (black/white) of the status bar according to the configuration of the dark theme.
     * @param callingActivity the activity calling this method
     * @param configuration the device configuration info
     */
    public static void changeStatusBarTheme(@NonNull Activity callingActivity, @NonNull Configuration configuration)
    {
        switch (configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK)
        {
            case Configuration.UI_MODE_NIGHT_NO:
            default:
                StatusBarUtils.setStatusBarLightMode(callingActivity); // set the text colour of the status bar to black if the dark theme is off
                break;

            case Configuration.UI_MODE_NIGHT_YES:
                StatusBarUtils.setStatusBarDarkMode(callingActivity); // set the text colour of the status bar to white if the dark theme is on
                break;
        } // end switch-case
    } // end method changeStatusBarTheme
} // end class StatusBarThemeUtils