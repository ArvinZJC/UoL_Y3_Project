/*
 * @Description: a necessary class for initialising the application
 * @Version: 1.3.2.20200208
 * @Author: Arvin Zhao
 * @Date: 2020-01-24 13:08:14
 * @Last Editors: Arvin Zhao
 * @LastEditTime : 2020-02-08 14:12:45
 */

package com.arvinzjc.xshielder;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;

import com.apkfuns.log2file.LogFileEngineFactory;
import com.apkfuns.logutils.LogLevel;
import com.apkfuns.logutils.LogUtils;
import com.xuexiang.xui.XUI;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AppInitialiser extends Application
{
    private static final String LOG_TAG_PREFIX = "XShielder"; // the tag prefix of logs
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String LOG_FILE_EXTENSION = ".log";
    private static final int LOG_FILE_LIFETIME = 7; // each log file has a lifetime of 7 days

    /**
     * The code of the permission request appeared on the activity of the Wi-Fi security shield.
     */
    public static final int PERMISSION_REQUEST_WIFI = 0;

    /**
     * The size (unit: dp) of the dialog icon.
     */
    public static final int DIALOG_ICON_SIZE = 24;

    /**
     * The size (unit: dp) of the right icon in a toolbar.
     */
    public static final int TOOLBAR_RIGHT_ICON_SIZE = 18;

    /**
     * The size (unit: dp) of the icon indicating the final result.
     */
    public static final int FINAL_RESULT_ICON_SIZE = 150;

    /**
     * The size (unit: dp) of the icon indicating the result of an item in the checklist.
     */
    public static final int RESULT_ICON_SIZE = 15;

    /**
     * The number of CPU cores.
     */
    public static final int CPU_CORE_COUNT = Runtime.getRuntime().availableProcessors();

    @Override
    public void onCreate()
    {
        super.onCreate();
        XUI.init(this); // initialise XUI
        XUI.debug(BuildConfig.DEBUG); // enable the debug mode of XUI when the app is in the debug mode
        LogUtils.getLogConfig()
                .configTagPrefix(LOG_TAG_PREFIX)
                .configShowBorders(false)
                .configLevel(LogLevel.TYPE_INFO)
                .configFormatTag("%t %c{-3}"); // configure LogUtils to enable generating advanced INFO/WARNING/ERROR logs in the logcat

        File fileExternalFilesDirectory = this.getExternalFilesDir(null); // a File object containing the absolute path to an application-specific directory named "files" on the primary shared/external storage device
        String logFilesPath; // the absolute path to the directory storing log files of the application

        if ((Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) || !Environment.isExternalStorageRemovable()) && fileExternalFilesDirectory != null)
            logFilesPath = fileExternalFilesDirectory.getPath() + "/logs/";
        else
        {
            LogUtils.w("Shared storage is not currently available. Logs will be stored under an app-specific directory named \"files\" on the filesystem.");
            logFilesPath = this.getFilesDir().getPath() + "/logs/";
        } // end if...else

        LogUtils.getLog2FileConfig()
                .configLog2FileEnable(true)
                .configLog2FilePath(logFilesPath)
                .configLog2FileNameFormat("%d{" + DATE_FORMAT + "}" + LOG_FILE_EXTENSION)
                .configLog2FileLevel(LogLevel.TYPE_INFO)
                .configLogFileEngine(new LogFileEngineFactory(this)); // configure LogUtils to enable keeping INFO/WARNING/ERROR logs in local files

        File[] logFiles = new File(logFilesPath).listFiles();

        if (logFiles != null && logFiles.length > 0)
        {
            List<String> logFileNameList = new ArrayList<>(); // use to store log file names without the extension

            for (File logFile : logFiles)
                logFileNameList.add(logFile.getName().replace(LOG_FILE_EXTENSION, ""));

            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());

            for (int count = 0; count < logFiles.length; count++)
            {
                String logFileName = logFileNameList.get(count); // a log file name without the extension
                boolean isAbnormalLog = false;

                try
                {
                    Date logFileDate = dateFormat.parse(logFileName);

                    if (logFileDate != null)
                    {
                        long timeDifference = new Date().getTime() - logFileDate.getTime();
                        long dateDifference = TimeUnit.DAYS.convert(timeDifference, TimeUnit.MILLISECONDS);

                        if (dateDifference >= LOG_FILE_LIFETIME)
                        {
                            File outdatedLog = new File(logFilesPath, logFileName + LOG_FILE_EXTENSION);

                            if (outdatedLog.exists())
                            {
                                if (outdatedLog.delete())
                                    LogUtils.i("Successfully delete the outdated log file \"" + logFileName + LOG_FILE_EXTENSION + "\".");
                                else
                                    LogUtils.w("Failed to delete the outdated log file \"" + logFileName + LOG_FILE_EXTENSION + "\". Some errors might occur.");
                            } // end if
                        } // end if
                    }
                    else
                    {
                        LogUtils.w("Failed to parse the log file name \"" + logFileName + "\" to get its birthday. Some errors might occur.");
                        isAbnormalLog = true;
                    } // end if...else
                }
                catch (ParseException e)
                {
                    LogUtils.e("Exception occurred when the app parsed the log file name \"" + logFileName + "\" to get its birthday.");
                    LogUtils.e(e);
                    isAbnormalLog = true;
                } // end try...catch

                if (isAbnormalLog)
                {
                    File abnormalLog = new File(logFilesPath, logFileName + LOG_FILE_EXTENSION);

                    if (abnormalLog.exists())
                    {
                        if (abnormalLog.delete())
                            LogUtils.i("Successfully delete the abnormal log file \"" + logFileName + LOG_FILE_EXTENSION + "\".");
                        else
                            LogUtils.w("Failed to delete the abnormal log file \"" + logFileName + LOG_FILE_EXTENSION + "\". Some errors might occur.");
                    } // end if
                } // end if
            } // end for
        } // end if

        String versionName;

        try
        {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = packageInfo.versionName;
            LogUtils.i("\n------------------------------" +
                    "\nThe app started and initialised. Version: " + versionName);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            versionName = getResources().getString(R.string.unknownInfo);
            LogUtils.i("\n------------------------------" +
                    "\nThe app started and initialised. Version: " + versionName);
            LogUtils.e("Exception occurred when the app tried to get the version name.");
            LogUtils.e(e);
        } // end try...catch
    } // end method onCreate
} // end class AppInitialiser