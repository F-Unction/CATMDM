package com.mdm.cat.b;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.alibaba.fastjson.JSONObject;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Switch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.huawei.android.app.admin.DeviceControlManager;
import com.huawei.android.app.admin.DeviceHwSystemManager;
import com.huawei.android.app.admin.DeviceNetworkManager;
import com.huawei.android.app.admin.DevicePackageManager;
import com.huawei.android.app.admin.DeviceRestrictionManager;
import com.huawei.android.app.admin.DeviceSettingsManager;
import com.mdm.cat.b.Utils;

public class MainActivity extends AppCompatActivity {

    private static MainActivity mainActivity;

    //region Objects
    public EditText editNetworkAccessBlacklist;
    public EditText editNetworkAccessWhitelist;
    public EditText editInstallPackageWhiteList;
    public EditText editDefaultLauncher;

    public Switch switchWifiDisabled;
    public Switch switchHomeButtonDisabled;
    public Switch switchExternalStorageDisabled;
    public Switch switchUSBDataDisabled;
    public Switch switchStatusBarExpandPanelDisabled;
    public Switch switchAdbDisabled;
    public Switch switchTaskButtonDisabled;
    public Switch switchSystemBrowserDisabled;
    public Switch switchScreenCaptureDisabled;
    public Switch switchChangeWallpaperDisabled;
    public Switch switchSendNotificationDisabled;
    public Switch switchDevelopmentOptionDisabled;
    public Switch switchRestoreFactoryDisabled;
    //endregion

    public MainActivity() {
        mainActivity = this;
    }

