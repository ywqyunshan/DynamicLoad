package com.btzh.dynamicload;

import android.app.*;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import net.wequick.small.Small;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class LaunchActivity extends AppCompatActivity {
    private View mContentView;
    private  Context mAContext;
    //本地服务器插件更新地址
    private final static String UPDATEURL="http://192.168.0.109/dynamicload/upgrade/bundles.json";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        mAContext=this;
        mContentView = findViewById(R.id.fullscreen_content);

        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        // Remove the status and navigation bar
        if (Build.VERSION.SDK_INT < 14) return;
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        if (Small.getIsNewHostApp()) {
            TextView tvPrepare = (TextView) findViewById(R.id.prepare_text);
            tvPrepare.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Application.isUpdate){
            openMian();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkUpgrade();
        /*Small.setUp(this, new net.wequick.small.Small.OnCompleteListener() {
            @Override
            public void onComplete() {
                Small.openUri("main", LaunchActivity.this);
                finish();
            }
        });*/
        if (Application.isUpdate){
            openMian();
        }
    }

    public  void openMian(){
        Small.setUp(this, new net.wequick.small.Small.OnCompleteListener() {
            @Override
            public void onComplete() {
                Small.openUri("main", LaunchActivity.this);
                finish();
            }
        });
    }

    private void checkUpgrade() {
        new Application.UpgradeManager(mAContext).checkUpgrade();
    }


}
