/*
 * @Description: utilities for supporting the integrated anti-malware engine
 * @Version: 1.0.6.20200414
 * @Author: Jichen Zhao
 * @Date: 2020-04-07 19:28:36
 * @Last Editors: Jichen Zhao
 * @LastEditTime: 2020-04-14 19:30:26
 */

package com.arvinzjc.xshielder.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.util.HashMap;
import java.util.Map;

public class EngineUtils
{
    private static final String DATA_READER_MODULE = "data_reader";
    private static final String DATA_READER_VERSION_FUNCTION = "get_engine_version";
    private static final String DICTIONARY_GENERATOR_MODULE = "dictionary_generator";
    private static final String DICTIONARY_GENERATOR_CORE_FUNCTION = "generate_dictionary";
    private static final String FEATURE_EXTRACTOR_MODULE = "feature_extractor";
    private static final String FEATURE_EXTRACTOR_CORE_FUNCTION = "extract_compressed_features";
    private static final String APP_CLASSIFIER_MODULE = "app_classifier";
    private static final String APP_CLASSIFIER_CORE_FUNCTION = "classify_apps";

    private Python mEngine; // store the interface to the Python code of the integrated anti-malware engine
    private String mApkFolderDirectory; // the directory of the folder containing APKs for scanning

    /**
     * The default constructor to use utilities for supporting the integrated anti-malware engine.
     * @param context global info about an app environment
     * @param apkFolderDirectory the directory of the folder containing APKs for scanning
     */
    public EngineUtils(@NonNull Context context, @NonNull String apkFolderDirectory)
    {
        if (!Python.isStarted())
            Python.start(new AndroidPlatform(context));

        mEngine = Python.getInstance();
        mApkFolderDirectory = apkFolderDirectory;
    } // end constructor EngineUtils

    /**
     * Run the specified Python code to get the version of the integrated anti-malware engine.
     * @return the version of the integrated anti-malware engine
     */
    public String getEngineVersion()
    {
        return mEngine.getModule(DATA_READER_MODULE).callAttr(DATA_READER_VERSION_FUNCTION).toJava(String.class);
    } // end method getEngineVersion

    /**
     * Run the specified Python code to generate a dictionary storing mapping all distinct API calls to numbers and pickle the dictionary.
     * @return the length of the API call dictionary (-1 if any exception occurs during the execution of the Python code)
     */
    public int generateDictionary()
    {
        return mEngine.getModule(DICTIONARY_GENERATOR_MODULE).callAttr(DICTIONARY_GENERATOR_CORE_FUNCTION, mApkFolderDirectory).toJava(Integer.class);
    } // end method generateDictionary

    /**
     * Run the specified Python code to extract compressed features for apps and pickle them.
     * @return a map recording problems (keys for APK names and values for corresponding problems)
     */
    public HashMap<String, String> extractFeatures()
    {
        /*
         * a list recording failed extraction (even indexes for APK names and odd indexes for the corresponding problems);
         * avoid using ArrayList here, as PyList from Chaquopy cannot be cast to ArrayList
         */
        Map<PyObject, PyObject> problemMap_pyObject = mEngine.getModule(FEATURE_EXTRACTOR_MODULE).callAttr(FEATURE_EXTRACTOR_CORE_FUNCTION, mApkFolderDirectory).asMap();
        HashMap<String, String> problemMap = new HashMap<>();

        if (problemMap_pyObject.size() > 0)
            for (Map.Entry<PyObject, PyObject> problemEntry : problemMap_pyObject.entrySet())
                problemMap.put(problemEntry.getKey().toJava(String.class), problemEntry.getValue().toJava(String.class));

        return problemMap;
    } // end method extractFeatures

    /**
     * Run the specified Python code to classify apps as benign or malicious apps.
     * @return a map recording classification results (keys for APK names and values for corresponding results - 0 represents a benign app, while 1 represents malware)
     */
    public HashMap<String, Integer> classifyApps()
    {
        Map<PyObject, PyObject> predictionMap_pyObject = mEngine.getModule(APP_CLASSIFIER_MODULE).callAttr(APP_CLASSIFIER_CORE_FUNCTION, mApkFolderDirectory).asMap();
        HashMap<String, Integer> predictionMap = new HashMap<>();

        if (predictionMap_pyObject.size() > 0)
            for (Map.Entry<PyObject, PyObject> predictionEntry : predictionMap_pyObject.entrySet())
                predictionMap.put(predictionEntry.getKey().toJava(String.class), predictionEntry.getValue().toJava(Integer.class));

        return predictionMap;
    } // end method classifyApps
} // end class EngineUtils