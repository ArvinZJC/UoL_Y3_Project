/*
 * @Description: a class for the fragment of settings
 * @Version: 1.1.7.20200320
 * @Author: Arvin Zhao
 * @Date: 2020-03-02 19:37:54
 * @Last Editors: Arvin Zhao
 * @LastEditTime: 2020-03-20 19:41:26
 */

package com.arvinzjc.xshielder;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.apkfuns.logutils.Log2FileConfig;
import com.apkfuns.logutils.LogUtils;
import com.arvinzjc.xshielder.utils.AppUtils;
import com.developer.filepicker.model.DialogProperties;
import com.mikepenz.iconics.IconicsColorInt;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.IconicsSizeDp;
import com.mikepenz.iconics.typeface.library.ionicons.Ionicons;
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;

import java.io.File;

public class FragmentSettings extends PreferenceFragmentCompat
{
    /**
     * The code of the permission request appeared on the activity where this fragment is included.
     */
    public static final int PERMISSION_REQUEST_SETTINGS = 0;

    /**
     * The customised file picker dialogue for viewing the log file list.
     */
    public FilePickerDialogue mFilePickerDialogueViewingLogFiles;

    private static final String FILE_PROVIDER_AUTHORITIES = "com.arvinzjc.xshielder.fileprovider",
            VIEWING_LOG_FILES_KEY = "preferenceViewingLogFiles",
            CLEARING_LOG_FILES_KEY = "preferenceClearingLogFiles",
            SENDING_FEEDBACK_KEY = "preferenceSendingFeedback",
            ABOUT_APP_KEY = "preferenceAboutApp",

            SOURCE_CODE_URL = "https://github.com/ArvinZJC/UoL_Y3_Project/tree/master/XShielder",
            FEEDBACK_EMAIL = "zjcarvin@outlook.com";

    private MaterialDialog mDialogueDisableLoggingConfirmation, mDialoguePermissionWarning, mDialogueClearingLogFilesConfirmation, mDialogueAboutApp;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        setPreferencesFromResource(R.xml.settings, rootKey);

        Context context = getContext();

