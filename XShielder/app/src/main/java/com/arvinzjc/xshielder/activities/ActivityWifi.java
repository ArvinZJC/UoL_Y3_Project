/*
 * @Description: a class for the activity of the Wi-Fi security shield
 * @Version: 2.1.5.20200303
 * @Author: Arvin Zhao
 * @Date: 2020-01-19 13:59:45
 * @Last Editors: Arvin Zhao
 * @LastEditTime : 2020-03-03 23:52:07
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
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.arvinzjc.xshielder.AppInitialiser;
import com.arvinzjc.xshielder.R;
import com.arvinzjc.xshielder.databinding.ActivityWifiBinding;
import com.arvinzjc.xshielder.utils.SystemBarThemeUtils;
import com.arvinzjc.xshielder.utils.WifiUtils;
import com.mikepenz.iconics.IconicsColorInt;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.IconicsSizeDp;
import com.mikepenz.iconics.typeface.library.ionicons.Ionicons;
import com.mikepenz.iconics.view.IconicsImageView;
import com.apkfuns.logutils.LogUtils;
import com.xuexiang.xui.utils.ViewUtils;
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;
import com.xuexiang.xui.widget.grouplist.XUICommonListItemView;
import com.xuexiang.xui.widget.grouplist.XUIGroupListView;

public class ActivityWifi extends AppCompatActivity
{
    private enum LocationServicesStatus
    {
        ENABLED,
        DISABLED,
        UNKNOWN
    } // end enum LocationServicesStatus

    private static final int PERMISSION_REQUEST_WIFI = 0; // the code of the permission request appeared on this activity
    private static final int LOCATION_SERVICES_REQUEST = 1; // the code of the request of enabling Location Services and returning results
    private static final int WIFI_CONFIGURATION_REQUEST = 2; // the code of the request of configuring Wi-Fi and returning results

    /*
     * 1-10. the info of the connected Wi-Fi;
     * 11. secured security type;
     * 12. connectivity;
     * 13. secured DNS;
     * 14. secured SSL;
     * 15. ready for displaying results
     */
    private static final int SCAN_TASK_COUNT = 15;

    /*
     * 1. secured DNS;
     * 2. secured SSL
     */
    private static final int PARALLEL_TASK_COUNT = 2;

    private static final int THREAD_TASK_TIMEOUT = 1; // the maximum time (unit: minute) to wait for thread tasks to complete execution

    private ActivityWifiBinding mActivityWifiBinding;
    private int mCompletedScanTaskCount = 0, mAnimationDuration;
    private boolean mIsFirstScan = true, mHasInternetConnection, mIsSecuredDns, mIsSecuredSsl;
    private Configuration mConfiguration;
    private AppInitialiser.FinalResults mFinalResult = null;
    private ExecutorService mExecutorServiceScan;
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
    private MaterialDialog mDialogueScanStopConfirmation, mDialogueIssueDisregardConfirmation, mDialogueLocationServicesStatusWarning, mDialoguePermissionWarning;
    private Handler mHandlerWifi;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        LogUtils.i("Enter the activity of the Wi-Fi security shield.");

        mConfiguration = getResources().getConfiguration();
        SystemBarThemeUtils.changeStatusBarTheme(this, mConfiguration);
        SystemBarThemeUtils.changeNavigationBarTheme(this, mConfiguration, getColor(R.color.app_themeColour), false);

        mActivityWifiBinding = ActivityWifiBinding.inflate(getLayoutInflater());
        setContentView(mActivityWifiBinding.getRoot());
        setSupportActionBar(mActivityWifiBinding.toolbarWifi);

        mAnimationDuration = getResources().getInteger(android.R.integer.config_mediumAnimTime); // 400 milliseconds
        mExecutorServiceScan = Executors.newSingleThreadExecutor();
        mActivityWifiBinding.roundButtonWifiAction.setOnClickListener(view ->
        {
            if (mActivityWifiBinding.roundButtonWifiAction.getText().equals(getString(R.string.wifi_roundButtonAction_fail)))
            {
                LogUtils.i("User chose to go to the Wi-Fi settings screen to configure Wi-Fi.");
                startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), WIFI_CONFIGURATION_REQUEST);
            }
            else
            {
                LogUtils.i("User chose to check Wi-Fi security again.");
                mActivityWifiBinding.roundButtonWifiAction.setEnabled(false); // avoid abnormal progress values when the user clicks quickly
                mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INITIALISATION_FLAG);
                ViewUtils.fadeOut(mActivityWifiBinding.imageViewWifi, mAnimationDuration, null);
                ViewUtils.fadeOut(mActivityWifiBinding.textViewWifiRationale, mAnimationDuration, null);
                ViewUtils.slideOut(mActivityWifiBinding.linearLayoutWifiResults, mAnimationDuration, null, ViewUtils.Direction.TOP_TO_BOTTOM);
                findViewById(R.id.nestedScrollViewWifiResults).scrollTo(0, 0);
                (new Handler()).postDelayed(() ->
                {
                    SystemBarThemeUtils.changeNavigationBarTheme(this, mConfiguration, getColor(R.color.app_themeColour), false);
                    ViewUtils.fadeIn(mActivityWifiBinding.circleProgressViewWifi, mAnimationDuration, null);
                }, mAnimationDuration / 2);

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
        mHandlerWifi = new Handler(message ->
        {
            switch (message.what)
            {
                case AppInitialiser.PROGRESS_INCREMENT_FLAG:
                    mActivityWifiBinding.circleProgressViewWifi.setProgress(++mCompletedScanTaskCount * 100f / SCAN_TASK_COUNT);
                    if (mCompletedScanTaskCount == SCAN_TASK_COUNT)
                        playAnimationAfterProgress();
                    return true;

                case AppInitialiser.PROGRESS_INITIALISATION_FLAG:
                    mActivityWifiBinding.circleProgressViewWifi.setProgress(AppInitialiser.START_PROGRESS);
                    return true;

                case AppInitialiser.PROGRESS_ERROR_FLAG:
                    mCompletedScanTaskCount = SCAN_TASK_COUNT;
                    mActivityWifiBinding.circleProgressViewWifi.setProgress(AppInitialiser.END_PROGRESS);
                    playAnimationAfterProgress();
                    return true;

                default:
                    LogUtils.w("Received abnormal handler message flag (" + message.what + "). Some errors might occur.");
                    return false;
            } // end switch-case
        });

        // hide all widgets except the toolbar and the circle progress view
        mActivityWifiBinding.imageViewWifi.setVisibility(View.INVISIBLE);
        mActivityWifiBinding.textViewWifiRationale.setVisibility(View.INVISIBLE);
        mActivityWifiBinding.linearLayoutWifiResults.setVisibility(View.INVISIBLE);

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
                    finish();
                } // end if...else
                break;

            case WIFI_CONFIGURATION_REQUEST:
                mActivityWifiBinding.roundButtonWifiAction.setText(R.string.roundButtonAction_normal);
                break;

            default:
                LogUtils.w("Received abnormal request code to identify who the results came from (" + requestCode + "). Some errors might occur.");
        } // end switch-case
    } // end method onActivityResult

    /**
     * Confirm stopping checking Wi-Fi security when the back button is pressed and the scan is not completed.
     */
    @Override
    public void onBackPressed()
    {
        confirmBeforeLeaving();
    } // end method onBackPressed

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

        if (mDialogueScanStopConfirmation != null)
            mDialogueScanStopConfirmation.dismiss();

        if (mDialogueIssueDisregardConfirmation != null)
            mDialogueIssueDisregardConfirmation.dismiss();

        if (mDialogueLocationServicesStatusWarning != null)
            mDialogueLocationServicesStatusWarning.dismiss();

        if (mDialoguePermissionWarning != null)
            mDialoguePermissionWarning.dismiss();

        mExecutorServiceScan.shutdownNow();
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
            confirmBeforeLeaving();
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
                finish();
            } // end if...else
        } // end if
    } // end method onRequestPermissionsResult

    // show a confirmation dialogue when necessary before finishing this activity
    private void confirmBeforeLeaving()
    {
        if (mCompletedScanTaskCount < SCAN_TASK_COUNT)
            mDialogueScanStopConfirmation = new MaterialDialog.Builder(this)
                    .backgroundColor(getColor(R.color.card_backgroundColour))
                    .icon(new IconicsDrawable(this)
                            .icon(Ionicons.Icon.ion_information_circled)
                            .color(new IconicsColorInt(getColor(R.color.colourInfo)))
                            .size(new IconicsSizeDp(AppInitialiser.DIALOGUE_ICON_SIZE)))
                    .title(R.string.dialogueScanStopConfirmation_title)
                    .titleColor(getColor(R.color.primaryTextColour))
                    .content(R.string.wifi_dialogueScanStopConfirmation_content)
                    .contentColor(getColor(R.color.contentTextColour))
                    .positiveText(R.string.dialogueScanStopConfirmation_positiveText)
                    .positiveColor(getColor(R.color.colourInfo))
                    .onPositive((MaterialDialog dialogue, DialogAction which) ->
                    {
                        // stop checking Wi-Fi security if the user clicks the confirmation button when the scan is not completed
                        if (mCompletedScanTaskCount < SCAN_TASK_COUNT)
                        {
                            LogUtils.i("User chose to stop checking Wi-Fi security.");
                            finish();
                        } // end if
                    })
                    .negativeText(R.string.dialogue_defaultNegativeText)
                    .negativeColor(getColor(R.color.colourInfo))
                    .cancelable(false)
                    .show(); // show a confirmation dialogue asking whether to stop checking Wi-Fi security when the scan is not completed
        else
        {
            if (mFinalResult == AppInitialiser.FinalResults.FAIL)
                mDialogueIssueDisregardConfirmation = new MaterialDialog.Builder(this)
                        .backgroundColor(getColor(R.color.card_backgroundColour))
                        .icon(new IconicsDrawable(this)
                                .icon(Ionicons.Icon.ion_information_circled)
                                .color(new IconicsColorInt(getColor(R.color.colourInfo)))
                                .size(new IconicsSizeDp(AppInitialiser.DIALOGUE_ICON_SIZE)))
                        .title(R.string.dialogueIssueDisregardConfirmation_title)
                        .titleColor(getColor(R.color.primaryTextColour))
                        .content(R.string.wifi_dialogueIssueDisregardConfirmation_content)
                        .contentColor(getColor(R.color.contentTextColour))
                        .positiveText(R.string.dialogueIssueDisregardConfirmation_positiveText)
                        .positiveColor(getColor(R.color.colourInfo))
                        .onPositive((MaterialDialog dialogue, DialogAction which) ->
                        {
                            LogUtils.i("User chose to disregard Wi-Fi security issues.");
                            finish();
                        })
                        .negativeText(R.string.dialogue_defaultNegativeText)
                        .negativeColor(getColor(R.color.colourInfo))
                        .cancelable(false)
                        .show(); // show a confirmation dialogue asking whether to disregard Wi-Fi security issues
            else
                finish();
        } // end if...else
    } // end method confirmBeforeLeaving

    // get Wi-Fi security scan results
    private void getWifiResults(boolean isFirstScan)
    {
        if (isFirstScan)
        {
            mCommonListItemViewChecklistSecurityType = mActivityWifiBinding.groupListViewWifiResults.createItemView(getString(R.string.wifi_checklist_securityType));
            mCommonListItemViewChecklistConnectivity = mActivityWifiBinding.groupListViewWifiResults.createItemView(getString(R.string.wifi_checklist_connectivity));
            mCommonListItemViewChecklistDns = mActivityWifiBinding.groupListViewWifiResults.createItemView(getString(R.string.wifi_checklist_dns));
            mCommonListItemViewChecklistSsl = mActivityWifiBinding.groupListViewWifiResults.createItemView(getString(R.string.wifi_checklist_ssl));
            mCommonListItemViewInfoSsid = mActivityWifiBinding.groupListViewWifiResults.createItemView(getString(R.string.wifi_info_ssid));
            mCommonListItemViewInfoSecurity = mActivityWifiBinding.groupListViewWifiResults.createItemView(getString(R.string.wifi_info_security));
            mCommonListItemViewInfoFrequency = mActivityWifiBinding.groupListViewWifiResults.createItemView(getString(R.string.wifi_info_frequency));
            mCommonListItemViewInfoSignalStrength = mActivityWifiBinding.groupListViewWifiResults.createItemView(getString(R.string.wifi_info_signalStrength));
            mCommonListItemViewInfoLinkSpeed = mActivityWifiBinding.groupListViewWifiResults.createItemView(getString(R.string.wifi_info_linkSpeed));
            mCommonListItemViewInfoBssid = mActivityWifiBinding.groupListViewWifiResults.createItemView(getString(R.string.wifi_info_bssid));
            mCommonListItemViewInfoIp = mActivityWifiBinding.groupListViewWifiResults.createItemView(getString(R.string.wifi_info_ip));
            mCommonListItemViewInfoGateway = mActivityWifiBinding.groupListViewWifiResults.createItemView(getString(R.string.wifi_info_gateway));
            mCommonListItemViewInfoSubnetMask = mActivityWifiBinding.groupListViewWifiResults.createItemView(getString(R.string.wifi_info_subnetMask));
            mCommonListItemViewInfoDns = mActivityWifiBinding.groupListViewWifiResults.createItemView(getString(R.string.wifi_info_dns));
        }
        else
        {
            mCompletedScanTaskCount = 0; // initialise the number of completed scan tasks
            mFinalResult = null;
        } // end if...else

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
        AppInitialiser.FinalResults securedSecurityType, internetConnection, securedDns, securedSsl;

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
                securedSecurityType = AppInitialiser.FinalResults.PASS;

                runOnUiThread(() -> imageViewChecklistSecurityType.setIcon(drawablePass));
                LogUtils.i("Secured security type.");
            }
            else if (securityTypeAndLabel[1].equals(WifiUtils.UNSECURED_SECURITY_TYPE))
            {
                securedSecurityType = AppInitialiser.FinalResults.FAIL;

                runOnUiThread(() -> imageViewChecklistSecurityType.setIcon(drawableFail));
                LogUtils.w("Unsecured security type.");
            }
            else
            {
                securedSecurityType = AppInitialiser.FinalResults.UNKNOWN;

                runOnUiThread(() -> imageViewChecklistSecurityType.setIcon(drawableUnknown));
                LogUtils.w("Cannot tell whether the security type is secured/unsecured. Some errors might occur.");
            } // end nested if...else

            mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INCREMENT_FLAG); // complete Scan Task 11

            ExecutorService executorServiceInternetConnection = Executors.newSingleThreadExecutor();
            executorServiceInternetConnection.execute(() -> mHasInternetConnection = wifiUtils.hasInternetConnection());
            executorServiceInternetConnection.shutdown();

            if (!executorServiceInternetConnection.awaitTermination(THREAD_TASK_TIMEOUT, TimeUnit.MINUTES))
                executorServiceInternetConnection.shutdownNow();

            if (mHasInternetConnection)
            {
                internetConnection = AppInitialiser.FinalResults.PASS;

                runOnUiThread(() -> imageViewChecklistConnectivity.setIcon(drawablePass));
                mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INCREMENT_FLAG); // complete Scan Task 12

                ExecutorService executorServiceParallelTasks = Executors.newFixedThreadPool(AppInitialiser.CPU_CORE_COUNT + 1 < PARALLEL_TASK_COUNT ? AppInitialiser.CPU_CORE_COUNT : PARALLEL_TASK_COUNT);
                executorServiceParallelTasks.execute(() -> mIsSecuredDns = wifiUtils.isSecuredDns());
                executorServiceParallelTasks.execute(() -> mIsSecuredSsl = wifiUtils.isSecuredSsl());
                executorServiceParallelTasks.shutdown();

                if (!executorServiceParallelTasks.awaitTermination(THREAD_TASK_TIMEOUT, TimeUnit.MINUTES))
                    executorServiceParallelTasks.shutdownNow();

                if (mIsSecuredDns)
                {
                    securedDns = AppInitialiser.FinalResults.PASS;

                    runOnUiThread(() -> imageViewChecklistDns.setIcon(drawablePass));
                }
                else
                {
                    securedDns = AppInitialiser.FinalResults.FAIL;

                    runOnUiThread(() -> imageViewChecklistDns.setIcon(drawableFail));
                } // end if...else

                mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INCREMENT_FLAG); // complete Scan Task 13

                if (mIsSecuredSsl)
                {
                    securedSsl = AppInitialiser.FinalResults.PASS;

                    runOnUiThread(() -> imageViewChecklistSsl.setIcon(drawablePass));
                }
                else
                {
                    securedSsl = AppInitialiser.FinalResults.FAIL;

                    runOnUiThread(() -> imageViewChecklistSsl.setIcon(drawableFail));
                } // end if...else

                mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INCREMENT_FLAG); // complete Scan Task 14
            }
            else
            {
                internetConnection = AppInitialiser.FinalResults.FAIL;
                mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INCREMENT_FLAG); // complete Scan Task 12

                securedDns = AppInitialiser.FinalResults.UNKNOWN;
                LogUtils.w("DNS security: unknown");
                mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INCREMENT_FLAG); // complete Scan Task 13

                securedSsl = AppInitialiser.FinalResults.UNKNOWN;
                LogUtils.w("SSL security: unknown");
                mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INCREMENT_FLAG); // complete Scan Task 14

                runOnUiThread(() ->
                {
                    imageViewChecklistConnectivity.setIcon(drawableFail);
                    imageViewChecklistDns.setIcon(drawableUnknown);
                    imageViewChecklistSsl.setIcon(drawableUnknown);
                });
            } // end if...else

            if (securedSecurityType == AppInitialiser.FinalResults.PASS
                    && internetConnection == AppInitialiser.FinalResults.PASS
                    && securedDns == AppInitialiser.FinalResults.PASS
                    && securedSsl == AppInitialiser.FinalResults.PASS)
            {
                mFinalResult = AppInitialiser.FinalResults.PASS;

                runOnUiThread(() ->
                {
                    mActivityWifiBinding.imageViewWifi.setIcon(new IconicsDrawable(this)
                            .icon(Ionicons.Icon.ion_checkmark_circled)
                            .color(new IconicsColorInt(getColor(R.color.colourPass)))
                            .size(new IconicsSizeDp(AppInitialiser.FINAL_RESULT_ICON_SIZE)));
                    mActivityWifiBinding.textViewWifiRationale.setText(R.string.textViewRationale_pass);
                    mActivityWifiBinding.roundButtonWifiAction.setText(R.string.roundButtonAction_normal);
                });
            }
            else if ((securedSecurityType == AppInitialiser.FinalResults.FAIL
                    || securedDns == AppInitialiser.FinalResults.FAIL
                    || securedSsl == AppInitialiser.FinalResults.FAIL))
            {
                mFinalResult = AppInitialiser.FinalResults.FAIL;

                runOnUiThread(() ->
                {
                    mActivityWifiBinding.imageViewWifi.setIcon(new IconicsDrawable(this)
                            .icon(Ionicons.Icon.ion_android_alert)
                            .color(new IconicsColorInt(Color.RED))
                            .size(new IconicsSizeDp(AppInitialiser.FINAL_RESULT_ICON_SIZE)));
                    mActivityWifiBinding.textViewWifiRationale.setText(R.string.wifi_textViewRationale_fail_default);
                    mActivityWifiBinding.roundButtonWifiAction.setText(R.string.wifi_roundButtonAction_fail);
                });
            }
            /*
             * execute this part if the connected Wi-Fi does not have unknown security type and Internet connection (internetConnection == WifiResults.FAIL);
             * the condition in the parentheses above is not included because it is always true at this point;
             * the reason is that the results of DNS and SSL security are unknown only when there is Internet connection;
             * therefore, there is only 1 situation suitable for this part (secured security type, available Internet connection, and unknown DNS and SSL security)
             */
            else if (securedSecurityType != AppInitialiser.FinalResults.UNKNOWN)
            {
                mFinalResult = AppInitialiser.FinalResults.FAIL;

                runOnUiThread(() ->
                {
                    mActivityWifiBinding.imageViewWifi.setIcon(new IconicsDrawable(this)
                            .icon(Ionicons.Icon.ion_android_alert)
                            .color(new IconicsColorInt(Color.RED))
                            .size(new IconicsSizeDp(AppInitialiser.FINAL_RESULT_ICON_SIZE)));
                    mActivityWifiBinding.textViewWifiRationale.setText(R.string.wifi_textViewRationale_fail_connectivity);
                    mActivityWifiBinding.roundButtonWifiAction.setText(R.string.wifi_roundButtonAction_fail);
                });
            }
            else
            {
                mFinalResult = AppInitialiser.FinalResults.UNKNOWN;

                runOnUiThread(() ->
                {
                    mActivityWifiBinding.imageViewWifi.setIcon(drawableBigUnknown);
                    mActivityWifiBinding.textViewWifiRationale.setText(R.string.textViewRationale_unknown_appError);
                    mActivityWifiBinding.roundButtonWifiAction.setText(R.string.roundButtonAction_unknown);
                });
            } // end nested if...else

            LogUtils.i("Successfully finish checking Wi-Fi security.");
            mHandlerWifi.sendEmptyMessage(AppInitialiser.PROGRESS_INCREMENT_FLAG); // complete Scan Task 15
        }
        catch (InterruptedException | NetworkErrorException e)
        {
            if (e instanceof InterruptedException)
            {
                LogUtils.e("The scan thread has been interrupted. An exception occurred (" + e.getMessage() + "). This might be caused by stopping checking Wi-Fi security.");
                LogUtils.e(e);
            }
            else
            {
                LogUtils.e("Failed to check Wi-Fi security. An exception occurred (" + e.getMessage() + ").");
                LogUtils.e(e);
                runOnUiThread(() ->
                {
                    mActivityWifiBinding.imageViewWifi.setIcon(drawableBigUnknown);
                    mActivityWifiBinding.textViewWifiRationale.setText(R.string.wifi_textViewRationale_unknown_connectivity);
                    mActivityWifiBinding.roundButtonWifiAction.setText(R.string.wifi_roundButtonAction_fail);
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

        View.OnLongClickListener onLongClickListenerWifiInfo = view ->
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
                        .setSeparatorDrawableRes(R.drawable.list_item_background_selector,
                                R.drawable.list_item_background_selector,
                                R.drawable.list_item_background_selector,
                                R.drawable.list_item_background_selector)
                        .addItemView(mCommonListItemViewChecklistSecurityType, null)
                        .addItemView(mCommonListItemViewChecklistConnectivity, null)
                        .addItemView(mCommonListItemViewChecklistDns, null)
                        .addItemView(mCommonListItemViewChecklistSsl, null)
                        .addTo(mActivityWifiBinding.groupListViewWifiResults);
                XUIGroupListView.newSection(this)
                        .setTitle(getString(R.string.wifi_info_title))
                        .setDescription(getString(R.string.wifi_info_description))
                        .addTo(mActivityWifiBinding.groupListViewWifiResults);
                XUIGroupListView.newSection(this)
                        .setSeparatorDrawableRes(R.drawable.list_item_background_selector,
                                R.drawable.list_item_background_selector,
                                R.drawable.list_item_background_selector,
                                R.drawable.list_item_background_selector)
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
                        .addTo(mActivityWifiBinding.groupListViewWifiResults);
            } // end if
        });
    } // end method getWifiResults

    // get the status of Location Services
    private LocationServicesStatus getLocationServicesStatus()
    {
        LocationManager locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);

        if (locationManager != null)
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                return LocationServicesStatus.ENABLED;
            else
                return LocationServicesStatus.DISABLED;

        LogUtils.w("Failed to get the status of Location Services. Some errors might occur.");
        return LocationServicesStatus.UNKNOWN;
    } // end method getLocationServicesStatus

    // play animation to hide the circle progress view and to display Wi-Fi security scan results
    private void playAnimationAfterProgress()
    {
        ViewUtils.fadeOut(mActivityWifiBinding.circleProgressViewWifi, mAnimationDuration, null);
        (new Handler()).postDelayed(() ->
        {
            ViewUtils.fadeIn(mActivityWifiBinding.imageViewWifi, mAnimationDuration, null);
            ViewUtils.fadeIn(mActivityWifiBinding.textViewWifiRationale, mAnimationDuration, null);
            ViewUtils.slideIn(mActivityWifiBinding.linearLayoutWifiResults, mAnimationDuration, null, ViewUtils.Direction.BOTTOM_TO_TOP);
            SystemBarThemeUtils.changeNavigationBarTheme(this, mConfiguration, getColor(R.color.card_backgroundColour), false);
            mActivityWifiBinding.roundButtonWifiAction.setEnabled(true);
        }, mAnimationDuration / 2);
    } // end method playAnimationAfterProgress

    // show a Location Services status warning dialogue to ask for enabling Location Services
    private void showLocationServicesStatusWarningDialogue()
    {
        mDialogueLocationServicesStatusWarning = new MaterialDialog.Builder(this)
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
                    finish();
                })
                .cancelable(false)
                .show();
        LogUtils.w("Location Services is not enabled.");
    } // end method showLocationServicesStatusWarningDialogue

    // show a permission warning dialogue to give a brief rationale of the permission(s) to enable checking Wi-Fi security
    private void showPermissionWarningDialogue()
    {
        String[] permissionArray;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            permissionArray = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        else
            permissionArray = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION};

        mDialoguePermissionWarning = new MaterialDialog.Builder(this)
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
                .onPositive((MaterialDialog dialogue, DialogAction which) -> ActivityCompat.requestPermissions(this, permissionArray, PERMISSION_REQUEST_WIFI))
                .positiveColor(getColor(R.color.colourInfo))
                .cancelable(false)
                .show();
        LogUtils.w("The permission(s) to enable checking Wi-Fi security is/are not granted.");
    } // end method showPermissionWarningDialogue
} // end class ActivityWifi