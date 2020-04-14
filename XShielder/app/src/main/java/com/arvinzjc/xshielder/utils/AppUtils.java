/*
 * @Description: utilities for supporting some app and file operations
 * @Version: 1.0.6.20200414
 * @Author: Jichen Zhao
 * @Date: 2020-02-28 13:31:06
 * @Last Editors: Jichen Zhao
 * @LastEditTime: 2020-04-14 13:32:18
 */

package com.arvinzjc.xshielder.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;

import androidx.annotation.NonNull;

import com.apkfuns.logutils.LogUtils;
import com.arvinzjc.xshielder.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class AppUtils
{
    /**
     * Copy the data of the source APK file of a specified non-system app to a new APK file.
     * @param nonSystemAppInfo info of the specified non-system app
     * @param newApk a File object containing the absolute path of a new APK file of a specified non-system app
     * @throws IOException this exception is thrown when an I/O exception of some sort has occurred
     */
    public static void copyApk(@NonNull ApplicationInfo nonSystemAppInfo, @NonNull File newApk) throws IOException
    {
        InputStream inputStream = new FileInputStream(new File(nonSystemAppInfo.sourceDir));
        OutputStream outputStream = new FileOutputStream(newApk);
        byte[] data = new byte[1024];
        int byteCount; // the number of bytes to write

        // copy the data of the source APK file of a specified non-system app and paste it to the new APK file
        while ((byteCount = inputStream.read(data)) > 0)
            outputStream.write(data, 0, byteCount);

        inputStream.close();
        outputStream.close();
    } // end method copyApk

    /**
     * Clear the files in the folder containing APKs for scanning.
     * @param apkFolder a File object containing the directory of the folder containing APKs for scanning
     */
    public static void clearApkFolder(@NonNull File apkFolder)
    {
        File[] oldFiles = apkFolder.listFiles();

        if (oldFiles != null && oldFiles.length > 0)
            for (File oldFile : oldFiles)
                if (oldFile.isFile())
                    if (!oldFile.delete())
                        LogUtils.w("Failed to delete an old file (" + oldFile.getName() + "). Some errors might occur.");
    } // end method clearApkFolder

    /**
     * Get the absolute path to the app-specific cache directory on the external storage device or the filesystem (when the external storage device is unavailable or
     * removable.
     * @param context global info about an app environment
     * @return the absolute path to the app-specific cache directory on the external storage device or the filesystem
     */
    @NonNull
    public static String getAppCacheDirectory(@NonNull Context context)
    {
        File externalCacheDirectory = context.getExternalCacheDir(); // a File object containing the absolute path to the app-specific cache directory on the external storage device

        if (externalCacheDirectory != null && (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) || !Environment.isExternalStorageRemovable()))
            return externalCacheDirectory.getPath();

        LogUtils.i("Unavailable/Removable external storage device. Turn to the app-specific cache directory on the filesystem.");
        return context.getCacheDir().getPath();
    } // end method getAppCacheDirectory

    /**
     * Get the absolute path to the app-specific files directory on the external storage device or the filesystem (when the external storage device is unavailable or
     * removable.
     * @param context global info about an app environment
     * @return the absolute path to the app-specific files directory on the external storage device or the filesystem
     */
    @NonNull
    public static String getAppFilesDirectory(@NonNull Context context)
    {
        File externalFilesDirectory = context.getExternalFilesDir(null); // a File object containing the absolute path to the app-specific files directory on the external storage device

        if (externalFilesDirectory != null && (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) || !Environment.isExternalStorageRemovable()))
            return externalFilesDirectory.getPath();

        LogUtils.i("Unavailable/Removable external storage device. Turn to the app-specific files directory on the filesystem.");
        return context.getFilesDir().getPath();
    } // end method getAppFilesDirectory

    /**
     * Get the version name of this app.
     * @param context global info about an app environment
     * @return the version name of this app
     */
    public static String getAppVersionName(@NonNull Context context)
    {
        try
        {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            LogUtils.e("An exception occurred when the app tried to get the version name (" + e.getMessage() + ").");
            LogUtils.e(e);
            return context.getResources().getString(R.string.unknownInfo);
        } // end try...catch
    } // end method getAppVersionName

    /**
     * Get the info list of installed non-system apps.
     * @param context global info about an app environment
     * @param packageManager a package manager
     * @return the info list of installed non-system apps
     */
    @NonNull
    public static ArrayList<ApplicationInfo> getNonSystemAppInfoList(@NonNull Context context, @NonNull PackageManager packageManager)
    {
        ArrayList<ApplicationInfo> nonSystemAppInfoList = new ArrayList<>();

        for (ApplicationInfo appInfo : packageManager.getInstalledApplications(0))
            // add the info of all third-party apps except this app to the info list of non-system apps
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0 && !appInfo.packageName.equals(context.getApplicationInfo().packageName))
                nonSystemAppInfoList.add(appInfo);

        return nonSystemAppInfoList;
    } // end method getNonSystemAppInfoList

    /**
     * Check if the specified app is installed.
     * @param context global info about an app environment
     * @param packageManager a package manager
     * @param appInfo the info of the specified app
     * @return true if the specified app is installed; otherwise, false
     */
    public static boolean isInstalled(@NonNull Context context, @NonNull PackageManager packageManager, @NonNull ApplicationInfo appInfo)
    {
        ArrayList<ApplicationInfo> nonSystemAppInfoList = getNonSystemAppInfoList(context, packageManager);

        for (ApplicationInfo nonSystemAppInfo : nonSystemAppInfoList)
            if (nonSystemAppInfo.packageName.equals(appInfo.packageName))
                return true;

        return false;
    } // end method isInstalled
} // end class AppUtils