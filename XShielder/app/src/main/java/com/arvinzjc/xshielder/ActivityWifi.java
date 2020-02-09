/*
 * @Description: a class for the activity of the Wi-Fi security shield
 * @Version: 1.9.1.20200208
 * @Author: Arvin Zhao
 * @Date: 2020-01-19 13:59:45
 * @Last Editors: Arvin Zhao
 * @LastEditTime : 2020-02-08 23:52:07
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
import android.os.Message;
import android.provider.Settings;
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
import com.apkfuns.logutils.LogUtils;
import com.xuexiang.xui.utils.StatusBarUtils;
import com.xuexiang.xui.utils.ViewUtils;
import com.xuexiang.xui.widget.button.roundbutton.RoundButton;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;
import com.xuexiang.xui.widget.grouplist.XUICommonListItemView;
import com.xuexiang.xui.widget.grouplist.XUIGroupListView;
import com.xuexiang.xui.widget.layout.XUILinearLayout;
import com.xuexiang.xui.widget.progress.CircleProgressView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ActivityWifi extends AppCompatActivity
{
    private enum WifiResults
    {
        PASS,
        FAIL,
        UNKNOWN
    } // end enum WifiResults

    /*
     * 1. the info of the connected Wi-Fi;
     * 2. secured security type;
     * 3. connectivity;
     * 4. secured DNS;
     * 5. secured SSL
     */
    private static final int SCAN_TASK_COUNT = 5;

    /*
     * 1. secured DNS;
     * 2. secured SSL
     */
    private static final int THREAD_TASK_COUNT = 2;

    private int mCompletedScanTaskCount = 0, mAnimationDuration;
    private boolean mIsSecuredDns, mIsSecuredSsl;
    private CircleProgressView mCircleProgressViewWifi;
    private IconicsImageView mImageViewWifi;
    private TextView mTextViewWifiRationale;
    private XUILinearLayout mLinearLayoutWifiResults;
    private XUIGroupListView mGroupListViewWifiResults;
    private XUICommonListItemView mCommonListItemViewChecklistSecurityType;
    private XUICommonListItemView mCommonListItemViewChecklistConnectivity;
    private XUICommonListItemView mCommonListItemViewChecklistDns;
    private XUICommonListItemView mCommonListItemViewChecklistSsl;
    private XUICommonListItemView mCommonListItemViewInfoSsid;
    private XUICommonListItemView mCommonListItemViewInfoSecurity;
    private XUICommonListItemView mCommonListItemViewInfoFrequency;
    private XUICommonListItemView mCommonListItemViewInfoSignalStrength;
    private XUICommonListItemView mCommonListItemViewInfoLinkSpeed;
    private XUICommonListItemView mCommonListItemViewInfoBssid;
    private XUICommonListItemView mCommonListItemViewInfoIp;
    private XUICommonListItemView mCommonListItemViewInfoGateway;
    private XUICommonListItemView mCommonListItemViewInfoSubnetMask;
    private XUICommonListItemView mCommonListItemViewInfoDns;
    private RoundButton mRoundButtonWifiAction;
    private Handler mHandlerWifi;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        LogUtils.i("Enter the activity of the Wi-Fi security shield.");
        StatusBarUtils.translucent(this); // enable the translucent status bar
        StatusBarThemeUtils.changeStatusBarTheme(this, getResources().getConfiguration()); // call the specified method to change the text colour (black/white) of the status bar according to the configuration of the dark theme
        setContentView(R.layout.activity_wifi);
        setSupportActionBar(findViewById(R.id.toolbarWifi));

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // enable the Up button for this activity whose parent activity is the home activity
        else
            LogUtils.w("Failed to get this activity's action bar. Some errors might occur.");

        mAnimationDuration = getResources().getInteger(android.R.integer.config_mediumAnimTime); // 400 milliseconds
        mCircleProgressViewWifi = findViewById(R.id.circleProgressViewWifi);
        mImageViewWifi = findViewById(R.id.imageViewWifi);
        mTextViewWifiRationale = findViewById(R.id.textViewWifiRationale);
        mLinearLayoutWifiResults = findViewById(R.id.linearLayoutWifiResults);
        mGroupListViewWifiResults = findViewById(R.id.groupListViewWifiResults);
        mRoundButtonWifiAction = findViewById(R.id.roundButtonWifiAction);
        mHandlerWifi = new Handler((Message message) ->
            {
                if (!Thread.currentThread().isInterrupted())
                {
                    if (message.what == 1)
                    {
                        mCircleProgressViewWifi.setProgress(++mCompletedScanTaskCount * 100 / SCAN_TASK_COUNT);

                        if (mCompletedScanTaskCount == SCAN_TASK_COUNT)
                        {
                            playAnimation();
                        } // end if
                    }
                    else if (message.what == 0)
                    {
                        mCompletedScanTaskCount = 0;
                        mCircleProgressViewWifi.setProgress(0);
                    }
                    else
                    {
                        mCircleProgressViewWifi.setProgress(100);
                        //mCircleProgressViewWifi.stopProgressAnimation();
                        playAnimation();
                    } // end if...else

                    return true;
                } // end if

                return false;
            });

        // hide all widgets except the toolbar and the circle progress view
        mImageViewWifi.setVisibility(View.INVISIBLE);
        mTextViewWifiRationale.setVisibility(View.INVISIBLE);
        mLinearLayoutWifiResults.setVisibility(View.INVISIBLE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            LogUtils.i("The permission(s) to enable detecting Wi-Fi security is/are not granted. A permission warning dialog from the app appears.");

            String[] permissionArray;

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                permissionArray = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
            else
                permissionArray = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION};

            new MaterialDialog.Builder(this)
                    .icon(new IconicsDrawable(this)
                            .icon(Ionicons.Icon.ion_information_circled)
                            .color(new IconicsColorInt(getColor(R.color.colourWarning)))
                            .size(new IconicsSizeDp(AppInitialiser.DIALOG_ICON_SIZE)))
                    .title(R.string.dialogPermissionWarning_title)
                    .content(R.string.wifi_dialogPermissionWarning_content)
                    .positiveText(R.string.dialogPermissionWarning_positiveText)
                    .positiveColor(getColor(R.color.button_backgroundColour))
                    .cancelable(false)
                    .dismissListener(permissionAlert ->
                            ActivityCompat.requestPermissions(this, permissionArray, AppInitialiser.PERMISSION_REQUEST_WIFI))
                    .show();
        }
        else
            (new Handler()).postDelayed(() -> getWifiResults(true), mAnimationDuration);
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

    /**
     * Respond to the decision on the permission request appeared on this activity.
     * @param requestCode the permission request code
     * @param permissionArray an array of the permission(s) requested
     * @param grantResultArray the grant result(s) for the corresponding permission(s)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissionArray, @NonNull int[] grantResultArray)
    {
        if (requestCode == AppInitialiser.PERMISSION_REQUEST_WIFI)
        {
            if (grantResultArray.length > 0 && grantResultArray[0] == PackageManager.PERMISSION_GRANTED)
            {
                LogUtils.i("Accepted to grant the permission(s). Start to detect Wi-Fi security.");
                getWifiResults(true);
            }
            else
            {
                LogUtils.w("Refused to grant the permission(s) to enable detecting Wi-Fi security. Return to the specified activity.");
                finish();
            } // end if...else
        } // end if
    } // end method onRequestPermissionsResult

    /**
     * Respond to the click event of the round button for the action after scanning Wi-Fi security.
     * @param view the view of the round button for the action after scanning Wi-Fi security
     */
    public void onClickRoundButtonWifiAction(View view)
    {
        if (mRoundButtonWifiAction.getText().equals(getString(R.string.wifi_roundButtonAction_fail)))
        {
            LogUtils.i("User chose to go to the Wi-Fi settings page to configure Wi-Fi.");
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            mRoundButtonWifiAction.setText(R.string.wifi_roundButtonAction_normal);
        }
        else
        {
            LogUtils.i("User chose to detect Wi-Fi security again.");
            mCircleProgressViewWifi.setProgress(0);
            ViewUtils.fadeOut(mImageViewWifi, mAnimationDuration, null);
            ViewUtils.fadeOut(mTextViewWifiRationale, mAnimationDuration, null);
            ViewUtils.slideOut(mLinearLayoutWifiResults, mAnimationDuration, null, ViewUtils.Direction.TOP_TO_BOTTOM);
            (new Handler()).postDelayed(() -> ViewUtils.fadeIn(mCircleProgressViewWifi, mAnimationDuration, null), mAnimationDuration / 2);
            (new Handler()).postDelayed(() -> getWifiResults(false), mAnimationDuration);
        } // end if...else
    } // end method onClickRoundButtonWifiAction

    // get and display the scan results of Wi-Fi security
    private void getWifiResults(boolean isFirstScan)
    {
        if (isFirstScan)
        {
            mCommonListItemViewChecklistSecurityType = mGroupListViewWifiResults.createItemView(getString(R.string.wifi_checklist_securityType));
            mCommonListItemViewChecklistConnectivity = mGroupListViewWifiResults.createItemView(getString(R.string.wifi_checklist_connectivity));
            mCommonListItemViewChecklistDns = mGroupListViewWifiResults.createItemView(getString(R.string.wifi_checklist_dns));
            mCommonListItemViewChecklistSsl = mGroupListViewWifiResults.createItemView(getString(R.string.wifi_checklist_ssl));
            mCommonListItemViewInfoSsid = mGroupListViewWifiResults.createItemView(getString(R.string.wifi_info_ssid));
            mCommonListItemViewInfoSecurity = mGroupListViewWifiResults.createItemView(getString(R.string.wifi_info_security));
            mCommonListItemViewInfoFrequency = mGroupListViewWifiResults.createItemView(getString(R.string.wifi_info_frequency));
            mCommonListItemViewInfoSignalStrength = mGroupListViewWifiResults.createItemView(getString(R.string.wifi_info_signalStrength));
            mCommonListItemViewInfoLinkSpeed = mGroupListViewWifiResults.createItemView(getString(R.string.wifi_info_linkSpeed));
            mCommonListItemViewInfoBssid = mGroupListViewWifiResults.createItemView(getString(R.string.wifi_info_bssid));
            mCommonListItemViewInfoIp = mGroupListViewWifiResults.createItemView(getString(R.string.wifi_info_ip));
            mCommonListItemViewInfoGateway = mGroupListViewWifiResults.createItemView(getString(R.string.wifi_info_gateway));
            mCommonListItemViewInfoSubnetMask = mGroupListViewWifiResults.createItemView(getString(R.string.wifi_info_subnetMask));
            mCommonListItemViewInfoDns = mGroupListViewWifiResults.createItemView(getString(R.string.wifi_info_dns));
        } // end if

        IconicsImageView imageViewChecklistSecurityType = new IconicsImageView(this),
                imageViewChecklistConnectivity = new IconicsImageView(this),
                imageViewChecklistDns = new IconicsImageView(this),
                imageViewChecklistSsl = new IconicsImageView(this);
        IconicsDrawable drawablePass = new IconicsDrawable(this)
                .icon(Ionicons.Icon.ion_checkmark_round)
                .color(new IconicsColorInt(getColor(R.color.colourPass)))
                .size(new IconicsSizeDp(AppInitialiser.RESULT_ICON_SIZE));
        IconicsDrawable drawableFail = new IconicsDrawable(this)
                .icon(Ionicons.Icon.ion_alert)
                .color(new IconicsColorInt(Color.RED))
                .size(new IconicsSizeDp(AppInitialiser.RESULT_ICON_SIZE));
        IconicsDrawable drawableUnknown = new IconicsDrawable(this)
                .icon(Ionicons.Icon.ion_help)
                .color(new IconicsColorInt(getColor(R.color.colourWarning)))
                .size(new IconicsSizeDp(AppInitialiser.RESULT_ICON_SIZE));
        IconicsDrawable drawableBigUnknown = new IconicsDrawable(this)
                .icon(Ionicons.Icon.ion_ios_help)
                .color(new IconicsColorInt(getColor(R.color.colourWarning)))
                .size(new IconicsSizeDp(AppInitialiser.FINAL_RESULT_ICON_SIZE));
        String unknownResult = getString(R.string.unknownInfo);
        String noSignalStrenghth = getString(R.string.wifi_info_signalStrength_none);
        WifiResults securedSecurityType, validatedConnectivity, securedDns, securedSsl;

        sendMessage(0); // initialise the progress

        mCommonListItemViewChecklistSecurityType.setAccessoryType(XUICommonListItemView.ACCESSORY_TYPE_CUSTOM);
        mCommonListItemViewChecklistConnectivity.setAccessoryType(XUICommonListItemView.ACCESSORY_TYPE_CUSTOM);
        mCommonListItemViewChecklistDns.setAccessoryType(XUICommonListItemView.ACCESSORY_TYPE_CUSTOM);
        mCommonListItemViewChecklistSsl.setAccessoryType(XUICommonListItemView.ACCESSORY_TYPE_CUSTOM);
        mCommonListItemViewChecklistSecurityType.addAccessoryCustomView(imageViewChecklistSecurityType);
        mCommonListItemViewChecklistConnectivity.addAccessoryCustomView(imageViewChecklistConnectivity);
        mCommonListItemViewChecklistDns.addAccessoryCustomView(imageViewChecklistDns);
        mCommonListItemViewChecklistSsl.addAccessoryCustomView(imageViewChecklistSsl);

        try
        {
            WifiUtils wifiUtils = new WifiUtils(this);

            String[] securityTypeAndLabel = wifiUtils.getSecurity();
            String[] ipAndSubnetMask = wifiUtils.getIpAndSubnetMask();

            mCommonListItemViewInfoSsid.setDetailText(wifiUtils.getSsid());
            mCommonListItemViewInfoSecurity.setDetailText(securityTypeAndLabel[0]);
            mCommonListItemViewInfoFrequency.setDetailText(wifiUtils.categoriseFrequency());
            mCommonListItemViewInfoSignalStrength.setDetailText(wifiUtils.getSignalStrength());
            mCommonListItemViewInfoLinkSpeed.setDetailText(wifiUtils.getLinkSpeed());
            mCommonListItemViewInfoBssid.setDetailText(wifiUtils.getBssid());
            mCommonListItemViewInfoIp.setDetailText(ipAndSubnetMask[0]);
            mCommonListItemViewInfoGateway.setDetailText(wifiUtils.getGateway());
            mCommonListItemViewInfoSubnetMask.setDetailText(ipAndSubnetMask[1]);
            mCommonListItemViewInfoDns.setDetailText(wifiUtils.getDns());
            sendMessage(1);

            if (securityTypeAndLabel[1].equals(WifiUtils.SECURED_SECURITY_TYPE))
            {
                securedSecurityType = WifiResults.PASS;

                imageViewChecklistSecurityType.setIcon(drawablePass);
                LogUtils.i("Secured security type.");
            }
            else if (securityTypeAndLabel[1].equals(WifiUtils.UNSECURED_SECURITY_TYPE))
            {
                securedSecurityType = WifiResults.FAIL;

                imageViewChecklistSecurityType.setIcon(drawableFail);
                LogUtils.w("Unsecured security type.");
            }
            else
            {
                securedSecurityType = WifiResults.UNKNOWN;

                imageViewChecklistSecurityType.setIcon(drawableUnknown);
                LogUtils.w("Cannot tell whether the security type is secured/unsecured. Some errors might occur.");
            } // end nested if...else

            sendMessage(1);

            if (wifiUtils.isValidatedConnectivity())
            {
                validatedConnectivity = WifiResults.PASS;

                imageViewChecklistConnectivity.setIcon(drawablePass);
                LogUtils.i("Validated connectivity.");
                sendMessage(1);

                ExecutorService executorServiceWifi = Executors.newFixedThreadPool(AppInitialiser.CPU_CORE_COUNT + 1 < THREAD_TASK_COUNT ? AppInitialiser.CPU_CORE_COUNT : THREAD_TASK_COUNT);
                executorServiceWifi.execute(() -> mIsSecuredDns = wifiUtils.isSecuredDns());
                executorServiceWifi.execute(() -> mIsSecuredSsl = wifiUtils.isSecuredSsl());
                executorServiceWifi.shutdown();
                executorServiceWifi.awaitTermination(1000, TimeUnit.MILLISECONDS);

                if (mIsSecuredDns)
                {
                    securedDns = WifiResults.PASS;

                    imageViewChecklistDns.setIcon(drawablePass);
                    LogUtils.i("Secured DNS.");
                }
                else
                {
                    securedDns = WifiResults.FAIL;

                    imageViewChecklistDns.setIcon(drawableFail);
                    LogUtils.w("Unsecured DNS.");
                } // end if...else

                sendMessage(1);

                if (mIsSecuredSsl)
                {
                    securedSsl = WifiResults.PASS;

                    imageViewChecklistSsl.setIcon(drawablePass);
                    LogUtils.i("Secured SSL.");
                }
                else
                {
                    securedSsl = WifiResults.FAIL;

                    imageViewChecklistSsl.setIcon(drawableFail);
                    LogUtils.w("Unsecured SSL.");
                } // end if...else

                sendMessage(1);
            }
            else
            {
                validatedConnectivity = WifiResults.FAIL;
                securedDns = WifiResults.UNKNOWN;
                securedSsl = WifiResults.UNKNOWN;

                imageViewChecklistConnectivity.setIcon(drawableFail);
                imageViewChecklistDns.setIcon(drawableUnknown);
                imageViewChecklistSsl.setIcon(drawableUnknown);
                LogUtils.w("Invalidated connectivity. Internet access is not available.");
                LogUtils.w("DNS security: unknown");
                LogUtils.w("SSL security: unknown");
            } // end if...else

            if (securedSecurityType == WifiResults.PASS
                    && validatedConnectivity == WifiResults.PASS
                    && securedDns == WifiResults.PASS
                    && securedSsl == WifiResults.PASS)
            {
                mImageViewWifi.setIcon(new IconicsDrawable(this)
                        .icon(Ionicons.Icon.ion_checkmark_circled)
                        .color(new IconicsColorInt(getColor(R.color.colourPass)))
                        .size(new IconicsSizeDp(AppInitialiser.FINAL_RESULT_ICON_SIZE)));
                mTextViewWifiRationale.setText(R.string.wifi_textViewRationale_pass);
                mRoundButtonWifiAction.setText(R.string.wifi_roundButtonAction_normal);
            }
            else if (securedSecurityType == WifiResults.FAIL
                    || validatedConnectivity == WifiResults.FAIL
                    || securedDns == WifiResults.FAIL
                    || securedSsl == WifiResults.FAIL)
            {
                mImageViewWifi.setIcon(new IconicsDrawable(this)
                        .icon(Ionicons.Icon.ion_android_alert)
                        .color(new IconicsColorInt(Color.RED))
                        .size(new IconicsSizeDp(AppInitialiser.FINAL_RESULT_ICON_SIZE)));
                mTextViewWifiRationale.setText(R.string.wifi_textViewRationale_fail);
                mRoundButtonWifiAction.setText(R.string.wifi_roundButtonAction_fail);
            }
            else
            {
                mImageViewWifi.setIcon(drawableBigUnknown);
                mTextViewWifiRationale.setText(R.string.wifi_textViewRationale_unknown_appError);
                mRoundButtonWifiAction.setText(R.string.wifi_roundButtonAction_unknown);
            } // end nested if...else
        }
        catch (Exception e)
        {
            LogUtils.e("Detection failed. Exception occurred.");
            LogUtils.e(e);
            mImageViewWifi.setIcon(drawableBigUnknown);

            if (e instanceof NetworkErrorException)
                mTextViewWifiRationale.setText(R.string.wifi_textViewRationale_unknown_connectivity);
            else
                mTextViewWifiRationale.setText(R.string.wifi_textViewRationale_unknown_appError);

            imageViewChecklistSecurityType.setIcon(drawableUnknown);
            imageViewChecklistConnectivity.setIcon(drawableUnknown);
            imageViewChecklistDns.setIcon(drawableUnknown);
            imageViewChecklistSsl.setIcon(drawableUnknown);
            mRoundButtonWifiAction.setText(R.string.wifi_roundButtonAction_unknown);
            mCommonListItemViewInfoSsid.setDetailText(unknownResult);
            mCommonListItemViewInfoSecurity.setDetailText(unknownResult);
            mCommonListItemViewInfoFrequency.setDetailText(unknownResult);
            mCommonListItemViewInfoSignalStrength.setDetailText(noSignalStrenghth);
            mCommonListItemViewInfoLinkSpeed.setDetailText(unknownResult);
            mCommonListItemViewInfoBssid.setDetailText(unknownResult);
            mCommonListItemViewInfoIp.setDetailText(unknownResult);
            mCommonListItemViewInfoGateway.setDetailText(unknownResult);
            mCommonListItemViewInfoSubnetMask.setDetailText(unknownResult);
            mCommonListItemViewInfoDns.setDetailText(unknownResult);
            sendMessage(-1);
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
                        LogUtils.w("Null object instantiated from the class " + ClipboardManager.class.getSimpleName() + ".");
                } // end if

                return false;
            };

        if (isFirstScan)
        {
            XUIGroupListView.newSection(this)
                    .setTitle(getString(R.string.wifi_checklist_title))
                    .setDescription(" ") // this description is set so as to leave space between 2 sections
                    .setSeparatorDrawableRes(R.drawable.common_list_item_bg_with_border_none_selector,
                            R.drawable.common_list_item_bg_with_border_none_selector,
                            R.drawable.common_list_item_bg_with_border_none_selector,
                            R.drawable.common_list_item_bg_with_border_none_selector)
                    .addItemView(mCommonListItemViewChecklistSecurityType, null)
                    .addItemView(mCommonListItemViewChecklistConnectivity, null)
                    .addItemView(mCommonListItemViewChecklistDns, null)
                    .addItemView(mCommonListItemViewChecklistSsl, null)
                    .addTo(mGroupListViewWifiResults);
            XUIGroupListView.newSection(this)
                    .setTitle(getString(R.string.wifi_info_title))
                    .setSeparatorDrawableRes(R.drawable.common_list_item_bg_with_border_none_selector,
                            R.drawable.common_list_item_bg_with_border_none_selector,
                            R.drawable.common_list_item_bg_with_border_none_selector,
                            R.drawable.common_list_item_bg_with_border_none_selector)
                    .addItemView(mCommonListItemViewInfoSsid, null, onLongClickListenerWifiInfo)
                    .addItemView(mCommonListItemViewInfoSecurity, null, onLongClickListenerWifiInfo)
                    .addItemView(mCommonListItemViewInfoFrequency, null, onLongClickListenerWifiInfo)
                    .addItemView(mCommonListItemViewInfoSignalStrength, null, onLongClickListenerWifiInfo)
                    .addItemView(mCommonListItemViewInfoLinkSpeed, null, onLongClickListenerWifiInfo)
                    .addItemView(mCommonListItemViewInfoBssid, null, onLongClickListenerWifiInfo)
                    .addItemView(mCommonListItemViewInfoIp, null, onLongClickListenerWifiInfo)
                    .addItemView(mCommonListItemViewInfoGateway, null, onLongClickListenerWifiInfo)
                    .addItemView(mCommonListItemViewInfoSubnetMask, null, onLongClickListenerWifiInfo)
                    .addItemView(mCommonListItemViewInfoDns, null, onLongClickListenerWifiInfo)
                    .addTo(mGroupListViewWifiResults);
        } // end if
    } // end method getWifiResults

    /**
     * Play animation to hide the circle progress view and to display scan results.
     */
    private void playAnimation()
    {
        ViewUtils.fadeOut(mCircleProgressViewWifi, mAnimationDuration, null);
        (new Handler()).postDelayed(() ->
        {
            ViewUtils.fadeIn(mImageViewWifi, mAnimationDuration, null);
            ViewUtils.fadeIn(mTextViewWifiRationale, mAnimationDuration, null);
            ViewUtils.slideIn(mLinearLayoutWifiResults, mAnimationDuration, null, ViewUtils.Direction.BOTTOM_TO_TOP);
        }, mAnimationDuration / 2);
    } // end method playAnimation

    /**
     * Send a message to the handler to update the progress.
     * @param flag the integer 1 if there is a new completed scan task; otherwise, the integer 0
     */
    private void sendMessage(int flag)
    {
        Message message = new Message();
        message.what = flag;
        mHandlerWifi.sendMessage(message);
    } // end method sendMessage
} // end class ActivityWifi