/*
 * @Description: a necessary class for applying XUI in the application
 * @Version: 1.0.0.20200124
 * @Author: Arvin Zhao
 * @Date: 2020-01-24 13:08:14
 * @Last Editors: Arvin Zhao
 * @LastEditTime : 2020-01-24 14:12:45
 */

package com.arvinzjc.xshielder;

import android.app.Application;

import com.xuexiang.xui.XUI;

public class XuiInitialiser extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();
        XUI.init(this); // initialise XUI
        XUI.debug(true); // enable the debug mode of XUI
    } // end method onCreate
} // end class XuiInitialiser