    @Override
    protected void onStart() {
        super.onStart();

        try {
            updateObjects();
        } catch (Exception e) {
            ;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        try {
            createMDMObject();
            selfProtect();
            readConfig();
            getObjects();
            updateObjects();
            applyPolicy(true);
        } catch (Exception e) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("错误")
                    .setMessage(e.getMessage())
                    .setPositiveButton("确定", null)
                    .show();
        }

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    applyPolicy(false);
                } catch (Exception e) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("错误")
                            .setMessage(e.getMessage())
                            .setPositiveButton("确定", null)
                            .show();
                }
            }
        });
    }

    private void applyPolicy(boolean onStart) throws Exception {
        try {
            if (Utils.config.getString("Url") == "") { //因为在config初始化的时候已经明显包含了url="" 所以这里我们不考虑null
                // 应用策略
                Utils.DRM.setWifiDisabled(Utils.mAdminName, switchWifiDisabled.isChecked());
                Utils.DRM.setHomeButtonDisabled(Utils.mAdminName, switchHomeButtonDisabled.isChecked());
                Utils.DRM.setExternalStorageDisabled(Utils.mAdminName, switchExternalStorageDisabled.isChecked());
                Utils.DRM.setUSBDataDisabled(Utils.mAdminName, switchUSBDataDisabled.isChecked());
                Utils.DRM.setStatusBarExpandPanelDisabled(Utils.mAdminName, switchStatusBarExpandPanelDisabled.isChecked());
                Utils.DRM.setAdbDisabled(Utils.mAdminName, switchAdbDisabled.isChecked());
                Utils.DRM.setTaskButtonDisabled(Utils.mAdminName, switchTaskButtonDisabled.isChecked());
                Utils.DRM.setSystemBrowserDisabled(Utils.mAdminName, switchSystemBrowserDisabled.isChecked());
                Utils.DRM.setScreenCaptureDisabled(Utils.mAdminName, switchScreenCaptureDisabled.isChecked());
                Utils.DRM.setChangeWallpaperDisabled(Utils.mAdminName, switchChangeWallpaperDisabled.isChecked());
                Utils.DRM.setSendNotificationDisabled(Utils.mAdminName, switchSendNotificationDisabled.isChecked());
                Utils.DSM.setDevelopmentOptionDisabled(Utils.mAdminName, switchDevelopmentOptionDisabled.isChecked());
                Utils.DSM.setRestoreFactoryDisabled(Utils.mAdminName, switchRestoreFactoryDisabled.isChecked());

                Utils.DCM.clearDefaultLauncher(Utils.mAdminName);
                if (editDefaultLauncher.getText().toString() != "") {
                    Utils.DCM.setDefaultLauncher(Utils.mAdminName, editDefaultLauncher.getText().toString(), editDefaultLauncher.getText().toString() + ".MainActivity");
                }
                List<String> listTo, listFrom;
                if (!editNetworkAccessBlacklist.getText().toString().equals("")) {
                    listTo = Arrays.asList(editNetworkAccessBlacklist.getText().toString().split("\n"));
                    listFrom = Utils.DNM.getNetworkAccessBlackList(Utils.mAdminName);
                    for (int i = 0; i < listFrom.size(); i++) {
                        if (!listTo.contains(listFrom.get(i))) {//新的里面不包含原来的
                            Utils.DNM.removeNetworkAccessBlackList(Utils.mAdminName, new ArrayList(Arrays.asList(listFrom.get(i))));
                        }
                    }
                    for (int i = 0; i < listTo.size(); i++) {
                        if (!listFrom.contains(listTo.get(i))) {//原来的里面不包含新的
                            Utils.DNM.addNetworkAccessBlackList(Utils.mAdminName, new ArrayList(Arrays.asList(listTo.get(i))));
                        }
                    }
                } else {
                    listFrom = Utils.DNM.getNetworkAccessBlackList(Utils.mAdminName);
                    for (int i = 0; i < listFrom.size(); i++) {
                        Utils.DNM.removeNetworkAccessBlackList(Utils.mAdminName, new ArrayList(Arrays.asList(listFrom.get(i))));
                    }
                }

                if (!editNetworkAccessWhitelist.getText().toString().equals("")) {
                    listTo = Arrays.asList(editNetworkAccessWhitelist.getText().toString().split("\n"));
                    listFrom = Utils.DNM.getNetworkAccessWhitelist(Utils.mAdminName);
                    for (int i = 0; i < listFrom.size(); i++) {
                        if (!listTo.contains(listFrom.get(i))) {//新的里面不包含原来的
                            Utils.DNM.removeNetworkAccessWhitelist(Utils.mAdminName, Arrays.asList(listFrom.get(i)));
                        }
                    }
                    for (int i = 0; i < listTo.size(); i++) {
                        if (!listFrom.contains(listTo.get(i))) {//原来的里面不包含新的
                            Utils.DNM.addNetworkAccessWhitelist(Utils.mAdminName, Arrays.asList(listTo.get(i)));
                        }
                    }
                } else {
                    listFrom = Utils.DNM.getNetworkAccessWhitelist(Utils.mAdminName);
                    for (int i = 0; i < listFrom.size(); i++) {
                        Utils.DNM.removeNetworkAccessWhitelist(Utils.mAdminName, new ArrayList(Arrays.asList(listFrom.get(i))));
                    }
                }

                if (!editInstallPackageWhiteList.getText().toString().equals("")) {
                    listTo = Arrays.asList(editInstallPackageWhiteList.getText().toString().split("\n"));
                    listFrom = Utils.DPM.getInstallPackageWhiteList(Utils.mAdminName);
                    for (int i = 0; i < listFrom.size(); i++) {
                        if (!listTo.contains(listFrom.get(i))) {//新的里面不包含原来的
                            Utils.DPM.removeInstallPackageWhiteList(Utils.mAdminName, Arrays.asList(listFrom.get(i)));
                        }
                    }
                    for (int i = 0; i < listTo.size(); i++) {
                        if (!listFrom.contains(listTo.get(i))) {//原来的里面不包含新的
                            Utils.DPM.addInstallPackageWhiteList(Utils.mAdminName, Arrays.asList(listTo.get(i)));
                        }
                    }
                } else {
                    listFrom = Utils.DPM.getInstallPackageWhiteList(Utils.mAdminName);
                    for (int i = 0; i < listFrom.size(); i++) {
                        Utils.DPM.removeInstallPackageWhiteList(Utils.mAdminName, new ArrayList(Arrays.asList(listFrom.get(i))));
                    }
                }
            } else {
                Utils.updateWebPolicies(false);

                // 更改Edit内容
                editNetworkAccessWhitelist.setText(String.join("\n", Utils.DNM.getNetworkAccessWhitelist(Utils.mAdminName)));
                editNetworkAccessBlacklist.setText(String.join("\n", Utils.DNM.getNetworkAccessBlackList(Utils.mAdminName)));
                editInstallPackageWhiteList.setText(String.join("\n", Utils.DPM.getInstallPackageWhiteList(Utils.mAdminName)));

                // 更改Switch状态
                switchWifiDisabled.setChecked(Utils.DRM.isWifiDisabled(Utils.mAdminName));
                switchHomeButtonDisabled.setChecked(Utils.DRM.isHomeButtonDisabled(Utils.mAdminName));
                switchExternalStorageDisabled.setChecked(Utils.DRM.isExternalStorageDisabled(Utils.mAdminName));
                switchUSBDataDisabled.setChecked(Utils.DRM.isUSBDataDisabled(Utils.mAdminName));
                switchStatusBarExpandPanelDisabled.setChecked(Utils.DRM.isStatusBarExpandPanelDisabled(Utils.mAdminName));
                switchAdbDisabled.setChecked(Utils.DRM.isAdbDisabled(Utils.mAdminName));
                switchTaskButtonDisabled.setChecked(Utils.DRM.isTaskButtonDisabled(Utils.mAdminName));
                switchSystemBrowserDisabled.setChecked(Utils.DRM.isSystemBrowserDisabled(Utils.mAdminName));
                switchScreenCaptureDisabled.setChecked(Utils.DRM.isScreenCaptureDisabled(Utils.mAdminName));
                switchChangeWallpaperDisabled.setChecked(Utils.DRM.isChangeWallpaperDisabled(Utils.mAdminName));
                switchSendNotificationDisabled.setChecked(Utils.DRM.isSendNotificationDisabled(Utils.mAdminName));
                switchDevelopmentOptionDisabled.setChecked(Utils.DSM.isDevelopmentOptionDisabled(Utils.mAdminName));
                switchRestoreFactoryDisabled.setChecked(Utils.DSM.isRestoreFactoryDisabled(Utils.mAdminName));
            }


        } catch (Exception e) {
            throw e;
        }

        if (!onStart) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("成功")
                    .setMessage("策略已更新")
                    .setPositiveButton("确定", null)
                    .show();
        }
    }

    private void selfProtect() {
        ArrayList<String> superWhiteListPackage = Utils.DHSM.getSuperWhiteListForHwSystemManger(Utils.mAdminName);
        if (superWhiteListPackage != null && !superWhiteListPackage.contains(getPackageName())) {
            superWhiteListPackage.add(getPackageName());
        } else {
            superWhiteListPackage = new ArrayList<String>(Arrays.asList(new String[]{getPackageName()}));
        }
        Utils.DHSM.setSuperWhiteListForHwSystemManger(Utils.mAdminName, superWhiteListPackage);
        //Utils.DHSM.removeSuperWhiteListForHwSystemManger(Utils.mAdminName, superWhiteListPackage);
    }

    private void updateObjects() throws Exception {
        try {
            // 更改Edit内容
            editNetworkAccessWhitelist.setText(String.join("\n", Utils.DNM.getNetworkAccessWhitelist(Utils.mAdminName)));
            editNetworkAccessBlacklist.setText(String.join("\n", Utils.DNM.getNetworkAccessBlackList(Utils.mAdminName)));
            editInstallPackageWhiteList.setText(String.join("\n", Utils.DPM.getInstallPackageWhiteList(Utils.mAdminName)));
            editDefaultLauncher.setText(Utils.getLauncherPackageName(MainActivity.this));

            // 更改Switch状态
            switchWifiDisabled.setChecked(Utils.DRM.isWifiDisabled(Utils.mAdminName));
            switchHomeButtonDisabled.setChecked(Utils.DRM.isHomeButtonDisabled(Utils.mAdminName));
            switchExternalStorageDisabled.setChecked(Utils.DRM.isExternalStorageDisabled(Utils.mAdminName));
            switchUSBDataDisabled.setChecked(Utils.DRM.isUSBDataDisabled(Utils.mAdminName));
            switchStatusBarExpandPanelDisabled.setChecked(Utils.DRM.isStatusBarExpandPanelDisabled(Utils.mAdminName));
            switchAdbDisabled.setChecked(Utils.DRM.isAdbDisabled(Utils.mAdminName));
            switchTaskButtonDisabled.setChecked(Utils.DRM.isTaskButtonDisabled(Utils.mAdminName));
            switchSystemBrowserDisabled.setChecked(Utils.DRM.isSystemBrowserDisabled(Utils.mAdminName));
            switchScreenCaptureDisabled.setChecked(Utils.DRM.isScreenCaptureDisabled(Utils.mAdminName));
            switchChangeWallpaperDisabled.setChecked(Utils.DRM.isChangeWallpaperDisabled(Utils.mAdminName));
            switchSendNotificationDisabled.setChecked(Utils.DRM.isSendNotificationDisabled(Utils.mAdminName));
            switchDevelopmentOptionDisabled.setChecked(Utils.DSM.isDevelopmentOptionDisabled(Utils.mAdminName));
            switchRestoreFactoryDisabled.setChecked(Utils.DSM.isRestoreFactoryDisabled(Utils.mAdminName));
        } catch (Exception e) {
            throw e;
        }
    }

    private void getObjects() {
        // 获取Edit
        editNetworkAccessBlacklist = findViewById(R.id.editNetworkAccessBlacklist);
        editNetworkAccessWhitelist = findViewById(R.id.editNetworkAccessWhitelist);
        editInstallPackageWhiteList = findViewById(R.id.editInstallPackageWhiteList);
        editDefaultLauncher = findViewById(R.id.editDefaultLauncher);
        // 获取Switch
        switchWifiDisabled = findViewById(R.id.switchWifiDisabled);
        switchHomeButtonDisabled = findViewById(R.id.switchHomeButtonDisable);
        switchExternalStorageDisabled = findViewById(R.id.switchExternalStorageDisabled);
        switchUSBDataDisabled = findViewById(R.id.switchUSBDataDisabled);
        switchStatusBarExpandPanelDisabled = findViewById(R.id.switchStatusBarExpandPanelDisabled);
        switchAdbDisabled = findViewById(R.id.switchAdbDisabled);
        switchTaskButtonDisabled = findViewById(R.id.switchTaskButtonDisabled);
        switchSystemBrowserDisabled = findViewById(R.id.switchSystemBrowserDisabled);
        switchScreenCaptureDisabled = findViewById(R.id.switchScreenCaptureDisabled);
        switchChangeWallpaperDisabled = findViewById(R.id.switchChangeWallpaperDisabled);
        switchSendNotificationDisabled = findViewById(R.id.switchSendNotificationDisabled);
        switchDevelopmentOptionDisabled = findViewById(R.id.switchDevelopmentOptionDisabled);
        switchRestoreFactoryDisabled = findViewById(R.id.switchRestoreFactoryDisabled);
    }

    private void createMDMObject() throws Exception {

        Utils.mAdminName = new ComponentName(MainActivity.this, Receiver.class);
        Utils.DRM = new DeviceRestrictionManager();
        Utils.DNM = new DeviceNetworkManager();
        Utils.DPM = new DevicePackageManager();
        Utils.DSM = new DeviceSettingsManager();
        Utils.DCM = new DeviceControlManager();
        Utils.DHSM = new DeviceHwSystemManager();

    }

    private void readConfig() throws IOException {
        try {
            File file = new File(getFilesDir(), "config.json");
            if (file.exists()) {
                FileInputStream fis = this.openFileInput(file.getName());
                int length = fis.available();
                byte[] buffer = new byte[length];
                fis.read(buffer);
                Utils.config = JSONObject.parseObject(new String(buffer, "UTF-8"));
                fis.close();
            } else {
                file.createNewFile();
                Utils.config = new JSONObject();
                Utils.config.put("Url", "");

                FileOutputStream fos = this.openFileOutput(file.getName(), MODE_PRIVATE);
                byte[] bytes = Utils.config.toJSONString().getBytes();
                fos.write(bytes);
                fos.close();
            }
        } catch (Exception e) {
            throw e;
        }
    }

    public static MainActivity getMainActivity() {
        return mainActivity;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        if (id == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        } else if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }
}