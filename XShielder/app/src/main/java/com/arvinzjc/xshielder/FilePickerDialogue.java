/*
 * @Description: a class for the customised file picker dialogue
 * @Version: 1.1.4.20200330
 * @Author: Akshay Sunil Masram
 * @Date: 2020-03-02 19:32:55
 * @Last Editors: Jichen Zhao
 * @LastEditTime: 2020-03-30 20:27:44
 */

package com.arvinzjc.xshielder;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import com.apkfuns.logutils.LogUtils;
import com.developer.filepicker.R;
import com.developer.filepicker.controller.DialogSelectionListener;
import com.developer.filepicker.controller.adapters.FileListAdapter;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.model.FileListItem;
import com.developer.filepicker.model.MarkedItemList;
import com.developer.filepicker.utils.ExtensionFilter;
import com.developer.filepicker.utils.Utility;

public class FilePickerDialogue extends Dialog implements AdapterView.OnItemClickListener
{
    private final Context mContext;
    private ListView mListView;
    private TextView mDname, mDirPath, mTitle;
    private DialogProperties mProperties;
    private DialogSelectionListener mCallbacks;
    private ArrayList<FileListItem> mInternalList;
    private ExtensionFilter mFilter;
    private FileListAdapter mFileListAdapter;
    private String mTitleStr = null;

