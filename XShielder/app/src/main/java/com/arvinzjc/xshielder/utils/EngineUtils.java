/*
 * @Description: utilities for supporting the integrated anti-malware engine
 * @Version: 1.0.3.20200410
 * @Author: Jichen Zhao
 * @Date: 2020-04-07 19:28:36
 * @Last Editors: Jichen Zhao
 * @LastEditTime: 2020-04-10 19:30:26
 */

package com.arvinzjc.xshielder.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EngineUtils
{
    private static final String DICTIONARY_GENERATOR_MODULE = "dictionary_generator";
    private static final String DICTIONARY_GENERATOR_CORE_FUNCTION = "generate_dictionary";
    private static final String FEATURE_EXTRACTOR_MODULE = "feature_extractor";
    private static final String FEATURE_EXTRACTOR_CORE_FUNCTION = "extract_compressed_features";

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
     * Run the specified Python code to generate a dictionary storing mapping all distinct API calls to numbers and pickle the dictionary.
     * @return the length of the API call dictionary (-1 if any exception occurs during the execution of the Python code)
     */
    public int generateDictionary()
    {
        return mEngine.getModule(DICTIONARY_GENERATOR_MODULE).callAttr(DICTIONARY_GENERATOR_CORE_FUNCTION, mApkFolderDirectory).toJava(Integer.class);
    } // end method generateDictionary

    /**
     * Run the specified Python code to extract compressed features for apps and pickle them.
     * @return a map recording problems (keys for APK names and values for the corresponding problems)
     */
    public HashMap<String, String> extractFeatures()
    {
        /*
         * a list recording failed extraction (even indexes for APK names and odd indexes for the corresponding problems);
         * avoid using ArrayList here, as PyList from Chaquopy cannot be cast to ArrayList
         */
        List<PyObject> problemList = mEngine.getModule(FEATURE_EXTRACTOR_MODULE).callAttr(FEATURE_EXTRACTOR_CORE_FUNCTION, mApkFolderDirectory).asList();
        int problemListSize = problemList.size();
        HashMap<String, String> problemMap = new HashMap<>();

        if (problemListSize > 0)
        {
            ArrayList<String> apkNameList = new ArrayList<>(); // a list storing even indexes of the problem list
            ArrayList<String> correspondingProblemList = new ArrayList<>(); // a list storing odd indexes of the problem list

            for (int count = 0; count < problemListSize; count += 2)
                apkNameList.add(problemList.get(count).toString());

            for (int count = 1; count < problemListSize; count += 2)
                correspondingProblemList.add(problemList.get(count).toString());

            for (int count = 0; count < apkNameList.size(); count++)
                problemMap.put(apkNameList.get(count), correspondingProblemList.get(count));
        } // end if

        return problemMap;
    } // end method extractFeatures
} // end class EngineUtils