package com.google.android.mgc.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;

public class MainActivity extends AppCompatActivity {
    private static final int GETPERMISSION_SUCCESS = 1;//获取权限成功
    private static final int GETPERMISSION_FAILER = 2;//获取权限失败
    private TextView tv_id;
    private TextView tv_name;
    private Button bt_id;
    private Button bt_aliving;
    private Button bt_videorec;

    private int MY_SCAN_REQUEST_CODE = 100;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        bt_id = (Button) findViewById(R.id.btn_id);
        bt_aliving = (Button) findViewById(R.id.btn_aliving);
        bt_videorec = (Button) findViewById(R.id.btn_videorec);

        bt_id.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, IDActivity.class);
                startActivity(intent);
            }
        });

        bt_aliving.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, CameraActivity.class);
                startActivity(intent);
            }
        });

        bt_videorec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, RecognizeActivity.class);
                startActivity(intent);
            }
        });
    }

}
