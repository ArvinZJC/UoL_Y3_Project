/*
 * @Description: a class for the activity of the Wi-Fi security shield
 * @Version: 1.8.3.20200202
 * @Author: Arvin Zhao
 * @Date: 2020-01-19 13:59:45
 * @Last Editors: Arvin Zhao
 * @LastEditTime : 2020-02-02 23:52:07
 */

package com.arvinzjc.xshielder;

import android.Manifest;
import android.accounts.NetworkErrorException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.mikepenz.iconics.IconicsColorInt;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.IconicsSizeDp;
import com.mikepenz.iconics.typeface.library.ionicons.Ionicons;
import com.mikepenz.iconics.view.IconicsImageView;
import com.xuexiang.xui.utils.StatusBarUtils;
import com.xuexiang.xui.utils.ViewUtils;
import com.xuexiang.xui.widget.button.roundbutton.RoundButton;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;
import com.xuexiang.xui.widget.grouplist.XUICommonListItemView;
import com.xuexiang.xui.widget.grouplist.XUIGroupListView;
import com.xuexiang.xui.widget.layout.XUILinearLayout;

public class ActivityWifi extends AppCompatActivity
{
    private static final String TAG = ActivityWifi.class.getSimpleName(); // the tag of the log from this class
    private static final int PERMISSION_REQUEST_WIFI = 0; // the request code of the permission(s) to enable detecting Wi-Fi security

    private int mAnimationDuration;
    private IconicsImageView mImageViewFinalResult;
    private TextView mTextViewRationale;
    private XUILinearLayout mLinearLayoutWifiInfo;
    private XUIGroupListView mGroupListViewWifiInfo;
    private XUICommonListItemView mCommonListItemViewSecurityResult;
    private XUICommonListItemView mCommonListItemViewSsid;
    private XUICommonListItemView mCommonListItemViewSecurity;
    private XUICommonListItemView mCommonListItemViewFrequency;
    private XUICommonListItemView mCommonListItemViewSignalStrength;
    private XUICommonListItemView mCommonListItemViewLinkSpeed;
    private XUICommonListItemView mCommonListItemViewMac;
    private XUICommonListItemView mCommonListItemViewIp;
    private XUICommonListItemView mCommonListItemViewGateway;
    private XUICommonListItemView mCommonListItemViewSubnetMask;
    private XUICommonListItemView mCommonListItemViewDns;
    private RoundButton mRoundButtonAction;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        StatusBarUtils.translucent(this); // enable the translucent status bar
        StatusBarThemeUtils.changeStatusBarTheme(this, getResources().getConfiguration()); // call the specified method to change the text colour (black/white) of the status bar according to the configuration of the dark theme
        setContentView(R.layout.activity_wifi);
        setSupportActionBar(findViewById(R.id.toolbarWifi));

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // enable the Up button for this activity whose parent activity is the home activity
        else
            Log.w(TAG, "Failed to get this activity's action bar. Some errors might occur.");

        mAnimationDuration = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        mImageViewFinalResult = findViewById(R.id.imageViewFinalResult);
        mTextViewRationale = findViewById(R.id.textViewRationale);
        mLinearLayoutWifiInfo = findViewById(R.id.linearLayoutWifiInfo);
        mGroupListViewWifiInfo = findViewById(R.id.groupListViewWifiInfo);
        mRoundButtonAction = findViewById(R.id.roundButtonAction);

        mImageViewFinalResult.setVisibility(View.INVISIBLE);
        mTextViewRationale.setVisibility(View.INVISIBLE);
        mLinearLayoutWifiInfo.setVisibility(View.INVISIBLE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            String[] permissionList;

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                permissionList = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
            else
                permissionList = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION};

