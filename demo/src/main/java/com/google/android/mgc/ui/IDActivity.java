package com.google.android.mgc.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;
import com.google.android.mgc.ui.IDCameraActivity;
import com.tzutalin.dlib.Constants;

import java.io.File;

public class IDActivity extends AppCompatActivity {
    private static final int GETPERMISSION_SUCCESS = 1;//获取权限成功
    private static final int GETPERMISSION_FAILER = 2;//获取权限失败
    private EditText tv_id;
    private EditText tv_name;
    private ImageView tv_idpic;
    private Button tv_compare;
    private int MY_SCAN_REQUEST_CODE = 100;
    private Context mContext;
    private MyHandler myHandler = new MyHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.id_activity_main);
        mContext = this;
        tv_id = (EditText) findViewById(R.id.tv_id);
        tv_name = (EditText) findViewById(R.id.tv_name);
        tv_idpic = (ImageView) findViewById(R.id.tv_idpic);
        findViewById(R.id.btn_go).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent scanIntent = new Intent(mContext, IDCameraActivity.class);
                startActivityForResult(scanIntent, MY_SCAN_REQUEST_CODE);
            }
        });

        tv_compare = (Button) findViewById(R.id.btn_compare);
        tv_compare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, IDPicRecognizeActivity.class);
                startActivity(intent);
            }
        });
    }

    private void requestAllPermission() {
        PermissionsManager.getInstance().requestAllManifestPermissionsIfNecessary(IDActivity.this,
                new PermissionsResultAction() {
                    @Override
                    public void onGranted() {
                        myHandler.sendEmptyMessage(GETPERMISSION_SUCCESS);
                    }

                    @Override
                    public void onDenied(String permission) {
                        myHandler.sendEmptyMessage(GETPERMISSION_FAILER);
                    }
                });
    }

    //因为权限管理类无法监听系统，所以需要重写onRequestPermissionResult方法，更新权限管理类，并回调结果。这个是必须要有的。
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MY_SCAN_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String id = data.getStringExtra("id");
            String name = data.getStringExtra("name");
            //Toast.makeText(this, id, Toast.LENGTH_LONG).show();
            if (id != null && id.length() == 18 && name != null) {
                tv_id.setText(id);
                tv_name.setText(name);

                if (tv_idpic != null) {
                    String path = Constants.getDLibDirectoryPath() + "/idimage/face.jpg";
                    File mFile=new File(path);

                    if (mFile.exists()) {
                        Bitmap bitmap= BitmapFactory.decodeFile(path);

                        if(bitmap != null)
                            tv_idpic.setImageBitmap(bitmap);
                    }
                }
                tv_name.setFocusable(true);
                tv_name.setCursorVisible(true);
                tv_name.setFocusableInTouchMode(true);
                tv_name.requestFocus();

                tv_idpic.setVisibility(View.VISIBLE);
                tv_compare.setVisibility(View.VISIBLE);
            }
        }
    }


    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case GETPERMISSION_SUCCESS:
                    Intent scanIntent = new Intent(mContext, IDCameraActivity.class);
                    startActivityForResult(scanIntent, MY_SCAN_REQUEST_CODE);
                    break;
                case GETPERMISSION_FAILER:
                    Toast.makeText(mContext, "此功能须获摄像头权限1111111", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }
}
