/*
 * @Description: a necessary class for initialising the application
 * @Version: 1.1.0.20200202
 * @Author: Arvin Zhao
 * @Date: 2020-01-24 13:08:14
 * @Last Editors: Arvin Zhao
 * @LastEditTime : 2020-02-02 14:12:45
 */

package com.arvinzjc.xshielder;

import android.app.Application;

import com.xuexiang.xui.XUI;

public class AppInitialiser extends Application
{
    public static final int TOOLBAR_RIGHT_ICON_SIZE = 18; // the size (unit: dp) of the right icon in a toolbar
    public static final int DIALOG_ICON_SIZE = 24; // the size (unit: dp) of the dialog icon
    public static final int FINAL_RESULT_ICON_SIZE = 150; // the size (unit: dp) of the icon indicating the final result
    public static final int RESULT_ICON_SIZE = 15; // the size (unit: dp) of the icon indicating the result of an item in the checklist

    @Override
    public void onCreate()
    {
        super.onCreate();
        XUI.init(this); // initialise XUI
        XUI.debug(true); // enable the debug mode of XUI
    } // end method onCreate
} // end class AppInitialiser