        if (context != null)
        {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
            boolean enablingLoggingValue = sharedPreferences.getBoolean(AppInitialiser.ENABLING_LOGGING_KEY, true); // the value of the switch preference for enabling logging
            Log2FileConfig log2FileConfig = LogUtils.getLog2FileConfig();

            if (!sharedPreferences.contains(AppInitialiser.ENABLING_LOGGING_KEY))
            {
                sharedPreferencesEditor.putBoolean(AppInitialiser.ENABLING_LOGGING_KEY, enablingLoggingValue).apply();
                log2FileConfig.configLog2FileEnable(true);
            } // end if

            SwitchPreference switchPreferenceEnablingLogging = findPreference(AppInitialiser.ENABLING_LOGGING_KEY);

            if (switchPreferenceEnablingLogging != null)
            {
                if (enablingLoggingValue)
                    switchPreferenceEnablingLogging.setChecked(true);
                else
                    switchPreferenceEnablingLogging.setChecked(false);

                switchPreferenceEnablingLogging.setOnPreferenceChangeListener((Preference preference, Object newValue) ->
                {
                    if ((Boolean)newValue)
                    {
                        log2FileConfig.configLog2FileEnable((Boolean)newValue);
                        LogUtils.i("User chose to enable logging.");
                    }
                    else
                    {
                        mDialogueDisableLoggingConfirmation = new MaterialDialog.Builder(context)
                                .backgroundColor(context.getColor(R.color.card_backgroundColour))
                                .icon(new IconicsDrawable(context)
                                        .icon(Ionicons.Icon.ion_information_circled)
                                        .color(new IconicsColorInt(context.getColor(R.color.colourInfo)))
                                        .size(new IconicsSizeDp(AppInitialiser.DIALOGUE_ICON_SIZE)))
                                .title(R.string.settings_dialogueDisableLoggingConfirmation_title)
                                .titleColor(context.getColor(R.color.primaryTextColour))
                                .content(R.string.settings_dialogueDisableLoggingConfirmation_content)
                                .contentColor(context.getColor(R.color.contentTextColour))
                                .positiveText(R.string.settings_dialogueDisableLoggingConfirmation_positiveText)
                                .positiveColor(context.getColor(R.color.colourInfo))
                                .onPositive((MaterialDialog dialogue, DialogAction which) ->
                                {
                                    LogUtils.i("User chose to disable logging.");
                                    log2FileConfig.flushAsync();
                                    log2FileConfig.configLog2FileEnable((Boolean)newValue);
                                })
                                .negativeText(R.string.dialogue_defaultNegativeText)
                                .negativeColor(context.getColor(R.color.colourInfo))
                                .onNegative((MaterialDialog dialogue, DialogAction which) -> switchPreferenceEnablingLogging.setChecked(true))
                                .cancelable(false)
                                .show(); // show a confirmation dialogue asking whether to disable logging
                    } // end if...else

                    return true;
                });
            }
            else
                LogUtils.w("Failed to find the switch preference for enabling logging. Some errors might occur.");

            Preference preferenceViewingLogFiles = findPreference(VIEWING_LOG_FILES_KEY), preferenceClearingLogFiles = findPreference(CLEARING_LOG_FILES_KEY);
            String logFilePath = AppUtils.getAppFilesDirectory(context) + AppInitialiser.LOG_FILE_FOLDER; // the absolute path to the directory storing log files of this app
            File logFileFolder = new File(logFilePath);
            boolean isLogFileFolderExisted = logFileFolder.exists();
            File[] logFiles = logFileFolder.listFiles();

            if (preferenceViewingLogFiles != null && preferenceClearingLogFiles != null)
            {
                if (isLogFileFolderExisted)
                {
                    if (logFiles != null && logFiles.length > 0)
                    {
                        DialogProperties dialogueProperties = new DialogProperties();
                        dialogueProperties.root = logFileFolder;
                        dialogueProperties.extensions = new String[]{AppInitialiser.LOG_FILE_EXTENSION};

                        mFilePickerDialogueViewingLogFiles = new FilePickerDialogue(context, dialogueProperties);
                        mFilePickerDialogueViewingLogFiles.setTitle("Select a log file");
                        mFilePickerDialogueViewingLogFiles.setDialogSelectionListener(files ->
                        {
                            String logFileName = files[0].substring(files[0].lastIndexOf("/") + 1);
                            LogUtils.i("User chose to view a specified log file (" + logFileName + ").");

                            Intent intent = new Intent(Intent.ACTION_VIEW);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                            {
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                intent.setDataAndType(FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITIES, new File(files[0])), "text/plain");
                            }
                            else
                                intent.setDataAndType(Uri.fromFile(new File(files[0])), "text/plain");

                            try
                            {
                                context.startActivity(intent);
                            }
                            catch (ActivityNotFoundException e)
                            {
                                LogUtils.e("Failed to find a suitable app to view a specified log file (" + logFileName + "). An exception occurred.");
                                LogUtils.e(e);
                                Toast.makeText(context.getApplicationContext(), R.string.settings_toastViewingLogFilesFailed, Toast.LENGTH_SHORT).show(); // the application context is required to avoid any abnormal toast styles
                            } // end try...catch
                        });

                        preferenceViewingLogFiles.setSummary(getString(R.string.settings_preferenceViewingLogFiles_summary_existed) + logFilePath);
                        preferenceViewingLogFiles.setEnabled(true);
                        preferenceViewingLogFiles.setLayoutResource(R.layout.customised_preference);
                        preferenceViewingLogFiles.setOnPreferenceClickListener(preference ->
                        {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                            {
                                mDialoguePermissionWarning = new MaterialDialog.Builder(context)
                                        .backgroundColor(context.getColor(R.color.card_backgroundColour))
                                        .icon(new IconicsDrawable(context)
                                                .icon(Ionicons.Icon.ion_android_alert)
                                                .color(new IconicsColorInt(context.getColor(R.color.colourWarning)))
                                                .size(new IconicsSizeDp(AppInitialiser.DIALOGUE_ICON_SIZE)))
                                        .title(R.string.dialoguePermissionWarning_title)
                                        .titleColor(context.getColor(R.color.primaryTextColour))
                                        .content(R.string.settings_dialoguePermissionWarning_content)
                                        .contentColor(context.getColor(R.color.contentTextColour))
                                        .positiveText(R.string.dialogue_defaultPositiveText)
                                        .onPositive((MaterialDialog dialogue, DialogAction which) -> ((Activity)context).requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_SETTINGS))
                                        .positiveColor(context.getColor(R.color.colourInfo))
                                        .cancelable(false)
                                        .show();
                                LogUtils.w("The permission to view log files is not granted.");
                            }
                            else
                                mFilePickerDialogueViewingLogFiles.show();

                            return true;
                        });

                        preferenceClearingLogFiles.setSummary(R.string.settings_preferenceClearingLogFiles_summary);
                        preferenceClearingLogFiles.setEnabled(true);
                        preferenceClearingLogFiles.setLayoutResource(R.layout.customised_preference);
                        preferenceClearingLogFiles.setOnPreferenceClickListener(preference ->
                        {
                            mDialogueClearingLogFilesConfirmation = new MaterialDialog.Builder(context)
                                    .backgroundColor(context.getColor(R.color.card_backgroundColour))
                                    .icon(new IconicsDrawable(context)
                                            .icon(Ionicons.Icon.ion_information_circled)
                                            .color(new IconicsColorInt(context.getColor(R.color.colourInfo)))
                                            .size(new IconicsSizeDp(AppInitialiser.DIALOGUE_ICON_SIZE)))
                                    .title(R.string.settings_dialogueClearingLogFilesConfirmation_title)
                                    .titleColor(context.getColor(R.color.primaryTextColour))
                                    .content(R.string.settings_dialogueClearingLogFilesConfirmation_content)
                                    .contentColor(context.getColor(R.color.contentTextColour))
                                    .positiveText(R.string.settings_dialogueClearingLogFilesConfirmation_positiveText)
                                    .positiveColor(context.getColor(R.color.colourInfo))
                                    .onPositive((MaterialDialog dialogue, DialogAction which) ->
                                    {
                                        LogUtils.i("User chose to clear all log files.");

                                        File[] logFilesBeingCleared = logFileFolder.listFiles();

                                        if (logFilesBeingCleared != null && logFilesBeingCleared.length > 0)
                                        {
                                            boolean isCleared = true; // indicate if all log files are cleared

                                            for (File logFileBeingCleared : logFilesBeingCleared)
                                                if (logFileBeingCleared.exists())
                                                    if (!logFileBeingCleared.delete())
                                                    {
                                                        isCleared = false;
                                                        LogUtils.w("Failed to delete the log file \"" + logFileBeingCleared.getName() + "\". Some errors might occur.");
                                                    } // end if...else

                                            if (isCleared)
                                            {
                                                disableOperatingLogFiles(preferenceViewingLogFiles, preferenceClearingLogFiles);
                                                LogUtils.i("Successfully delete all log files.");
                                                Toast.makeText(context.getApplicationContext(), R.string.settings_toastLogFilesCleared, Toast.LENGTH_SHORT).show(); // the application context is required to avoid any abnormal toast styles
                                            } // end if
                                        } // end if
                                    })
                                    .negativeText(R.string.dialogue_defaultNegativeText)
                                    .negativeColor(context.getColor(R.color.colourInfo))
                                    .cancelable(false)
                                    .show(); // show a confirmation dialogue asking whether to clear all log files
                            return true;
                        });
                    }
                    else
                        disableOperatingLogFiles(preferenceViewingLogFiles, preferenceClearingLogFiles);
                }
                else
                    disableOperatingLogFiles(preferenceViewingLogFiles, preferenceClearingLogFiles);
            }
            else
                LogUtils.w("Failed to find the preference for viewing/clearing log files. Some errors might occur.");

            Preference preferenceSendingFeedback = findPreference(SENDING_FEEDBACK_KEY);

            if (preferenceSendingFeedback != null)
            {
                preferenceSendingFeedback.setOnPreferenceClickListener(preference ->
                {
                    try
                    {
                        startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + FEEDBACK_EMAIL)));
                    }
                    catch (ActivityNotFoundException e)
                    {
                        LogUtils.e("Failed to find a suitable mail app to send feedback. An exception occurred.");
                        LogUtils.e(e);
                        Toast.makeText(context.getApplicationContext(), R.string.settings_toastOpeningMailFailed, Toast.LENGTH_SHORT).show(); // the application context is required to avoid any abnormal toast styles
                    } // end try...catch

                    return true;
                });
            }
            else
                LogUtils.w("Failed to find the preference for sending feedback. Some errors might occur.");

            Preference preferenceAboutApp = findPreference(ABOUT_APP_KEY);

            if (preferenceAboutApp != null)
            {
                preferenceAboutApp.setSummary(context.getString(R.string.settings_preferenceAboutApp_summary) + AppUtils.getAppVersionName(context));
                preferenceAboutApp.setOnPreferenceClickListener(preference ->
                {
                    LogUtils.i("User chose to know about the app.");

                    Drawable drawableAppIcon = context.getDrawable(R.mipmap.ic_launcher_foreground);

                    if (drawableAppIcon == null)
                    {
                        LogUtils.w("Failed to get the app icon. Some errors might occur.");
                        drawableAppIcon = new IconicsDrawable(context)
                                .icon(Ionicons.Icon.ion_information_circled)
                                .color(new IconicsColorInt(context.getColor(R.color.colourInfo)))
                                .size(new IconicsSizeDp(AppInitialiser.DIALOGUE_ICON_SIZE));
                    } // end if

                    String openSourceText = context.getString(R.string.settings_dialogueAboutApp_content_part2);
                    SpannableString openSourceLinkText = new SpannableString(openSourceText);
                    openSourceLinkText.setSpan(
                            new ClickableSpan()
                            {
                                @Override
                                public void onClick(@NonNull View widget)
                                {
                                    try
                                    {
                                        LogUtils.i("User chose to view the source code of the app.");
                                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_CODE_URL)));
                                    }
                                    catch (ActivityNotFoundException e)
                                    {
                                        LogUtils.e("Failed to find a suitable browser to view the source code of the app. An exception occurred.");
                                        LogUtils.e(e);
                                        Toast.makeText(context.getApplicationContext(), R.string.toastOpeningLinkFailed, Toast.LENGTH_SHORT).show(); // the application context is required to avoid any abnormal toast styles
                                    } // end try...catch
                                } // end method onClick
                            },
                            0,
                            openSourceText.length(),
                            Spanned.SPAN_MARK_MARK);

                    mDialogueAboutApp = new MaterialDialog.Builder(context)
                            .backgroundColor(context.getColor(R.color.card_backgroundColour))
                            .icon(drawableAppIcon)
                            .title(context.getString(R.string.settings_dialogueAboutApp_title) + context.getString(R.string.app_name))
                            .titleColor(context.getColor(R.color.primaryTextColour))
                            .content(new SpannableStringBuilder()
                                    .append(context.getString(R.string.settings_dialogueAboutApp_content_part1))
                                    .append(openSourceLinkText)
                                    .append(context.getString(R.string.settings_dialogueAboutApp_content_part3)))
                            .contentColor(context.getColor(R.color.contentTextColour))
                            .positiveText(R.string.dialogue_defaultPositiveText)
                            .positiveColor(context.getColor(R.color.colourInfo))
                            .cancelable(true)
                            .build();

                    TextView dialogueTextViewAboutApp = mDialogueAboutApp.getContentView();

                    if (dialogueTextViewAboutApp != null)
                        dialogueTextViewAboutApp.setMovementMethod(LinkMovementMethod.getInstance());
                    else
                        LogUtils.w("Failed to get the text view of the info dialogue about the app. Some errors might occur.");

                    mDialogueAboutApp.show(); // show an info dialogue about the app
                    return true;
                });
            }
            else
                LogUtils.w("Failed to find the preference about the app. Some errors might occur.");
        }
        else
            LogUtils.w("Failed to get the context that the preference fragment is currently associated with. Some errors might occur.");
    } // end method onCreatePreferences

    /**
     * Perform some necessary tasks when destroying the preference fragment.
     */
    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (mDialogueDisableLoggingConfirmation != null)
            mDialogueDisableLoggingConfirmation.dismiss();

        if (mDialoguePermissionWarning != null)
            mDialoguePermissionWarning.dismiss();

        if (mFilePickerDialogueViewingLogFiles != null)
            mFilePickerDialogueViewingLogFiles.dismiss();

        if (mDialogueClearingLogFilesConfirmation != null)
            mDialogueClearingLogFilesConfirmation.dismiss();

        if (mDialogueAboutApp != null)
            mDialogueAboutApp.dismiss();
    } // end method onPause

    // set the style of the disabled preferences for viewing and clearing log files
    private void disableOperatingLogFiles(@NonNull Preference preferenceViewingLogFiles, @NonNull Preference preferenceClearingLogFiles)
    {
        preferenceViewingLogFiles.setSummary(R.string.settings_preferenceViewingLogFiles_summary_none);
        preferenceViewingLogFiles.setEnabled(false);
        preferenceViewingLogFiles.setLayoutResource(R.layout.customised_preference_disabled);

        preferenceClearingLogFiles.setSummary(null);
        preferenceClearingLogFiles.setEnabled(false);
        preferenceClearingLogFiles.setLayoutResource(R.layout.customised_preference_disabled);
    } // end method disableOperatingLogFiles
} // end class FragmentSettings