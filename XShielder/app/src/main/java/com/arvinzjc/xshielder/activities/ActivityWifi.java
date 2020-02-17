/*
 * @Description: a class for the activity of the Wi-Fi security shield
 * @Version: 2.0.0.20200213
 * @Author: Arvin Zhao
 * @Date: 2020-01-19 13:59:45
 * @Last Editors: Arvin Zhao
 * @LastEditTime : 2020-02-13 23:52:07
 */

package com.arvinzjc.xshielder.activities;

import android.Manifest;
import android.accounts.NetworkErrorException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.LocationManager;
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

import com.arvinzjc.xshielder.AppInitialiser;
import com.arvinzjc.xshielder.R;
import com.arvinzjc.xshielder.utils.StatusBarThemeUtils;
import com.arvinzjc.xshielder.utils.WifiUtils;
import com.mikepenz.iconics.IconicsColorInt;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.IconicsSizeDp;
import com.mikepenz.iconics.typeface.library.ionicons.Ionicons;
import com.mikepenz.iconics.view.IconicsImageView;
import com.apkfuns.logutils.LogUtils;
import com.xuexiang.xui.utils.StatusBarUtils;
import com.xuexiang.xui.utils.ViewUtils;
import com.xuexiang.xui.widget.button.roundbutton.RoundButton;
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction;
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
    private enum LocationServicesStatus
    {
        ENABLED,
        DISABLED,
        UNKNOWN
    } // end enum LocationServicesStatus

    private enum WifiResults
    {
        PASS,
        FAIL,
        UNKNOWN
    } // end enum WifiResults

    private static final int PERMISSION_REQUEST_WIFI = 0; // the code of the permission request appeared on this activity
    private static final int LOCATION_SERVICES_REQUEST = 1; // the code of the request of enabling Location Services and returning results
    private static final int WIFI_CONFIGURATION_REQUEST = 2; // the code of the request of configuring Wi-Fi and returning results

    /*
     * 1-10. the info of the connected Wi-Fi;
     * 11. secured security type;
     * 12. connectivity;
     * 13. secured DNS;
     * 14. secured SSL
     */
    private static final int SCAN_TASK_COUNT = 14;

    /*
     * 1. secured DNS;
     * 2. secured SSL
     */
    private static final int PARALLEL_TASK_COUNT = 2;
    private static final int THREAD_TASK_TIMEOUT = 1; // the maximum time (unit: minute) to wait for thread tasks to complete execution

    private int mCompletedScanTaskCount, mAnimationDuration;
    private boolean mIsFirstScan = true, mHasInternetConnection, mIsSecuredDns, mIsSecuredSsl;
    private WifiResults mFinalResult;
    private ExecutorService mExecutorServiceScan;
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

        mExecutorServiceScan = Executors.newSingleThreadExecutor();
        mAnimationDuration = getResources().getInteger(android.R.integer.config_mediumAnimTime); // 400 milliseconds
        mCircleProgressViewWifi = findViewById(R.id.circleProgressViewWifi);
        mImageViewWifi = findViewById(R.id.imageViewWifi);
        mTextViewWifiRationale = findViewById(R.id.textViewWifiRationale);
        mLinearLayoutWifiResults = findViewById(R.id.linearLayoutWifiResults);
        mGroupListViewWifiResults = findViewById(R.id.groupListViewWifiResults);
        mRoundButtonWifiAction = findViewById(R.id.roundButtonWifiAction);
        mRoundButtonWifiAction.setOnClickListener((View view) ->
        {
            if (mRoundButtonWifiAction.getText().equals(getString(R.string.wifi_roundButtonAction_fail)))
            {
                LogUtils.i("User chose to go to the Wi-Fi settings page to configure Wi-Fi.");
                startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), WIFI_CONFIGURATION_REQUEST);
            }
            else
            {
                LogUtils.i("User chose to check Wi-Fi security again.");
                mRoundButtonWifiAction.setEnabled(false); // avoid abnormal progress values when the user clicks quickly
                mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INITIALISATION_FLAG);
                ViewUtils.fadeOut(mImageViewWifi, mAnimationDuration, null);
                ViewUtils.fadeOut(mTextViewWifiRationale, mAnimationDuration, null);
                ViewUtils.slideOut(mLinearLayoutWifiResults, mAnimationDuration, null, ViewUtils.Direction.TOP_TO_BOTTOM);
                findViewById(R.id.nestedScrollViewWifiResults).scrollTo(0, 0);
                (new Handler()).postDelayed(() -> ViewUtils.fadeIn(mCircleProgressViewWifi, mAnimationDuration, null), mAnimationDuration / 2);

                mIsFirstScan = false;

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    showPermissionWarningDialogue();
                else
                    if (getLocationServicesStatus() == LocationServicesStatus.ENABLED)
                        (new Handler()).postDelayed(() -> mExecutorServiceScan.execute(() -> getWifiResults(mIsFirstScan)), mAnimationDuration);
                    else
                        showLocationServicesStatusWarningDialogue();
            } // end if...else
        });
        mHandlerWifi = new Handler((Message message) ->
        {
            switch (message.what)
            {
                case AppInitialiser.PROGRESS_INCREMENT_FLAG:
                    mCircleProgressViewWifi.setProgress(++mCompletedScanTaskCount * 100 / SCAN_TASK_COUNT);
                    if (mCompletedScanTaskCount == SCAN_TASK_COUNT)
                        playAnimationAfterProgress();
                    return true;

                case AppInitialiser.PROGRESS_INITIALISATION_FLAG:
                    mCircleProgressViewWifi.setProgress(AppInitialiser.START_PROGRESS);
                    return true;

                case AppInitialiser.PROGRESS_ERROR_FLAG:
                    mCompletedScanTaskCount = SCAN_TASK_COUNT;
                    mCircleProgressViewWifi.setProgress(AppInitialiser.END_PROGRESS);
                    playAnimationAfterProgress();
                    return true;

                default:
                    LogUtils.w("Received abnormal handler message flag (" + message.what + "). Some errors might occur.");
                    return false;
            } // end switch-case
        });

        // hide all widgets except the toolbar and the circle progress view
        mImageViewWifi.setVisibility(View.INVISIBLE);
        mTextViewWifiRationale.setVisibility(View.INVISIBLE);
        mLinearLayoutWifiResults.setVisibility(View.INVISIBLE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            showPermissionWarningDialogue();
        else
            if (getLocationServicesStatus() == LocationServicesStatus.ENABLED)
                (new Handler()).postDelayed(() -> mExecutorServiceScan.execute(() -> getWifiResults(mIsFirstScan)), mAnimationDuration);
            else
                showLocationServicesStatusWarningDialogue();
    } // end method onCreate

    /**
     * Dispatch incoming results to this activity.
     * @param requestCode the request code to identify who the results came from
     * @param resultCode the result code returned by the child activity
     * @param data an Intent which can return result data to the caller
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode)
        {
            case LOCATION_SERVICES_REQUEST:
                if (getLocationServicesStatus() == LocationServicesStatus.ENABLED)
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                        recreate();
                    else
                        (new Handler()).postDelayed(() -> mExecutorServiceScan.execute(() -> getWifiResults(mIsFirstScan)), mAnimationDuration);
                else
                {
                    LogUtils.w("User chose not to enable Location Services. Return to the specified activity.");
                    mExecutorServiceScan.shutdownNow();
                    finish();
                } // end if...else
                break;

            case WIFI_CONFIGURATION_REQUEST:
                mRoundButtonWifiAction.setText(R.string.wifi_roundButtonAction_normal);
                break;

            default:
                LogUtils.w("Received abnormal request code to identify who the results came from (" + requestCode + "). Some errors might occur.");
        } // end switch-case
    } // end method onActivityResult

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
            confirmBeforeLeaving(); // confirm stopping checking Wi-Fi security when the Up button on the tool bar is clicked and the scan is not completed
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
        if (requestCode == PERMISSION_REQUEST_WIFI)
        {
            if (grantResultArray.length > 0 && grantResultArray[0] == PackageManager.PERMISSION_GRANTED)
            {
                LogUtils.i("Accepted to grant the permission(s). Start to check Wi-Fi security.");

                if (getLocationServicesStatus() == LocationServicesStatus.ENABLED)
                    mExecutorServiceScan.execute(() -> getWifiResults(mIsFirstScan));
                else
                    showLocationServicesStatusWarningDialogue();
            }
            else
            {
                LogUtils.w("Refused to grant the permission(s) to enable checking Wi-Fi security. Return to the specified activity.");
                mExecutorServiceScan.shutdownNow();
                finish();
            } // end if...else
        } // end if
    } // end method onRequestPermissionsResult

    /**
     * Confirm stopping checking Wi-Fi security when the back button is pressed and the scan is not completed.
     */
    @Override
    public void onBackPressed()
    {
        confirmBeforeLeaving();
    } // end method onBackPressed

    // show a confirmation dialogue before finishing this activity when necessary
    private void confirmBeforeLeaving()
    {
        if (mCompletedScanTaskCount < SCAN_TASK_COUNT)
            new MaterialDialog.Builder(this)
                    .backgroundColor(getColor(R.color.card_backgroundColour))
                    .icon(new IconicsDrawable(this)
                            .icon(Ionicons.Icon.ion_information_circled)
                            .color(new IconicsColorInt(getColor(R.color.colourInfo)))
                            .size(new IconicsSizeDp(AppInitialiser.DIALOGUE_ICON_SIZE)))
                    .title(R.string.dialogueScanStopConfirmation_title)
                    .titleColor(getColor(R.color.primaryTextColour))
                    .content(R.string.wifi_dialogueScanStopConfirmation_content)
                    .contentColor(getColor(R.color.contentTextColour))
                    .positiveText(R.string.wifi_dialogueScanStopConfirmation_positiveText)
                    .positiveColor(getColor(R.color.colourInfo))
                    .onPositive((MaterialDialog dialogue, DialogAction which) ->
                    {
                        // stop checking Wi-Fi security if the user clicks the confirmation button when the scan is not completed
                        if (mCompletedScanTaskCount < SCAN_TASK_COUNT)
                        {
                            LogUtils.i("User chose to stop checking Wi-Fi security.");
                            mExecutorServiceScan.shutdownNow();
                            finish();
                        } // end if
                    })
                    .negativeText(R.string.dialogue_defaultNegativeText)
                    .negativeColor(getColor(R.color.colourInfo))
                    .cancelable(false)
                    .show(); // show a confirmation dialogue asking whether to stop checking Wi-Fi security when the scan is not completed
        else
        {
            if (mFinalResult == WifiResults.FAIL)
                new MaterialDialog.Builder(this)
                        .backgroundColor(getColor(R.color.card_backgroundColour))
                        .icon(new IconicsDrawable(this)
                                .icon(Ionicons.Icon.ion_information_circled)
                                .color(new IconicsColorInt(getColor(R.color.colourInfo)))
                                .size(new IconicsSizeDp(AppInitialiser.DIALOGUE_ICON_SIZE)))
                        .title(R.string.dialogueIssueDisregardConfirmation_title)
                        .titleColor(getColor(R.color.primaryTextColour))
                        .content(R.string.wifi_dialogueIssueDisregardConfirmation_content)
                        .contentColor(getColor(R.color.contentTextColour))
                        .positiveText(R.string.wifi_dialogueIssueDisregardConfirmation_positiveText)
                        .positiveColor(getColor(R.color.colourInfo))
                        .onPositive((MaterialDialog dialogue, DialogAction which) ->
                        {
                            LogUtils.i("User chose to disregard the Wi-Fi security issue(s).");
                            mExecutorServiceScan.shutdownNow();
                            finish();
                        })
                        .negativeText(R.string.dialogue_defaultNegativeText)
                        .negativeColor(getColor(R.color.colourInfo))
                        .cancelable(false)
                        .show(); // show a confirmation dialogue asking whether to disregard Wi-Fi security issues
            else
            {
                mExecutorServiceScan.shutdownNow();
                finish();
            } // end if...else
        } // end if...else
    } // end method confirmBeforeLeaving

    // get and display the scan results of Wi-Fi security
    private void getWifiResults(boolean isFirstScan)
    {
        mCompletedScanTaskCount = 0; // initialise the number of completed scan tasks

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
        WifiResults securedSecurityType, internetConnection, securedDns, securedSsl;

        runOnUiThread(() ->
        {
            mCommonListItemViewChecklistSecurityType.setAccessoryType(XUICommonListItemView.ACCESSORY_TYPE_CUSTOM);
            mCommonListItemViewChecklistConnectivity.setAccessoryType(XUICommonListItemView.ACCESSORY_TYPE_CUSTOM);
            mCommonListItemViewChecklistDns.setAccessoryType(XUICommonListItemView.ACCESSORY_TYPE_CUSTOM);
            mCommonListItemViewChecklistSsl.setAccessoryType(XUICommonListItemView.ACCESSORY_TYPE_CUSTOM);
            mCommonListItemViewChecklistSecurityType.addAccessoryCustomView(imageViewChecklistSecurityType);
            mCommonListItemViewChecklistConnectivity.addAccessoryCustomView(imageViewChecklistConnectivity);
            mCommonListItemViewChecklistDns.addAccessoryCustomView(imageViewChecklistDns);
            mCommonListItemViewChecklistSsl.addAccessoryCustomView(imageViewChecklistSsl);
        });

        try
        {
            WifiUtils wifiUtils = new WifiUtils(this);

            runOnUiThread(() -> mCommonListItemViewInfoSsid.setDetailText(wifiUtils.getSsid()));
            mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INCREMENT_FLAG); // complete Scan Task 1

            String[] securityTypeAndLabel = wifiUtils.getSecurity();
            runOnUiThread(() -> mCommonListItemViewInfoSecurity.setDetailText(securityTypeAndLabel[0]));
            mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INCREMENT_FLAG); // complete Scan Task 2

            runOnUiThread(() -> mCommonListItemViewInfoFrequency.setDetailText(wifiUtils.categoriseFrequency()));
            mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INCREMENT_FLAG); // complete Scan Task 3

            runOnUiThread(() -> mCommonListItemViewInfoSignalStrength.setDetailText(wifiUtils.getSignalStrength()));
            mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INCREMENT_FLAG); // complete Scan Task 4

            runOnUiThread(() -> mCommonListItemViewInfoLinkSpeed.setDetailText(wifiUtils.getLinkSpeed()));
            mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INCREMENT_FLAG); // complete Scan Task 5

            runOnUiThread(() -> mCommonListItemViewInfoBssid.setDetailText(wifiUtils.getBssid()));
            mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INCREMENT_FLAG); // complete Scan Task 6

            String[] ipAndSubnetMask = wifiUtils.getIpAndSubnetMask();
            runOnUiThread(() -> mCommonListItemViewInfoIp.setDetailText(ipAndSubnetMask[0]));
            mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INCREMENT_FLAG); // complete Scan Task 7
            runOnUiThread(() -> mCommonListItemViewInfoSubnetMask.setDetailText(ipAndSubnetMask[1]));
            mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INCREMENT_FLAG); // complete Scan Task 8

            runOnUiThread(() -> mCommonListItemViewInfoGateway.setDetailText(wifiUtils.getGateway()));
            mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INCREMENT_FLAG); // complete Scan Task 9

            runOnUiThread(() -> mCommonListItemViewInfoDns.setDetailText(wifiUtils.getDns()));
            mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INCREMENT_FLAG); // complete Scan Task 10

            if (securityTypeAndLabel[1].equals(WifiUtils.SECURED_SECURITY_TYPE))
            {
                securedSecurityType = WifiResults.PASS;

                runOnUiThread(() -> imageViewChecklistSecurityType.setIcon(drawablePass));
                LogUtils.i("Secured security type.");
            }
            else if (securityTypeAndLabel[1].equals(WifiUtils.UNSECURED_SECURITY_TYPE))
            {
                securedSecurityType = WifiResults.FAIL;

                runOnUiThread(() -> imageViewChecklistSecurityType.setIcon(drawableFail));
                LogUtils.w("Unsecured security type.");
            }
            else
            {
                securedSecurityType = WifiResults.UNKNOWN;

                runOnUiThread(() -> imageViewChecklistSecurityType.setIcon(drawableUnknown));
                LogUtils.w("Cannot tell whether the security type is secured/unsecured. Some errors might occur.");
            } // end nested if...else

            mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INCREMENT_FLAG); // complete Scan Task 11

            ExecutorService executorServiceInternetConnection = Executors.newSingleThreadExecutor();
            executorServiceInternetConnection.execute(() -> mHasInternetConnection = wifiUtils.hasInternetConnection());
            executorServiceInternetConnection.shutdown();
            executorServiceInternetConnection.awaitTermination(THREAD_TASK_TIMEOUT, TimeUnit.MINUTES);

            if (mHasInternetConnection)
            {
                internetConnection = WifiResults.PASS;

                runOnUiThread(() -> imageViewChecklistConnectivity.setIcon(drawablePass));
                mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INCREMENT_FLAG); // complete Scan Task 12

                ExecutorService executorServiceParallelTasks = Executors.newFixedThreadPool(AppInitialiser.CPU_CORE_COUNT + 1 < PARALLEL_TASK_COUNT ? AppInitialiser.CPU_CORE_COUNT : PARALLEL_TASK_COUNT);
                executorServiceParallelTasks.execute(() -> mIsSecuredDns = wifiUtils.isSecuredDns());
                executorServiceParallelTasks.execute(() -> mIsSecuredSsl = wifiUtils.isSecuredSsl());
                executorServiceParallelTasks.shutdown();
                executorServiceParallelTasks.awaitTermination(THREAD_TASK_TIMEOUT, TimeUnit.MINUTES);

                if (mIsSecuredDns)
                {
                    securedDns = WifiResults.PASS;

                    runOnUiThread(() -> imageViewChecklistDns.setIcon(drawablePass));
                }
                else
                {
                    securedDns = WifiResults.FAIL;

                    runOnUiThread(() -> imageViewChecklistDns.setIcon(drawableFail));
                } // end if...else

                mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INCREMENT_FLAG); // complete Scan Task 13

                if (mIsSecuredSsl)
                {
                    securedSsl = WifiResults.PASS;

                    runOnUiThread(() -> imageViewChecklistSsl.setIcon(drawablePass));
                }
                else
                {
                    securedSsl = WifiResults.FAIL;

                    runOnUiThread(() -> imageViewChecklistSsl.setIcon(drawableFail));
                } // end if...else

                mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INCREMENT_FLAG); // complete Scan Task 14
            }
            else
            {
                internetConnection = WifiResults.FAIL;
                mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INCREMENT_FLAG); // complete Scan Task 12

                securedDns = WifiResults.UNKNOWN;
                LogUtils.w("DNS security: unknown");
                mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INCREMENT_FLAG); // complete Scan Task 13

                securedSsl = WifiResults.UNKNOWN;
                LogUtils.w("SSL security: unknown");
                mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INCREMENT_FLAG); // complete Scan Task 14

                runOnUiThread(() ->
                {
                    imageViewChecklistConnectivity.setIcon(drawableFail);
                    imageViewChecklistDns.setIcon(drawableUnknown);
                    imageViewChecklistSsl.setIcon(drawableUnknown);
                });
            } // end if...else

            runOnUiThread(() ->
            {
                if (securedSecurityType == WifiResults.PASS
                        && internetConnection == WifiResults.PASS
                        && securedDns == WifiResults.PASS
                        && securedSsl == WifiResults.PASS)
                {
                    mFinalResult = WifiResults.PASS;

                    mImageViewWifi.setIcon(new IconicsDrawable(this)
                            .icon(Ionicons.Icon.ion_checkmark_circled)
                            .color(new IconicsColorInt(getColor(R.color.colourPass)))
                            .size(new IconicsSizeDp(AppInitialiser.FINAL_RESULT_ICON_SIZE)));
                    mTextViewWifiRationale.setText(R.string.wifi_textViewRationale_pass);
                    mRoundButtonWifiAction.setText(R.string.wifi_roundButtonAction_normal);
                }
                else if ((securedSecurityType == WifiResults.FAIL
                        || securedDns == WifiResults.FAIL
                        || securedSsl == WifiResults.FAIL))
                {
                    mFinalResult = WifiResults.FAIL;

                    mImageViewWifi.setIcon(new IconicsDrawable(this)
                            .icon(Ionicons.Icon.ion_android_alert)
                            .color(new IconicsColorInt(Color.RED))
                            .size(new IconicsSizeDp(AppInitialiser.FINAL_RESULT_ICON_SIZE)));
                    mTextViewWifiRationale.setText(R.string.wifi_textViewRationale_fail_default);
                    mRoundButtonWifiAction.setText(R.string.wifi_roundButtonAction_fail);
                }
                /*
                 * execute this part if the connected Wi-Fi does not have unknown security type and Internet connection (internetConnection == WifiResults.FAIL);
                 * the condition in the parentheses above is not included because it is always true at this point;
                 * the reason is that the results of DNS and SSL security are unknown only when there is Internet connection;
                 * therefore, there is only 1 situation suitable for this part (secured security type, available Internet connection, and unknown DNS and SSL security)
                 */
                else if (securedSecurityType != WifiResults.UNKNOWN)
                {
                    mFinalResult = WifiResults.FAIL;

                    mImageViewWifi.setIcon(new IconicsDrawable(this)
                            .icon(Ionicons.Icon.ion_android_alert)
                            .color(new IconicsColorInt(Color.RED))
                            .size(new IconicsSizeDp(AppInitialiser.FINAL_RESULT_ICON_SIZE)));
                    mTextViewWifiRationale.setText(R.string.wifi_textViewRationale_fail_connectivity);
                    mRoundButtonWifiAction.setText(R.string.wifi_roundButtonAction_fail);
                }
                else
                {
                    mFinalResult = WifiResults.UNKNOWN;

                    mImageViewWifi.setIcon(drawableBigUnknown);
                    mTextViewWifiRationale.setText(R.string.wifi_textViewRationale_unknown_appError);
                    mRoundButtonWifiAction.setText(R.string.wifi_roundButtonAction_unknown);
                } // end nested if...else
            });
        }
        catch (Exception e)
        {
            if (e instanceof InterruptedException)
            {
                LogUtils.e("The scan thread has been interrupted. An exception occurred. This might be caused by stopping checking Wi-Fi security.");
                LogUtils.e(e);
            }
            else
            {
                LogUtils.e("Failed to check Wi-Fi security. An exception occurred.");
                LogUtils.e(e);
                runOnUiThread(() ->
                {
                    mImageViewWifi.setIcon(drawableBigUnknown);

                    if (e instanceof NetworkErrorException)
                    {
                        mTextViewWifiRationale.setText(R.string.wifi_textViewRationale_unknown_connectivity);
                        mRoundButtonWifiAction.setText(R.string.wifi_roundButtonAction_fail);
                    }
                    else
                    {
                        mTextViewWifiRationale.setText(R.string.wifi_textViewRationale_unknown_appError);
                        mRoundButtonWifiAction.setText(R.string.wifi_roundButtonAction_unknown);
                    } // end if...else

                    imageViewChecklistSecurityType.setIcon(drawableUnknown);
                    imageViewChecklistConnectivity.setIcon(drawableUnknown);
                    imageViewChecklistDns.setIcon(drawableUnknown);
                    imageViewChecklistSsl.setIcon(drawableUnknown);
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
                });
            } // end if...else

            mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_ERROR_FLAG);
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
        runOnUiThread(() ->
        {
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
        });
    } // end method getWifiResults

    // get the status of Location Services
    private LocationServicesStatus getLocationServicesStatus()
    {
        LocationManager locationManager = (LocationManager)this.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        if (locationManager != null)
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                return LocationServicesStatus.ENABLED;
            else
                return LocationServicesStatus.DISABLED;

        LogUtils.w("Failed to get the status of Location Services. Some errors might occur.");
        return LocationServicesStatus.UNKNOWN;
    } // end method getLocationServicesStatus

    // play animation to hide the circle progress view and to display scan results
    private void playAnimationAfterProgress()
    {
        ViewUtils.fadeOut(mCircleProgressViewWifi, mAnimationDuration, null);
        (new Handler()).postDelayed(() ->
        {
            ViewUtils.fadeIn(mImageViewWifi, mAnimationDuration, null);
            ViewUtils.fadeIn(mTextViewWifiRationale, mAnimationDuration, null);
            ViewUtils.slideIn(mLinearLayoutWifiResults, mAnimationDuration, null, ViewUtils.Direction.BOTTOM_TO_TOP);
            mRoundButtonWifiAction.setEnabled(true);
        }, mAnimationDuration / 2);
    } // end method playAnimationAfterProgress

    // show a Location Services status warning dialogue to ask for enabling Location Services
    private void showLocationServicesStatusWarningDialogue()
    {
        new MaterialDialog.Builder(this)
                .backgroundColor(getColor(R.color.card_backgroundColour))
                .icon(new IconicsDrawable(this)
                        .icon(Ionicons.Icon.ion_android_alert)
                        .color(new IconicsColorInt(getColor(R.color.colourWarning)))
                        .size(new IconicsSizeDp(AppInitialiser.DIALOGUE_ICON_SIZE)))
                .title(R.string.wifi_dialogueLocationServicesStatusWarning_title)
                .titleColor(getColor(R.color.primaryTextColour))
                .content(R.string.wifi_dialogueLocationServicesStatusWarning_content)
                .contentColor(getColor(R.color.contentTextColour))
                .positiveText(R.string.wifi_dialogueLocationServicesStatusWarning_positiveText)
                .positiveColor(getColor(R.color.colourInfo))
                .onPositive((MaterialDialog dialogue, DialogAction which) -> startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), LOCATION_SERVICES_REQUEST))
                .negativeText(R.string.dialogue_defaultNegativeText)
                .negativeColor(getColor(R.color.colourInfo))
                .onNegative((MaterialDialog dialogue, DialogAction which) ->
                {
                    LogUtils.w("User chose not to enable Location Services. Return to the specified activity.");
                    mExecutorServiceScan.shutdownNow();
                    finish();
                })
                .cancelable(false)
                .show();
        LogUtils.w("Location Services is not enabled. A Location Services status warning dialogue from the app appeared.");
    } // end method showLocationServicesStatusWarningDialogue

    // show a permission warning dialogue to give a brief rationale of the permission(s) to enable checking Wi-Fi security
    private void showPermissionWarningDialogue()
    {
        String[] permissionArray;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            permissionArray = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        else
            permissionArray = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION};

        new MaterialDialog.Builder(this)
                .backgroundColor(getColor(R.color.card_backgroundColour))
                .icon(new IconicsDrawable(this)
                        .icon(Ionicons.Icon.ion_android_alert)
                        .color(new IconicsColorInt(getColor(R.color.colourWarning)))
                        .size(new IconicsSizeDp(AppInitialiser.DIALOGUE_ICON_SIZE)))
                .title(R.string.dialoguePermissionWarning_title)
                .titleColor(getColor(R.color.primaryTextColour))
                .content(R.string.wifi_dialoguePermissionWarning_content)
                .contentColor(getColor(R.color.contentTextColour))
                .positiveText(R.string.dialogue_defaultPositiveText)
                .positiveColor(getColor(R.color.colourInfo))
                .cancelable(false)
                .dismissListener(permissionAlert -> ActivityCompat.requestPermissions(this, permissionArray, PERMISSION_REQUEST_WIFI))
                .show();
        LogUtils.w("The permission(s) to enable checking Wi-Fi security is/are not granted. A permission warning dialogue from the app appeared.");
    } // end method showPermissionWarningDialogue
} // end class ActivityWifi