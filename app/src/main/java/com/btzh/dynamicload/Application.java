package com.btzh.dynamicload;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
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
 * Created by galen on 15/11/3.
 */
public class Application extends android.app.Application {
    //本地服务器插件更新地址
    private final static String UPDATEURL="http://192.168.0.109/dynamicload/upgrade/bundles.json";
    public static Boolean  isUpdate=false;
    @Override
    public void onCreate() {
        super.onCreate();

        // Options
        //Small.setBaseUri("http://m.wequick.net/demo/");

        // Required,初始化small框架
        Small.preSetUp(this);
    }

    public static class UpgradeManager {

        private static class UpdateInfo {
            public String packageName;
            public String downloadUrl;
        }

        private  static class UpgradeInfo {
            public JSONObject manifest;
            public List<UpdateInfo> updates;
        }

        private  interface OnResponseListener {
            void onResponse(UpgradeInfo info);
        }

        private interface OnUpgradeListener {
            void onUpgrade(boolean succeed);
        }

        private  static  class ResponseHandler extends Handler {
            private OnResponseListener mListener;
            public ResponseHandler(OnResponseListener listener) {
                mListener = listener;
            }

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        mListener.onResponse((UpgradeInfo) msg.obj);
                        break;
                }
            }
        }

        private ResponseHandler mResponseHandler;

        private Context mContext;
        private ProgressDialog mProgressDlg;

        public UpgradeManager(Context context) {
            mContext = context;
        }

        public void checkUpgrade() {
            mProgressDlg = ProgressDialog.show(mContext, "Small", "正在检查更新...");
            requestUpgradeInfo(Small.getBundleVersions(), new OnResponseListener() {
                @Override
                public void onResponse(UpgradeInfo info) {
                    mProgressDlg.setMessage("更新中...");
                    upgradeBundles(info,
                            new OnUpgradeListener() {
                                @Override
                                public void onUpgrade(boolean succeed) {
                                    mProgressDlg.dismiss();
                                    mProgressDlg = null;
                                    String text = succeed ?
                                            "更新成功! Switch to background and back to foreground to see changes."
                                            : "更新失败!";
                                    Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
                                    isUpdate=true;
                                }
                            });
                }
            });
        }



        /**
         *
         * @param versions
         * @param listener
         */
        private void requestUpgradeInfo(Map versions, OnResponseListener listener) {
            System.out.println(versions); // this should be passed as HTTP parameters
            mResponseHandler = new ResponseHandler(listener);
            new Thread() {
                @Override
                public void run() {
                    try {
                        // Example HTTP request to get the upgrade bundles information.
                        // Json format see http://wequick.github.io/small/upgrade/bundles.json
                        URL url = new URL(UPDATEURL);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        StringBuilder sb = new StringBuilder();
                        InputStream is = conn.getInputStream();
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = is.read(buffer)) != -1) {
                            sb.append(new String(buffer, 0, length));
                        }

                        // Parse json
                        JSONObject jo = new JSONObject(sb.toString());
                        JSONObject mf = jo.has("manifest") ? jo.getJSONObject("manifest") : null;
                        JSONArray updates = jo.getJSONArray("updates");
                        int N = updates.length();
                        List<UpdateInfo> infos = new ArrayList<UpdateInfo>(N);
                        for (int i = 0; i < N; i++) {
                            JSONObject o = updates.getJSONObject(i);
                            UpdateInfo info = new UpdateInfo();
                            info.packageName = o.getString("pkg");
                            info.downloadUrl = o.getString("url");
                            infos.add(info);
                        }
                        // Post message
                        UpgradeInfo ui = new UpgradeInfo();
                        ui.manifest = mf;
                        ui.updates = infos;
                        Message.obtain(mResponseHandler, 1, ui).sendToTarget();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }

        private  class DownloadHandler extends Handler {
            private OnUpgradeListener mListener;
            public DownloadHandler(OnUpgradeListener listener) {
                mListener = listener;
            }

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        mListener.onUpgrade((Boolean) msg.obj);
                        break;
                }
            }
        }

        private DownloadHandler mHandler;

        private void upgradeBundles(final UpgradeInfo info,
                                    final OnUpgradeListener listener) {
            // Just for example, you can do this by OkHttp or something.
            mHandler = new DownloadHandler(listener);
            new Thread() {
                @Override
                public void run() {
                    try {
                        // Update manifest
                        if (info.manifest != null) {
                            if (!Small.updateManifest(info.manifest, false)) {
                                Message.obtain(mHandler, 1, false).sendToTarget();
                                return;
                            }
                        }
                        // Download bundles
                        List<UpdateInfo> updates = info.updates;
                        for (UpdateInfo u : updates) {
                            // Get the patch file for downloading
                            net.wequick.small.Bundle bundle = Small.getBundle(u.packageName);
                            File file = bundle.getPatchFile();

                            // Download
                            URL url = new URL(u.downloadUrl);
                            HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
                            InputStream is = urlConn.getInputStream();
                            OutputStream os = new FileOutputStream(file);
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = is.read(buffer)) != -1) {
                                os.write(buffer, 0, length);
                            }
                            os.flush();
                            os.close();
                            is.close();

                            // Upgrade
                            bundle.upgrade();
                        }

                        Message.obtain(mHandler, 1, true).sendToTarget();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Message.obtain(mHandler, 1, false).sendToTarget();
                    }
                }
            }.start();
        }
    }
}
