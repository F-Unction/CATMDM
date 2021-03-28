package com.cat.mdm;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.ContextMenu;
import android.view.DragAndDropPermissions;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;

import com.huawei.android.app.admin.DeviceControlManager;
import com.huawei.android.app.admin.DeviceHwSystemManager;
import com.huawei.android.app.admin.DeviceNetworkManager;
import com.huawei.android.app.admin.DevicePackageManager;
import com.huawei.android.app.admin.DeviceRestrictionManager;
import com.huawei.android.app.admin.DeviceSettingsManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static MainActivity mainActivity;

    public JSONObject config;

    public EditText editNetworkAccessBlacklist;
    public EditText editNetworkAccessWhitelist;
    public EditText editInstallPackageWhiteList;

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


    public ComponentName mAdminName;
    public DeviceRestrictionManager DRM = new DeviceRestrictionManager();
    public DeviceNetworkManager DNM = new DeviceNetworkManager();
    public DevicePackageManager DPM = new DevicePackageManager();
    public DeviceSettingsManager DSM = new DeviceSettingsManager();
    public DeviceControlManager DCM = new DeviceControlManager();
    public DeviceHwSystemManager DHSM = new DeviceHwSystemManager();

    @Override
    protected void onStart() {
        super.onStart();
        // 更改Edit内容
        editNetworkAccessWhitelist.setText(String.join("\n", DNM.getNetworkAccessWhitelist(mAdminName)));
        editNetworkAccessBlacklist.setText(String.join("\n", DNM.getNetworkAccessBlackList(mAdminName)));
        editInstallPackageWhiteList.setText(String.join("\n", DPM.getInstallPackageWhiteList(mAdminName)));


        // 更改Switch状态
        switchWifiDisabled.setChecked(DRM.isWifiDisabled(mAdminName));
        switchHomeButtonDisabled.setChecked(DRM.isHomeButtonDisabled(mAdminName));
        switchExternalStorageDisabled.setChecked(DRM.isExternalStorageDisabled(mAdminName));
        switchUSBDataDisabled.setChecked(DRM.isUSBDataDisabled(mAdminName));
        switchStatusBarExpandPanelDisabled.setChecked(DRM.isStatusBarExpandPanelDisabled(mAdminName));
        switchAdbDisabled.setChecked(DRM.isAdbDisabled(mAdminName));
        switchTaskButtonDisabled.setChecked(DRM.isTaskButtonDisabled(mAdminName));
        switchSystemBrowserDisabled.setChecked(DRM.isSystemBrowserDisabled(mAdminName));
        switchScreenCaptureDisabled.setChecked(DRM.isScreenCaptureDisabled(mAdminName));
        switchChangeWallpaperDisabled.setChecked(DRM.isChangeWallpaperDisabled(mAdminName));
        switchSendNotificationDisabled.setChecked(DRM.isSendNotificationDisabled(mAdminName));
        switchDevelopmentOptionDisabled.setChecked(DSM.isDevelopmentOptionDisabled(mAdminName));
        switchRestoreFactoryDisabled.setChecked(DSM.isRestoreFactoryDisabled(mAdminName));

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mAdminName = new ComponentName(MainActivity.this, Receiver.class);

        try {
            DCM.setSilentActiveAdmin(mAdminName);
        } catch (Exception e) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("错误")
                    .setMessage("MDM无法激活 请确认是否激活设备管理器或是否完成内核破解")
                    .setPositiveButton("确定", null)
                    .show();
        }

        ArrayList<String> superWhiteListPackage = DHSM.getSuperWhiteListForHwSystemManger(mAdminName);
        if (superWhiteListPackage != null && !superWhiteListPackage.contains("com.cat.mdm")) {
            superWhiteListPackage.add("com.cat.mdm");
        } else {
            superWhiteListPackage = new ArrayList<String>(Arrays.asList(new String[]{"com.cat.mdm"}));
        }
        DHSM.setSuperWhiteListForHwSystemManger(mAdminName, superWhiteListPackage);

        DHSM.removeSuperWhiteListForHwSystemManger(mAdminName, superWhiteListPackage);

        // 载入设置
        try {
            File file = new File(getFilesDir(), "config.json");
            if (file.exists()) {
                FileInputStream fis = this.openFileInput(file.getName());
                int lenght = fis.available();
                byte[] buffer = new byte[lenght];
                fis.read(buffer);
                config = JSONObject.parseObject(new String(buffer, "UTF-8"));
                fis.close();
            } else {
                file.createNewFile();
                config = new JSONObject();
                config.put("Url", "");

                FileOutputStream fos = this.openFileOutput(file.getName(), MODE_PRIVATE);
                byte[] bytes = config.toJSONString().getBytes();
                fos.write(bytes);
                fos.close();
            }
        } catch (Exception e) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("错误")
                    .setMessage(e.getMessage())
                    .setPositiveButton("确定", null)
                    .show();
        }

        // 获取Edit
        editNetworkAccessBlacklist = findViewById(R.id.editNetworkAccessBlacklist);
        editNetworkAccessWhitelist = findViewById(R.id.editNetworkAccessWhitelist);
        editInstallPackageWhiteList = findViewById(R.id.editInstallPackageWhiteList);
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


        findViewById(R.id.fab).setOnClickListener(view -> {
            try {
                if (config.getString("Url") == "") {
                    // 应用策略
                    DRM.setWifiDisabled(mAdminName, switchWifiDisabled.isChecked());
                    DRM.setHomeButtonDisabled(mAdminName, switchHomeButtonDisabled.isChecked());
                    DRM.setExternalStorageDisabled(mAdminName, switchExternalStorageDisabled.isChecked());
                    DRM.setUSBDataDisabled(mAdminName, switchUSBDataDisabled.isChecked());
                    DRM.setStatusBarExpandPanelDisabled(mAdminName, switchStatusBarExpandPanelDisabled.isChecked());
                    DRM.setAdbDisabled(mAdminName, switchAdbDisabled.isChecked());
                    DRM.setTaskButtonDisabled(mAdminName, switchTaskButtonDisabled.isChecked());
                    DRM.setSystemBrowserDisabled(mAdminName, switchSystemBrowserDisabled.isChecked());
                    DRM.setScreenCaptureDisabled(mAdminName, switchScreenCaptureDisabled.isChecked());
                    DRM.setChangeWallpaperDisabled(mAdminName, switchChangeWallpaperDisabled.isChecked());
                    DRM.setSendNotificationDisabled(mAdminName, switchSendNotificationDisabled.isChecked());
                    DSM.setDevelopmentOptionDisabled(mAdminName, switchDevelopmentOptionDisabled.isChecked());
                    DSM.setRestoreFactoryDisabled(mAdminName, switchRestoreFactoryDisabled.isChecked());

                    List<String> listTo, listFrom;
                    if (!editNetworkAccessBlacklist.getText().toString().equals("")) {
                        listTo = Arrays.asList(editNetworkAccessBlacklist.getText().toString().split("\n"));
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

                    if (!editNetworkAccessWhitelist.getText().toString().equals("")) {
                        listTo = Arrays.asList(editNetworkAccessWhitelist.getText().toString().split("\n"));
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

                    if (!editInstallPackageWhiteList.getText().toString().equals("")) {
                        listTo = Arrays.asList(editInstallPackageWhiteList.getText().toString().split("\n"));
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
                } else {
                    SettingActivity.updateWebPolicies(false);

                    // 更改Edit内容
                    editNetworkAccessWhitelist.setText(String.join("\n", DNM.getNetworkAccessWhitelist(mAdminName)));
                    editNetworkAccessBlacklist.setText(String.join("\n", DNM.getNetworkAccessBlackList(mAdminName)));
                    editInstallPackageWhiteList.setText(String.join("\n", DPM.getInstallPackageWhiteList(mAdminName)));

                    // 更改Switch状态
                    switchWifiDisabled.setChecked(DRM.isWifiDisabled(mAdminName));
                    switchHomeButtonDisabled.setChecked(DRM.isHomeButtonDisabled(mAdminName));
                    switchExternalStorageDisabled.setChecked(DRM.isExternalStorageDisabled(mAdminName));
                    switchUSBDataDisabled.setChecked(DRM.isUSBDataDisabled(mAdminName));
                    switchStatusBarExpandPanelDisabled.setChecked(DRM.isStatusBarExpandPanelDisabled(mAdminName));
                    switchAdbDisabled.setChecked(DRM.isAdbDisabled(mAdminName));
                    switchTaskButtonDisabled.setChecked(DRM.isTaskButtonDisabled(mAdminName));
                    switchSystemBrowserDisabled.setChecked(DRM.isSystemBrowserDisabled(mAdminName));
                    switchScreenCaptureDisabled.setChecked(DRM.isScreenCaptureDisabled(mAdminName));
                    switchChangeWallpaperDisabled.setChecked(DRM.isChangeWallpaperDisabled(mAdminName));
                    switchSendNotificationDisabled.setChecked(DRM.isSendNotificationDisabled(mAdminName));
                    switchDevelopmentOptionDisabled.setChecked(DSM.isDevelopmentOptionDisabled(mAdminName));
                    switchRestoreFactoryDisabled.setChecked(DSM.isRestoreFactoryDisabled(mAdminName));
                }


            } catch (Exception e) {
                new AlertDialog.Builder(view.getContext())
                        .setTitle("错误")
                        .setMessage(e.getMessage())
                        .setPositiveButton("确定", null)
                        .show();
                return;
            }

            new AlertDialog.Builder(view.getContext())
                    .setTitle("成功")
                    .setMessage("策略已更新")
                    .setPositiveButton("确定", null)
                    .show();
        });

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
            Intent intent = new Intent(this, SettingActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    public MainActivity() {
        mainActivity = this;
    }

    public static MainActivity getMainActivity() {
        return mainActivity;
    }
}