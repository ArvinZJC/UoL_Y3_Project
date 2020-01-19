package com.arvinzjc.xshielder;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class ActivityWifi extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);
        setSupportActionBar((Toolbar)findViewById(R.id.toolbarWifi));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // enable the Up button for this activity whose parent activity is the home activity
    } // end method onCreate

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            finish();
            return true;
        } // end if

        return super.onOptionsItemSelected(item);
    } // end method onOptionsItemSelected
} // end class ActivityWifi