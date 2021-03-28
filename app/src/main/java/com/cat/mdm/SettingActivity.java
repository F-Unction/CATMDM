package com.cat.mdm;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.EditText;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.huawei.android.app.admin.DeviceControlManager;
import com.huawei.android.app.admin.DeviceNetworkManager;
import com.huawei.android.app.admin.DevicePackageManager;
import com.huawei.android.app.admin.DeviceRestrictionManager;
import com.huawei.android.app.admin.DeviceSettingsManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingActivity extends AppCompatActivity {

    private static EditText urlEdit;

    @SuppressLint("HandlerLeak")
    private final static Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            String val = data.getString("value");
            applyPolicy(val);
        }
    };

    private final static Runnable getPolicy = () -> {
        try {
            StringBuilder dataString = new StringBuilder();
            String urlString = MainActivity.getMainActivity().config.getString("Url");
            URL url = new URL(urlString);
            HttpURLConnection httpUrlConn = (HttpURLConnection) url.openConnection();
            httpUrlConn.setDoInput(true);
            httpUrlConn.setRequestMethod("GET");
            httpUrlConn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
            InputStream input = httpUrlConn.getInputStream();
            InputStreamReader read = new InputStreamReader(input, StandardCharsets.UTF_8);

            BufferedReader br = new BufferedReader(read);
            String jsonString = br.readLine();

            while (jsonString != null) {
                Log.d("debug", jsonString);
                dataString.append(jsonString);
                jsonString = br.readLine();
            }
            br.close();
            read.close();
            input.close();
            httpUrlConn.disconnect();
            Message msg = new Message();
            Bundle data = new Bundle();
            data.putString("value", dataString.toString());
            msg.setData(data);
            handler.sendMessage(msg);
        } catch (Exception e) {
            ;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        urlEdit.setEnabled(false);
        try {
            updateWebPolicies(true);
        } catch (Exception e) {
            new AlertDialog.Builder(SettingActivity.this)
                    .setTitle("错误")
                    .setMessage(e.getMessage())
                    .setPositiveButton("确定", null)
                    .show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        urlEdit = findViewById(R.id.editUrl);
        urlEdit.setText(MainActivity.getMainActivity().config.getString("Url"));

        findViewById(R.id.fab2).setOnClickListener(view -> {
            try {
                updateWebPolicies(true);
                new AlertDialog.Builder(view.getContext())
                        .setTitle("成功")
                        .setMessage("设置网络策略成功")
                        .setPositiveButton("确定", null)
                        .show();
            } catch (Exception e) {
                new AlertDialog.Builder(view.getContext())
                        .setTitle("错误")
                        .setMessage(e.getMessage())
                        .setPositiveButton("确定", null)
                        .show();
            }
        });
    }

    public static void updateWebPolicies(boolean isSetting) throws Exception {
        String uurl;
        if(isSetting)
        {
            uurl=urlEdit.getText().toString();
        }
        else
        {
            uurl=MainActivity.getMainActivity().config.getString("Url");
        }
        if (!uurl.equals("")) {
            MainActivity.getMainActivity().config.put("Url", uurl);
            new Thread(getPolicy).start();
        } else {
            urlEdit.setEnabled(true);
            MainActivity.getMainActivity().config.put("Url", "");
        }

        FileOutputStream fos = MainActivity.getMainActivity().openFileOutput("config.json", MODE_PRIVATE);
        byte[] bytes = MainActivity.getMainActivity().config.toJSONString().getBytes();
        fos.write(bytes);
        fos.close();
    }

    public static void applyPolicy(String str) {
        ComponentName mAdminName = MainActivity.getMainActivity().mAdminName;
        DeviceRestrictionManager DRM = MainActivity.getMainActivity().DRM;
        DeviceNetworkManager DNM = MainActivity.getMainActivity().DNM;
        DevicePackageManager DPM = MainActivity.getMainActivity().DPM;
        DeviceSettingsManager DSM = MainActivity.getMainActivity().DSM;

        JSONObject policies = JSONObject.parseObject(str);

        urlEdit.setEnabled(!policies.getBooleanValue("ForceWebPolicy"));

        DRM.setWifiDisabled(mAdminName, policies.getBooleanValue("WifiDisabled"));
        DRM.setHomeButtonDisabled(mAdminName, policies.getBooleanValue("HomeButtonDisabled"));
        DRM.setExternalStorageDisabled(mAdminName, policies.getBooleanValue("ExternalStorageDisabled"));
        DRM.setUSBDataDisabled(mAdminName, policies.getBooleanValue("USBDataDisabled"));
        DRM.setStatusBarExpandPanelDisabled(mAdminName, policies.getBooleanValue("StatusBarExpandPanelDisabled"));
        DRM.setAdbDisabled(mAdminName, policies.getBooleanValue("AdbDisabled"));
        DRM.setTaskButtonDisabled(mAdminName, policies.getBooleanValue("TaskButtonDisabled"));
        DRM.setSystemBrowserDisabled(mAdminName, policies.getBooleanValue("SystemBrowserDisabled"));
        DRM.setScreenCaptureDisabled(mAdminName, policies.getBooleanValue("ScreenCaptureDisabled"));
        DRM.setChangeWallpaperDisabled(mAdminName, policies.getBooleanValue("ChangeWallpaperDisabled"));
        DRM.setSendNotificationDisabled(mAdminName, policies.getBooleanValue("SendNotificationDisabled"));
        DSM.setDevelopmentOptionDisabled(mAdminName, policies.getBooleanValue("DevelopmentOptionDisabled"));
        DSM.setRestoreFactoryDisabled(mAdminName, policies.getBooleanValue("RestoreFactoryDisabled"));

        List<String> listTo, listFrom;
        if (!JSON.parseArray(policies.getJSONArray("NetworkAccessWhitelist").toJSONString(), String.class).isEmpty()) {
            listTo = JSON.parseArray(policies.getJSONArray("NetworkAccessWhitelist").toJSONString(), String.class);
            listFrom = DNM.getNetworkAccessBlackList(mAdminName);
            for (int i = 0; i < listFrom.size(); i++) {
                if (!listTo.contains(listFrom.get(i))) {//新的里面不包含原来的
                    DNM.removeNetworkAccessBlackList(mAdminName, new ArrayList(Arrays.asList(listFrom.get(i))));
                }
            }
            for (int i = 0; i < listTo.size(); i++) {
                if (!listFrom.contains(listTo.get(i))) {//原来的里面不包含新的
                    DNM.addNetworkAccessBlackList(mAdminName, new ArrayList(Arrays.asList(listTo.get(i))));
                }
            }
        } else {
            listFrom = DNM.getNetworkAccessBlackList(mAdminName);
            for (int i = 0; i < listFrom.size(); i++) {
                DNM.removeNetworkAccessBlackList(mAdminName, new ArrayList(Arrays.asList(listFrom.get(i))));
            }
        }

        if (!JSON.parseArray(policies.getJSONArray("NetworkAccessBlacklist").toJSONString(), String.class).isEmpty()) {
            listTo = JSON.parseArray(policies.getJSONArray("NetworkAccessBlacklist").toJSONString(), String.class);
            listFrom = DNM.getNetworkAccessWhitelist(mAdminName);
            for (int i = 0; i < listFrom.size(); i++) {
                if (!listTo.contains(listFrom.get(i))) {//新的里面不包含原来的
                    DNM.removeNetworkAccessWhitelist(mAdminName, Arrays.asList(listFrom.get(i)));
                }
            }
            for (int i = 0; i < listTo.size(); i++) {
                if (!listFrom.contains(listTo.get(i))) {//原来的里面不包含新的
                    DNM.addNetworkAccessWhitelist(mAdminName, Arrays.asList(listTo.get(i)));
                }
            }
        } else {
            listFrom = DNM.getNetworkAccessWhitelist(mAdminName);
            for (int i = 0; i < listFrom.size(); i++) {
                DNM.removeNetworkAccessWhitelist(mAdminName, new ArrayList(Arrays.asList(listFrom.get(i))));
            }
        }

        if (!JSON.parseArray(policies.getJSONArray("InstallPackageWhiteList").toJSONString(), String.class).isEmpty()) {
            listTo = JSON.parseArray(policies.getJSONArray("InstallPackageWhiteList").toJSONString(), String.class);
            listFrom = DPM.getInstallPackageWhiteList(mAdminName);
            for (int i = 0; i < listFrom.size(); i++) {
                if (!listTo.contains(listFrom.get(i))) {//新的里面不包含原来的
                    DPM.removeInstallPackageWhiteList(mAdminName, Arrays.asList(listFrom.get(i)));
                }
            }
            for (int i = 0; i < listTo.size(); i++) {
                if (!listFrom.contains(listTo.get(i))) {//原来的里面不包含新的
                    DPM.addInstallPackageWhiteList(mAdminName, Arrays.asList(listTo.get(i)));
                }
            }
        } else {
            listFrom = DPM.getInstallPackageWhiteList(mAdminName);
            for (int i = 0; i < listFrom.size(); i++) {
                DPM.removeInstallPackageWhiteList(mAdminName, new ArrayList(Arrays.asList(listFrom.get(i))));
            }
        }
    }
}

