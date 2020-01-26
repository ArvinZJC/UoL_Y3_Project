/*
 * @Description: a class for the activity of the Wi-Fi security detector
 * @Version: 1.6.2.20200126
 * @Author: Arvin Zhao
 * @Date: 2020-01-19 13:59:45
 * @Last Editors: Arvin Zhao
 * @LastEditTime : 2020-01-26 16:12:07
 */

package com.arvinzjc.xshielder;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.xuexiang.xui.XUI;
import com.xuexiang.xui.utils.StatusBarUtils;
import com.xuexiang.xui.widget.grouplist.XUICommonListItemView;
import com.xuexiang.xui.widget.grouplist.XUIGroupListView;
import com.xuexiang.xui.widget.toast.XToast;

public class ActivityWifi extends AppCompatActivity
{
    private static final String TAG = "ActivityWifi";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        StatusBarUtils.translucent(this); // enable the translucent status bar
        StatusBarThemeUtils.changeStatusBarTheme(this, getBaseContext().getResources().getConfiguration());
        setContentView(R.layout.activity_wifi);
        setSupportActionBar((Toolbar)findViewById(R.id.toolbarWifi));

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // enable the Up button for this activity whose parent activity is the home activity
        else
            Log.w(TAG, "Failed to get this activity's action bar. Some errors may occur.");

        XUIGroupListView groupListViewWifiInfo = findViewById(R.id.groupListViewWifiInfo);
        XUICommonListItemView commonListItemViewSsid = groupListViewWifiInfo.createItemView(getString(R.string.wifi_info_ssid));
        XUICommonListItemView commonListItemViewSecurity = groupListViewWifiInfo.createItemView(getString(R.string.wifi_info_security));
        XUICommonListItemView commonListItemViewFrequency = groupListViewWifiInfo.createItemView(getString(R.string.wifi_info_frequency));
        XUICommonListItemView commonListItemViewSignalStrength = groupListViewWifiInfo.createItemView(getString(R.string.wifi_info_signalStrength));
        XUICommonListItemView commonListItemViewLinkSpeed = groupListViewWifiInfo.createItemView(getString(R.string.wifi_info_linkSpeed));
        XUICommonListItemView commonListItemViewIp = groupListViewWifiInfo.createItemView(getString(R.string.wifi_info_ip));
        XUICommonListItemView commonListItemViewGateway = groupListViewWifiInfo.createItemView(getString(R.string.wifi_info_gateway));
        XUICommonListItemView commonListItemViewSubnetMask = groupListViewWifiInfo.createItemView(getString(R.string.wifi_info_subnetMask));
        XUICommonListItemView commonListItemViewDns;
        String dnsLabel = getString(R.string.wifi_info_dns);

        try
        {
            WifiInfoUtils wifiInfoUtils = new WifiInfoUtils(this);

            commonListItemViewSsid.setDetailText(wifiInfoUtils.getSsid());
            commonListItemViewSecurity.setDetailText(wifiInfoUtils.getSecurity());
            commonListItemViewFrequency.setDetailText(wifiInfoUtils.getFrequency());
            commonListItemViewSignalStrength.setDetailText(wifiInfoUtils.getSignalStrength());
            commonListItemViewLinkSpeed.setDetailText(wifiInfoUtils.getLinkSpeed());
            commonListItemViewIp.setDetailText(wifiInfoUtils.getIp());
            commonListItemViewGateway.setDetailText(wifiInfoUtils.getGateway());
            commonListItemViewSubnetMask.setDetailText(wifiInfoUtils.getSubnetMask());

            String dns = wifiInfoUtils.getDnsServer();

            if (dns.contains("\n"))
                dnsLabel = getString(R.string.wifi_info_dns_plural);

            commonListItemViewDns = groupListViewWifiInfo.createItemView(dnsLabel);
            commonListItemViewDns.setDetailText(dns);
        }
        catch (NullPointerException e)
        {
            String unknownResult = getString(R.string.wifi_info_unknownResult);

            commonListItemViewSsid.setDetailText(unknownResult);
            commonListItemViewSecurity.setDetailText(unknownResult);
            commonListItemViewFrequency.setDetailText(unknownResult);
            commonListItemViewSignalStrength.setDetailText(getString(R.string.wifi_info_signalStrength_none));
            commonListItemViewLinkSpeed.setDetailText(unknownResult);
            commonListItemViewIp.setDetailText(unknownResult);
            commonListItemViewGateway.setDetailText(unknownResult);
            commonListItemViewSubnetMask.setDetailText(unknownResult);

            commonListItemViewDns = groupListViewWifiInfo.createItemView(dnsLabel);
            commonListItemViewDns.setDetailText(unknownResult);
            Log.e(TAG, "Exception occurs.", e);
        } // end try...catch

        View.OnClickListener onClickListenerWifiInfo = new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                ClipboardManager clipboardManager;

                if (view instanceof XUICommonListItemView)
                {
                    clipboardManager = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);

                    if (clipboardManager != null)
                    {
                        clipboardManager.setPrimaryClip(ClipData.newPlainText(((XUICommonListItemView) view).getText(), ((XUICommonListItemView) view).getDetailText()));
                        XToast.normal(XUI.getContext(), R.string.wifi_toast_clipboard).show();
                    }
                    else
                        Log.w(TAG, "The object instantiated from the class ClipboardManager is null.");
                } // end if
            } // end method onClick
        };

        XUIGroupListView.newSection(getBaseContext())
                .setSeparatorDrawableRes(R.drawable.common_list_item_bg_with_border_none_selector,
                        R.drawable.common_list_item_bg_with_border_none_selector,
                        R.drawable.common_list_item_bg_with_border_none_selector,
                        R.drawable.common_list_item_bg_with_border_none_selector)
                .addItemView(commonListItemViewSsid, onClickListenerWifiInfo)
                .addItemView(commonListItemViewSecurity, onClickListenerWifiInfo)
                .addItemView(commonListItemViewFrequency, onClickListenerWifiInfo)
                .addItemView(commonListItemViewSignalStrength, onClickListenerWifiInfo)
                .addItemView(commonListItemViewLinkSpeed, onClickListenerWifiInfo)
                .addItemView(commonListItemViewIp, onClickListenerWifiInfo)
                .addItemView(commonListItemViewGateway, onClickListenerWifiInfo)
                .addItemView(commonListItemViewSubnetMask, onClickListenerWifiInfo)
                .addItemView(commonListItemViewDns, onClickListenerWifiInfo)
                .addTo(groupListViewWifiInfo);
    } // end method onCreate

    /**
     * Change the text colour (black/white) of the status bar when the configuration of the dark theme is changed.
     * @param configuration the device configuration info
     */
    @Override
    public void onConfigurationChanged(@NonNull Configuration configuration)
    {
        super.onConfigurationChanged(configuration);
        StatusBarThemeUtils.changeStatusBarTheme(this, configuration);
    } // end method onConfigurationChanged

    /**
     * Respond to the selected event of the menu item on this activity's tool bar.
     * @param item the menu item selected
     * @return  true to consume it here, or false to allow normal menu processing to proceed
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            finish();
            return true;
        } // end if

        return super.onOptionsItemSelected(item);
    } // end method onOptionsItemSelected
} // end class ActivityWifi