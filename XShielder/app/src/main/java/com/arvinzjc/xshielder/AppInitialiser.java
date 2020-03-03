/*
 * @Description: a necessary class for initialising the application
 * @Version: 1.4.0.20200302
 * @Author: Arvin Zhao
 * @Date: 2020-01-24 13:08:14
 * @Last Editors: Arvin Zhao
 * @LastEditTime : 2020-03-02 14:12:45
 */

package com.arvinzjc.xshielder;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.apkfuns.log2file.LogFileEngineFactory;
import com.apkfuns.logutils.Log2FileConfig;
import com.apkfuns.logutils.LogLevel;
import com.apkfuns.logutils.LogUtils;
import com.arvinzjc.xshielder.utils.AppUtils;
import com.xuexiang.xui.XUI;

public class AppInitialiser extends Application
{
    public enum FinalResults
    {
        PASS,
        FAIL,
        UNKNOWN
    } // end enum FinalResults

    /**
     * The folder under an app-specific file directory for storing log files.
     */
    public static final String LOG_FILE_FOLDER = "/logs/";

    /**
     * The extension of a log file.
     */
    public static final String LOG_FILE_EXTENSION = ".log";

    /**
     * The key of the switch preference for enabling logging.
     */
    public static final String ENABLING_LOGGING_KEY = "switchPreferenceEnablingLogging";

    /**
     * The start of a circle progress view.
     */
    public static final int START_PROGRESS = 0;

    /**
     * The end of a circle progress view.
     */
    public static final int END_PROGRESS = 100;

    /**
     * The handler flag of increasing the progress because of a new completed scan task.
     */
    public static final int PROGRESS_INCREMENT_FLAG = 1;

    /**
     * The handler flag of initialising the progress.
     */
    public static final int PROGRESS_INITIALISATION_FLAG = 0;

    /**
     * The handler flag of directly finishing the progress usually because of some errors.
     */
    public static final int PROGRESS_ERROR_FLAG = -1;

    /**
     * The size (unit: dp) of the right icon in a toolbar.
     */
    public static final int TOOLBAR_RIGHT_ICON_SIZE = 18;

    /**
     * The size (unit: dp) of the icon on the left of the title of a dialogue.
     */
    public static final int DIALOGUE_ICON_SIZE = 24;

    /**
     * The size (unit: dp) of an icon indicating the final result.
     */
    public static final int FINAL_RESULT_ICON_SIZE = 150;

    /**
     * The size (unit: dp) of an icon indicating the result of an item in the checklist.
     */
    public static final int RESULT_ICON_SIZE = 15;

    /**
     * The size (unit: px) of an icon in the app list.
     */
    public static final int APP_LIST_ICON_SIZE = 100;

    /**
     * The default size (unit: sp) of content text.
     */
    public static final int DEFAULT_CONTENT_TEXT_SIZE = 16;

    /**
     * The number of CPU cores.
     */
    public static final int CPU_CORE_COUNT = Runtime.getRuntime().availableProcessors();

    private static final String LOG_TAG_PREFIX = "XShielder"; // the tag prefix of logs
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final int LOG_FILE_LIFETIME = 7; // each log file has a lifetime of 7 days

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

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean enablingLoggingValue = sharedPreferences.getBoolean(ENABLING_LOGGING_KEY, true); // the value of the switch preference for enabling logging

        if (!sharedPreferences.contains(ENABLING_LOGGING_KEY))
            sharedPreferences.edit().putBoolean(ENABLING_LOGGING_KEY, enablingLoggingValue).apply();

        Log2FileConfig log2FileConfig = LogUtils.getLog2FileConfig();
        String logFilePath = AppUtils.getAppFilesDirectory(this) + LOG_FILE_FOLDER; // the absolute path to the directory storing log files of this app

        if (enablingLoggingValue)
            log2FileConfig.configLog2FileEnable(true);
        else
            log2FileConfig.configLog2FileEnable(false);

        log2FileConfig
                .configLog2FilePath(logFilePath)
                .configLog2FileNameFormat("%d{" + DATE_FORMAT + "}" + LOG_FILE_EXTENSION)
                .configLog2FileLevel(LogLevel.TYPE_INFO)
                .configLogFileEngine(new LogFileEngineFactory(this)); // configure LogUtils to enable keeping INFO/WARNING/ERROR logs in local files

        LogUtils.i("\n------------------------------");

        File logFileFolder = new File(logFilePath);

        if (logFileFolder.exists())
        {
            File[] logFiles = logFileFolder.listFiles();

            if (logFiles != null && logFiles.length > 0)
            {
                List<String> logFileNameList = new ArrayList<>(); // store log file names without the extension

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
                                File expiredLog = new File(logFilePath, logFileName + LOG_FILE_EXTENSION);

                                if (expiredLog.exists())
                                    if (expiredLog.delete())
                                        LogUtils.i("Successfully delete the expired log file \"" + logFileName + LOG_FILE_EXTENSION + "\".");
                                    else
                                        LogUtils.w("Failed to delete the expired log file \"" + logFileName + LOG_FILE_EXTENSION + "\". Some errors might occur.");
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
                        LogUtils.e("An exception occurred when the app parsed the log file name \"" + logFileName + "\" to get its birthday (" + e.getMessage() + ").");
                        LogUtils.e(e);
                        isAbnormalLog = true;
                    } // end try...catch

                    if (isAbnormalLog)
                    {
                        File abnormalLog = new File(logFilePath, logFileName + LOG_FILE_EXTENSION);

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
        } // end if

        LogUtils.i("The app started and initialised. Version: " + AppUtils.getAppVersionName(this));
    } // end method onCreate
} // end class AppInitialiser