            new MaterialDialog.Builder(this)
                    .icon(new IconicsDrawable(this)
                            .icon(Ionicons.Icon.ion_information_circled)
                            .color(new IconicsColorInt(getColor(R.color.colourWarning)))
                            .size(new IconicsSizeDp(AppInitialiser.DIALOG_ICON_SIZE)))
                    .title(R.string.wifi_dialogPermissionWarning_title)
                    .content(R.string.wifi_dialogPermissionWarning_content)
                    .positiveText(R.string.wifi_dialogPermissionWarning_positiveText)
                    .positiveColor(getColor(R.color.button_backgroundColour))
                    .cancelable(false)
                    .dismissListener(permissionAlert ->
                            ActivityCompat.requestPermissions(this, permissionList, PERMISSION_REQUEST_WIFI))
                    .show();
        }
        else
            (new Handler()).postDelayed(() -> getWifiScanResults(true), mAnimationDuration);
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

    /**
     * Respond to the decision on the permission request to enable detecting Wi-Fi security.
     * @param requestCode the permission request code
     * @param permissions the permission(s) requested
     * @param grantResults the decision
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == PERMISSION_REQUEST_WIFI)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                getWifiScanResults(true);
            else
            {
                Log.i(TAG, "Refused to grant the permission(s) to enable detecting Wi-Fi security. Return to the specified activity.");
                finish();
            } // end if...else
        } // end if
    } // end method onRequestPermissionsResult

    /**
     * Respond to the click event of the round button for the action after scanning Wi-Fi security.
     * @param view the view of the round button for the action after scanning Wi-Fi security
     */
    public void onClickRoundButtonAction(View view)
    {
        if (mRoundButtonAction.getText().equals(getString(R.string.wifi_roundButtonAction_failed)))
        {
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            mRoundButtonAction.setText(R.string.wifi_roundButtonAction_normal);
        }
        else
            getWifiScanResults(false);
    } // end method onClickRoundButtonAction

    // get and display the scan results
    private void getWifiScanResults(boolean isFirstScan)
    {
        if (isFirstScan)
        {
            mCommonListItemViewSecurityResult = mGroupListViewWifiInfo.createItemView(getString(R.string.wifi_info_securityResult));
            mCommonListItemViewSsid = mGroupListViewWifiInfo.createItemView(getString(R.string.wifi_info_ssid));
            mCommonListItemViewSecurity = mGroupListViewWifiInfo.createItemView(getString(R.string.wifi_info_security));
            mCommonListItemViewFrequency = mGroupListViewWifiInfo.createItemView(getString(R.string.wifi_info_frequency));
            mCommonListItemViewSignalStrength = mGroupListViewWifiInfo.createItemView(getString(R.string.wifi_info_signalStrength));
            mCommonListItemViewLinkSpeed = mGroupListViewWifiInfo.createItemView(getString(R.string.wifi_info_linkSpeed));
            mCommonListItemViewMac = mGroupListViewWifiInfo.createItemView(getString(R.string.wifi_info_mac));
            mCommonListItemViewIp = mGroupListViewWifiInfo.createItemView(getString(R.string.wifi_info_ip));
            mCommonListItemViewGateway = mGroupListViewWifiInfo.createItemView(getString(R.string.wifi_info_gateway));
            mCommonListItemViewSubnetMask = mGroupListViewWifiInfo.createItemView(getString(R.string.wifi_info_subnetMask));
            mCommonListItemViewDns = mGroupListViewWifiInfo.createItemView(getString(R.string.wifi_info_dns));
        } // end if

        IconicsImageView imageViewResult = new IconicsImageView(this);
        String unknownResult = getString(R.string.wifi_info_unknownResult);
        String noSignalStrenghth = getString(R.string.wifi_info_signalStrength_none);

        mCommonListItemViewSecurityResult.setAccessoryType(XUICommonListItemView.ACCESSORY_TYPE_CUSTOM);

        try
        {
            WifiInfoUtils wifiInfoUtils = new WifiInfoUtils(this);
            String securityLabel = wifiInfoUtils.getSecurity()[1]; // the representation indicating if the security type is secured

            if (securityLabel.equals(WifiInfoUtils.SECURED_SECURITY_TYPE))
            {
                mImageViewFinalResult.setIcon(new IconicsDrawable(this)
                        .icon(Ionicons.Icon.ion_checkmark_circled)
                        .color(new IconicsColorInt(getColor(R.color.colourPass)))
                        .size(new IconicsSizeDp(AppInitialiser.FINAL_RESULT_ICON_SIZE)));
                mTextViewRationale.setText(R.string.wifi_textViewRationale_pass);
                imageViewResult.setIcon(new IconicsDrawable(this)
                        .icon(Ionicons.Icon.ion_checkmark_round)
                        .color(new IconicsColorInt(getColor(R.color.colourPass)))
                        .size(new IconicsSizeDp(AppInitialiser.RESULT_ICON_SIZE)));
                mCommonListItemViewSecurityResult.addAccessoryCustomView(imageViewResult);
                mRoundButtonAction.setText(R.string.wifi_roundButtonAction_normal);
            }
            else if (securityLabel.equals(WifiInfoUtils.UNSECURED_SECURITY_TYPE))
            {
                mImageViewFinalResult.setIcon(new IconicsDrawable(this)
                        .icon(Ionicons.Icon.ion_android_alert)
                        .color(new IconicsColorInt(Color.RED))
                        .size(new IconicsSizeDp(AppInitialiser.FINAL_RESULT_ICON_SIZE)));
                mTextViewRationale.setText(R.string.wifi_textViewRationale_failed);
                imageViewResult.setIcon(new IconicsDrawable(this)
                        .icon(Ionicons.Icon.ion_alert)
                        .color(new IconicsColorInt(Color.RED))
                        .size(new IconicsSizeDp(AppInitialiser.RESULT_ICON_SIZE)));
                mCommonListItemViewSecurityResult.addAccessoryCustomView(imageViewResult);
                mRoundButtonAction.setText(R.string.wifi_roundButtonAction_failed);
            }
            else
            {
                mImageViewFinalResult.setIcon(new IconicsDrawable(this)
                        .icon(Ionicons.Icon.ion_ios_help)
                        .color(new IconicsColorInt(getColor(R.color.colourWarning)))
                        .size(new IconicsSizeDp(AppInitialiser.FINAL_RESULT_ICON_SIZE)));
                mTextViewRationale.setText(R.string.wifi_textViewRationale_unknown_appError);
                imageViewResult.setIcon(new IconicsDrawable(this)
                        .icon(Ionicons.Icon.ion_help)
                        .color(new IconicsColorInt(getColor(R.color.colourWarning)))
                        .size(new IconicsSizeDp(AppInitialiser.RESULT_ICON_SIZE)));
                mCommonListItemViewSecurityResult.addAccessoryCustomView(imageViewResult);
                mRoundButtonAction.setText(R.string.wifi_roundButtonAction_unknown);
            } // end nested if...else

            mCommonListItemViewSsid.setDetailText(wifiInfoUtils.getSsid());
            mCommonListItemViewSecurity.setDetailText(wifiInfoUtils.getSecurity()[0]);
            mCommonListItemViewFrequency.setDetailText(wifiInfoUtils.getFrequency());
            mCommonListItemViewSignalStrength.setDetailText(wifiInfoUtils.getSignalStrength());
            mCommonListItemViewLinkSpeed.setDetailText(wifiInfoUtils.getLinkSpeed());
            mCommonListItemViewMac.setDetailText(wifiInfoUtils.getMac());
            mCommonListItemViewIp.setDetailText(wifiInfoUtils.getIpAndSubnetMask()[0]);
            mCommonListItemViewGateway.setDetailText(wifiInfoUtils.getGateway());
            mCommonListItemViewSubnetMask.setDetailText(wifiInfoUtils.getIpAndSubnetMask()[1]);
            mCommonListItemViewDns.setDetailText(wifiInfoUtils.getDns());
        }
        catch (Exception e)
        {
            Log.e(TAG, "Exception occurred.", e);
            mImageViewFinalResult.setIcon(new IconicsDrawable(this)
                    .icon(Ionicons.Icon.ion_ios_help)
                    .color(new IconicsColorInt(getColor(R.color.colourWarning)))
                    .size(new IconicsSizeDp(AppInitialiser.FINAL_RESULT_ICON_SIZE)));

            if (e instanceof NetworkErrorException)
                mTextViewRationale.setText(R.string.wifi_textViewRationale_unknown_connectivity);
            else
                mTextViewRationale.setText(R.string.wifi_textViewRationale_unknown_appError);

            imageViewResult.setIcon(new IconicsDrawable(this)
                    .icon(Ionicons.Icon.ion_help)
                    .color(new IconicsColorInt(getColor(R.color.colourWarning)))
                    .size(new IconicsSizeDp(AppInitialiser.RESULT_ICON_SIZE)));
            mCommonListItemViewSecurityResult.addAccessoryCustomView(imageViewResult);
            mRoundButtonAction.setText(R.string.wifi_roundButtonAction_unknown);
            mCommonListItemViewSsid.setDetailText(unknownResult);
            mCommonListItemViewSecurity.setDetailText(unknownResult);
            mCommonListItemViewFrequency.setDetailText(unknownResult);
            mCommonListItemViewSignalStrength.setDetailText(noSignalStrenghth);
            mCommonListItemViewLinkSpeed.setDetailText(unknownResult);
            mCommonListItemViewMac.setDetailText(unknownResult);
            mCommonListItemViewIp.setDetailText(unknownResult);
            mCommonListItemViewGateway.setDetailText(unknownResult);
            mCommonListItemViewSubnetMask.setDetailText(unknownResult);
            mCommonListItemViewDns.setDetailText(unknownResult);
        } // end try...catch

        View.OnLongClickListener onLongClickListenerWifiInfo = (View view) ->
        {
            ClipboardManager clipboardManager;

            if (view instanceof XUICommonListItemView)
            {
                clipboardManager = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);

                if (clipboardManager != null)
                {
                    String wifiCopied = ((XUICommonListItemView) view).getDetailText().toString();

                    if (!(wifiCopied.equals(unknownResult) || wifiCopied.equals(noSignalStrenghth)))
                    {
                        clipboardManager.setPrimaryClip(ClipData.newPlainText(((XUICommonListItemView) view).getText(), wifiCopied));
                        Toast.makeText(getApplicationContext(), R.string.wifi_toastCopiedToClipboard, Toast.LENGTH_SHORT).show(); // the application context is required to avoid any abnormal toast styles
                        return true;
                    } // end if
                }
                else
                    Log.w(TAG, "Null object instantiated from the class " + ClipboardManager.class.getSimpleName() + ".");
            } // end if

            return false;
        };

        if (isFirstScan)
        {
            XUIGroupListView.newSection(this)
                    .setTitle(getString(R.string.wifi_info_checklist_title))
                    .setDescription(" ") // this description is set so as to leave space between 2 sections
                    .setSeparatorDrawableRes(R.drawable.common_list_item_bg_with_border_none_selector,
                            R.drawable.common_list_item_bg_with_border_none_selector,
                            R.drawable.common_list_item_bg_with_border_none_selector,
                            R.drawable.common_list_item_bg_with_border_none_selector)
                    .addItemView(mCommonListItemViewSecurityResult, null)
                    .addTo(mGroupListViewWifiInfo);
            XUIGroupListView.newSection(this)
                    .setTitle(getString(R.string.wifi_info_details_title))
                    .setSeparatorDrawableRes(R.drawable.common_list_item_bg_with_border_none_selector,
                            R.drawable.common_list_item_bg_with_border_none_selector,
                            R.drawable.common_list_item_bg_with_border_none_selector,
                            R.drawable.common_list_item_bg_with_border_none_selector)
                    .addItemView(mCommonListItemViewSsid, null, onLongClickListenerWifiInfo)
                    .addItemView(mCommonListItemViewSecurity, null, onLongClickListenerWifiInfo)
                    .addItemView(mCommonListItemViewFrequency, null, onLongClickListenerWifiInfo)
                    .addItemView(mCommonListItemViewSignalStrength, null, onLongClickListenerWifiInfo)
                    .addItemView(mCommonListItemViewLinkSpeed, null, onLongClickListenerWifiInfo)
                    .addItemView(mCommonListItemViewMac, null, onLongClickListenerWifiInfo)
                    .addItemView(mCommonListItemViewIp, null, onLongClickListenerWifiInfo)
                    .addItemView(mCommonListItemViewGateway, null, onLongClickListenerWifiInfo)
                    .addItemView(mCommonListItemViewSubnetMask, null, onLongClickListenerWifiInfo)
                    .addItemView(mCommonListItemViewDns, null, onLongClickListenerWifiInfo)
                    .addTo(mGroupListViewWifiInfo);
        } // end if

        ViewUtils.fadeIn(mImageViewFinalResult, mAnimationDuration, null);
        ViewUtils.fadeIn(mTextViewRationale, mAnimationDuration, null);
        ViewUtils.slideIn(mLinearLayoutWifiInfo, mAnimationDuration, null, ViewUtils.Direction.BOTTOM_TO_TOP);
    } // end method getWifiScanResults
} // end class ActivityWifi