package com.mdm.cat.b;

import com.alibaba.fastjson.JSON;
import com.mdm.cat.b.SettingsActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.huawei.android.app.admin.DeviceControlManager;
import com.huawei.android.app.admin.DeviceHwSystemManager;
import com.huawei.android.app.admin.DeviceNetworkManager;
import com.huawei.android.app.admin.DevicePackageManager;
import com.huawei.android.app.admin.DeviceRestrictionManager;
import com.huawei.android.app.admin.DeviceSettingsManager;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Utils {

    public static JSONObject config;

    public static ComponentName mAdminName;
    public static DeviceRestrictionManager DRM;
    public static DeviceNetworkManager DNM;
    public static DevicePackageManager DPM;
    public static DeviceSettingsManager DSM;
    public static DeviceControlManager DCM;
    public static DeviceHwSystemManager DHSM;

    private final static Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            String val = data.getString("value");
            applyPolicyFromConfig(val);
        }
    };

    private final static Runnable getPolicy = () -> {
        try {
            StringBuilder dataString = new StringBuilder();
            String urlString = Utils.config.getString("Url");
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

    public static void updateWebPolicies(boolean isInSettingsActivity) throws Exception {
        try {
            String uurl;
            if (isInSettingsActivity) {
                uurl = SettingsActivity.urlEdit.getText().toString();
            } else {
                uurl = Utils.config.getString("Url");
            }
            if (!uurl.equals("")) {
                Utils.config.put("Url", uurl);
                new Thread(getPolicy).start();
            } else {
                if (isInSettingsActivity) {
                    SettingsActivity.urlEdit.setEnabled(true);
                }
                Utils.config.put("Url", "");
            }

            FileOutputStream fos = MainActivity.getMainActivity().openFileOutput("config.json", Context.MODE_PRIVATE);
            byte[] bytes = Utils.config.toJSONString().getBytes();
            fos.write(bytes);
            fos.close();
        } catch (Exception e) {
            throw e;
        }
    }

    public static void applyPolicyFromConfig(String str) {
        JSONObject policies = JSONObject.parseObject(str);

        try {
            SettingsActivity.urlEdit.setEnabled(!policies.getBooleanValue("ForceWebPolicy"));
        } catch (Exception e) {
            ;
        }

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

        Utils.DCM.clearDefaultLauncher(Utils.mAdminName);
        if (policies.getString("DefaultLauncher") != "") {
            Utils.DCM.setDefaultLauncher(Utils.mAdminName, policies.getString("DefaultLauncher"), policies.getString("DefaultLauncher") + ".MainActivity");
        }

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

    public static String getLauncherPackageName(Context context) {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        final ResolveInfo res = context.getPackageManager().resolveActivity(intent, 0);
        if (res.activityInfo == null) {
            // should not happen. A home is always installed, isn't it?
            return null;
        }
        if (res.activityInfo.packageName.equals("android")) {
            // 有多个桌面程序存在，且未指定默认项时；
            return null;
        } else {
            return res.activityInfo.packageName;
        }

    }
}