    /**
     * The default constructor to use the customised file picker dialogue.
     * ATTENTION: call the method show() only after granting the storage access permission.
     * @param context global info about an app environment
     * @param properties the properties configured for the dialogue
     */
    FilePickerDialogue(Context context, DialogProperties properties)
    {
        super(context);
        mContext = context;
        mProperties = properties;
        mFilter = new ExtensionFilter(mProperties);
        mInternalList = new ArrayList<>();
    } // end constructor FilePickerDialogue

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_main);

        mListView = findViewById(R.id.fileList);
        mDname = findViewById(R.id.dname);
        mTitle = findViewById(R.id.title);
        mDirPath = findViewById(R.id.dir_path);
        Button select = findViewById(R.id.select);
        Button cancel = findViewById(R.id.cancel);
        int buttonColour = mContext.getResources().getColor(com.arvinzjc.xshielder.R.color.colourInfo, mContext.getTheme());

        ((RelativeLayout)findViewById(R.id.header).getParent()).setBackgroundColor(mContext.getResources().getColor(com.arvinzjc.xshielder.R.color.card_backgroundColour, mContext.getTheme()));
        ((LinearLayout)findViewById(R.id.imageView).getParent()).setBackgroundColor(buttonColour);

        select.setText(mContext.getResources().getString(R.string.choose_button_label));
        select.setTextAppearance(com.arvinzjc.xshielder.R.style.ButtonTextStyle);
        select.setOnClickListener(view ->
        {
            String[] paths = MarkedItemList.getSelectedPaths();
            if (mCallbacks != null)
                mCallbacks.onSelectedFilePaths(paths);

            dismiss();
        });

        if (MarkedItemList.getFileCount() == 0)
        {
            select.setEnabled(false);
            select.setTextColor(Color.argb(128, Color.red(buttonColour), Color.green(buttonColour), Color.blue(buttonColour)));
        } // end if

        cancel.setText(mContext.getString(com.arvinzjc.xshielder.R.string.dialogue_defaultNegativeText));
        cancel.setTextAppearance(com.arvinzjc.xshielder.R.style.ButtonTextStyle);
        cancel.setOnClickListener(view -> cancel());

        mFileListAdapter = new FileListAdapter(mInternalList, mContext, mProperties);
        mFileListAdapter.setNotifyItemCheckedListener(() ->
        {
            if (MarkedItemList.getFileCount() == 0)
            {
                select.setEnabled(false);
                select.setTextColor(Color.argb(128, Color.red(buttonColour), Color.green(buttonColour), Color.blue(buttonColour)));
            }
            else
            {
                select.setEnabled(true);
                select.setTextColor(buttonColour);
            } // end if...else

            mFileListAdapter.notifyDataSetChanged(); // if a single file has to be selected, clear the previously checked checkbox from the list
        });
        mListView.setAdapter(mFileListAdapter);
        setTitle();
    } // end method onCreate

    /**
     * Perform tasks when the dialogue has detected the user's press of the back key.
     */
    @Override
    public void onBackPressed()
    {
        String currentDirName = mDname.getText().toString();

        if (mInternalList.size() > 0)
        {
            FileListItem fitem = mInternalList.get(0);
            File currLoc = new File(fitem.getLocation());

            if (currentDirName.equals(mProperties.root.getName()) || !currLoc.canRead())
                super.onBackPressed();
            else
            {
                mDname.setText(currLoc.getName());
                mDirPath.setText(currLoc.getAbsolutePath());
                mInternalList.clear();

                if (!currLoc.getName().equals(mProperties.root.getName()))
                {
                    FileListItem parent = new FileListItem();

                    parent.setFilename(mContext.getString(R.string.label_parent_dir));
                    parent.setDirectory(true);

                    File parentFile = currLoc.getParentFile();

                    if (parentFile != null)
                    {
                        parent.setLocation(currLoc.getParentFile().getAbsolutePath());
                        parent.setTime(currLoc.lastModified());
                        mInternalList.add(parent);
                    }
                    else
                        LogUtils.w("Failed to get the parent file object. Some errors might occur.");
                } // end if

                mInternalList = Utility.prepareFileListEntries(mInternalList, currLoc, mFilter, mProperties.show_hidden_files);
                mFileListAdapter.notifyDataSetChanged();
            } // end if...else

            setTitle();
        }
        else
            super.onBackPressed();
    } // end method onBackPressed

    /**
     * Callback method to be invoked when an item in this adapter view has been clicked.
     * @param adapterView the adapter view where the click happened
     * @param view the view within the adapter view that was clicked (this will be a view provided by the adapter)
     * @param position the position of the view in the adapter
     * @param id the row id of the item that was clicked
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
    {
        if (mInternalList.size() > position)
        {
            FileListItem fileListItem = mInternalList.get(position);

            if (fileListItem.isDirectory())
            {
                if (new File(fileListItem.getLocation()).canRead())
                {
                    File currLoc = new File(fileListItem.getLocation());

                    mDname.setText(currLoc.getName());
                    setTitle();
                    mDirPath.setText(currLoc.getAbsolutePath());
                    mInternalList.clear();

                    if (!currLoc.getName().equals(mProperties.root.getName()))
                    {
                        FileListItem parent = new FileListItem();

                        parent.setFilename(mContext.getString(R.string.label_parent_dir));
                        parent.setDirectory(true);

                        File parentFile = currLoc.getParentFile();

                        if (parentFile != null)
                        {
                            parent.setLocation(currLoc.getParentFile().getAbsolutePath());
                            parent.setTime(currLoc.lastModified());
                            mInternalList.add(parent);
                        }
                        else
                            LogUtils.w("Failed to get the parent file object. Some errors might occur.");
                    } // end if

                    mInternalList = Utility.prepareFileListEntries(mInternalList, currLoc, mFilter, mProperties.show_hidden_files);
                    mFileListAdapter.notifyDataSetChanged();
                }
                else
                    Toast.makeText(mContext.getApplicationContext(), R.string.error_dir_access, Toast.LENGTH_SHORT).show(); // the application context is required to avoid any abnormal toast styles
            }
            else
                view.findViewById(R.id.file_mark).performClick();
        } // end if
    } // end method onItemClick

    /**
     * Perform tasks when the dialogue is starting.
     * ATTENTION: need the storage access permission granted.
     */
    @SuppressLint("SetTextI18n") // suppress the warning of concatenating strings in the method setText()
    @Override
    protected void onStart()
    {
        super.onStart();

        File currLoc;

        mInternalList.clear();

        if (mProperties.offset.isDirectory() && validateOffsetPath())
        {
            currLoc = new File(mProperties.offset.getAbsolutePath());

            FileListItem parent = new FileListItem();

            parent.setFilename(mContext.getString(R.string.label_parent_dir));
            parent.setDirectory(true);

            File parentFile = currLoc.getParentFile();

            if (parentFile != null)
            {
                parent.setLocation(currLoc.getParentFile().getAbsolutePath());
                parent.setTime(currLoc.lastModified());
                mInternalList.add(parent);
            }
            else
                LogUtils.w("Failed to get the parent file object. Some errors might occur.");
        }
        else if (mProperties.root.exists() && mProperties.root.isDirectory())
            currLoc = new File(mProperties.root.getAbsolutePath());
        else
            currLoc = new File(mProperties.error_dir.getAbsolutePath());

        mDname.setText(currLoc.getName());
        setTitle();
        mInternalList = Utility.prepareFileListEntries(mInternalList, currLoc, mFilter, mProperties.show_hidden_files);

        int fileCount = mInternalList.size();

        if (fileCount == 0 || fileCount == 1)
            mDirPath.setText(fileCount + " "+ mContext.getString(com.arvinzjc.xshielder.R.string.filePickerDialogue_defaultSubtitle_singular));
        else
            mDirPath.setText(fileCount + " "+ mContext.getString(com.arvinzjc.xshielder.R.string.filePickerDialogue_defaultSubtitle_plural));

        mFileListAdapter.notifyDataSetChanged();
        mListView.setOnItemClickListener(this);
    } // end method onStart

    /**
     * Dismiss this dialog, removing it from the screen.
     */
    @Override
    public void dismiss()
    {
        MarkedItemList.clearSelectionList();
        mInternalList.clear();
        super.dismiss();
    } // end method dismiss

    /**
     * Set the title text for this dialog's window.
     * @param titleStr the new text to display in the title
     */
    @Override
    public void setTitle(CharSequence titleStr)
    {
        if (titleStr != null)
            mTitleStr = titleStr.toString();
        else
            mTitleStr = null;

        setTitle();
    } // end overrided method setTitle

    /**
     * Get the properties configured for the dialogue.
     * @return the properties configured for the dialogue
     */
    public DialogProperties getProperties()
    {
        return mProperties;
    } // end method getProperties

    /**
     * Set the selection listener on the dialogue.
     * @param callbacks the selection listener on the dialogue
     */
    void setDialogSelectionListener(DialogSelectionListener callbacks)
    {
        mCallbacks = callbacks;
    } // end method setDialogSelectionListener

    /**
     * Set the properties configured for the dialogue.
     * @param properties the properties configured for the dialogue
     */
    public void setProperties(DialogProperties properties)
    {
        mProperties = properties;
        mFilter = new ExtensionFilter(mProperties);
    } // end method setProperties

    // set the title of the header of the dialogue
    private void setTitle()
    {
        if (mTitle == null || mDname == null)
            return;

        if (mTitleStr != null)
        {
            if (mTitle.getVisibility() == View.INVISIBLE)
                mTitle.setVisibility(View.VISIBLE);

            mTitle.setText(mTitleStr);

            if (mDname.getVisibility() == View.VISIBLE)
                mDname.setVisibility(View.INVISIBLE);
        }
        else
        {
            if (mTitle.getVisibility() == View.VISIBLE)
                mTitle.setVisibility(View.INVISIBLE);

            if (mDname.getVisibility() == View.INVISIBLE)
                mDname.setVisibility(View.VISIBLE);
        } // end if...else
    } // end method setTitle

    // validate the offset path
    private boolean validateOffsetPath()
    {
        String offset_path = mProperties.offset.getAbsolutePath();
        String root_path = mProperties.root.getAbsolutePath();
        return !offset_path.equals(root_path) && offset_path.contains(root_path);
    } // end method validateOffsetPath
} // end class FilePickerDialogue