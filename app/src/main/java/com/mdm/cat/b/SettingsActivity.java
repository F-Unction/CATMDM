package com.mdm.cat.b;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.EditText;

public class SettingsActivity extends AppCompatActivity {

    private static SettingsActivity settingsActivity;

    public static EditText urlEdit;

    public SettingsActivity() {
        settingsActivity = this;
    }

    @Override
    protected void onStart() {
        super.onStart();
        urlEdit.setEnabled(false);
        try {
            if (Utils.config.getString("Url") != "") {
                Utils.updateWebPolicies(false);
            }
        } catch (Exception e) {
            new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle("错误")
                    .setMessage(e.getMessage())
                    .setPositiveButton("确定", null)
                    .show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        urlEdit = findViewById(R.id.editUrl);

        urlEdit.setText(Utils.config.getString("Url"));

        findViewById(R.id.fab2).setOnClickListener(view -> {
            try {
                Utils.updateWebPolicies(true);
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

    public static SettingsActivity getSettingsActivity() {
        return settingsActivity;
    }
}