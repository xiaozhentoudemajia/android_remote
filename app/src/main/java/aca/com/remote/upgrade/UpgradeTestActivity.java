package aca.com.remote.upgrade;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import aca.com.remote.R;
import aca.com.remote.activity.BaseActivity;
import aca.com.remote.tunes.BackendService;
import aca.com.remote.tunes.daap.Session;
import aca.com.remote.tunes.util.ThreadExecutor;

/**
 * Created by Gavin.Liu on 2018/1/22.
 */

public class UpgradeTestActivity extends BaseActivity implements UpgNotify {
    private final String TAG = "UpgTest";

    public final static int NOTIFY_SESSION_READY = 0x10;
    public final static int NOTIFY_FIRMWARE_VERSION = 0x20;
    public final static int NOTIFY_DOWNLOAD_PROCESS = 0x30;
    public final static int NOTIFY_BURN_PROCESS = 0x40;
    public final static int NOTIFY_BURN_ROOTFS = 0x50;
    public final static int NOTIFY_CHECK_UPG_RESULT = 0x60;
    public final static int NOTIFY_NOTICE_UPDATE = 0x70;

    private Context mContext;
    private UpgNotify upgNotify;
    private Button buttonUpdate;
    private Button buttonCheck;
    private TextView upg_firmwareversion;
    private TextView upg_notify;
    private String notifyStr;

    private BackendService backendService;
    private Session session;
    private DeviceInfo mainDevice;

    protected int cur_index;

    public ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, final IBinder iBinder) {
            ThreadExecutor.runTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        backendService = ((BackendService.BackendBinder) iBinder).getService();
                        session = backendService.getSession();
                        if (session != null) {
                            Log.d(TAG, "onServiceConnected session:" + session);
                            mHandler.sendEmptyMessage(NOTIFY_SESSION_READY);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "getModelname Exception:" + e.getMessage());
                    }
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            backendService = null;
            session = null;
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        this.bindService(new Intent(this, BackendService.class), connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {;
        super.onStop();
        this.unbindService(connection);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        upgNotify = this;

        setContentView(R.layout.activity_upgrade_test);
        buttonCheck = (Button) findViewById(R.id.updateCheck);
        buttonUpdate = (Button) findViewById(R.id.updateStart);
        upg_firmwareversion = (TextView) findViewById(R.id.upg_firmwareversion);
        upg_notify = (TextView) findViewById(R.id.upg_notify);

        buttonCheck.setClickable(false);
        buttonUpdate.setClickable(false);

        buttonCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "click updateLocalPackage");
                if (mainDevice != null) {
                    if(0 == mainDevice.getState())
                        mainDevice.checkUpgVersion();
                }
            }
        });

        buttonUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "click updateFirmware");
                    if (mainDevice != null) {
                        if (0 == mainDevice.getState()) {
                            mainDevice.startUpgrade();
                        }
                    }
            }
        });

    }

    public void notifySatus(String info) {
        Log.d(TAG, "notifySatus:" + info);
        mHandler.sendMessage(mHandler.obtainMessage(NOTIFY_NOTICE_UPDATE, info));
    }

    @Override
    public void notifyDownloadSatus(int percent) {
        mHandler.sendMessage(mHandler.obtainMessage(NOTIFY_DOWNLOAD_PROCESS, percent,0));
    }

    public void notifyCheckUpgResult(String result) {
        Log.d(TAG, "notifyCheckUpgResult:" + result);
        mHandler.sendMessage(mHandler.obtainMessage(NOTIFY_CHECK_UPG_RESULT, result));
    }

    public void notifyFirmwareversion(String version) {
        Log.d(TAG, "notifyFirmwareversion:" + version);
        mHandler.sendMessage(mHandler.obtainMessage(NOTIFY_FIRMWARE_VERSION, version));
    }

    public void notifyBurnStatus(int percent) {
        Log.d(TAG, "notifyBurnStatus:" + percent);
        mHandler.sendMessage(mHandler.obtainMessage(NOTIFY_BURN_PROCESS, percent,0));
    }

    @Override
    public void notifyBurnRootfs() {
        mHandler.sendMessage(mHandler.obtainMessage(NOTIFY_BURN_ROOTFS));
    }

    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NOTIFY_SESSION_READY:
                    Log.d(TAG, "NOTIFY_SESSION_READY");
                    mainDevice = new DeviceInfo(session, upgNotify);
                    mainDevice.getDeviceInfo();
                    buttonCheck.setClickable(true);
                    break;
                case NOTIFY_CHECK_UPG_RESULT:
                    Log.d(TAG, "NOTIFY_CHECK_UPG_RESULT");
                    String result = (String) msg.obj;
                    if(result.equalsIgnoreCase("UPG_NEW")) {
                        buttonUpdate.setClickable(true);
                        notifyStr += "\n"+"Have new version,Please update!";
                    }else{
                        notifyStr += "\n"+"Already is new .";
                    }
                    upg_notify.setText(notifyStr);
                    break;

                case NOTIFY_FIRMWARE_VERSION:
                    Log.d(TAG, "NOTIFY_FIRMWARE_VERSION");
                    upg_firmwareversion.setText("SW Ver: " + (String) msg.obj);
                    break;
                case NOTIFY_NOTICE_UPDATE:
                    notifyStr += "\n" +(String)msg.obj;
                    upg_notify.setText(notifyStr);
                    break;
                case NOTIFY_DOWNLOAD_PROCESS:
                    if(notifyStr.contains("download process: ...")){
                        notifyStr += ".";
                    }else{
                        notifyStr += "\n\n" +"download process: ...";
                    }
                    if(30==msg.arg1)
                        notifyStr += "\n";
                    upg_notify.setText(notifyStr);
                    break;
                case NOTIFY_BURN_PROCESS:
                    if(notifyStr.contains("burn process: ...")){
                        notifyStr += ".";
                    }else{
                        notifyStr += "\n\n" +"burn process: ...";
                    }
                    if(30==msg.arg1)
                        notifyStr += "\n";
                    upg_notify.setText(notifyStr);
                    break;
                case NOTIFY_BURN_ROOTFS:
                    notifyStr += "\n\n" +  "Burn rootfs ...\n It's need about 5 minutes.\n Dont power off!";
                    upg_notify.setText(notifyStr);
                    break;
            }
        }


    };
}