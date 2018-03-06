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
    private Context mContext;
    private UpgNotify upgNotify;
    private Button updateLocalPackage;
    private Button updateFirmware;
    private TextView upg_localversion;
    private TextView upg_firmwareversion;
    private TextView upg_notify;
    private String notifyStr;

    private BackendService backendService;
    private Session session;

    private UpgradeModel upgradeModel;
    private DeviceInfo mainDevice;
    private UpgPackage upgPackage;

    protected int cur_index;

    public final static int NOTIFY_SESSION_READY = 0x10;
    public final static int NOTIFY_FIRMWARE_READY = 0x20;
    public final static int NOTIFY_LOACLPACK_UPDATE = 0x30;
    public final static int NOTIFY_NOTICE_UPDATE = 0x40;
    public final static int NOTIFY_DOWNLOAD_COUNT = 0x50;
    public final static int NOTIFY_DOWNLOAD_PART = 0x51;
    public final static int NOTIFY_DOWNLOAD_PART_FAIL = 0x52;

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
        updateLocalPackage = (Button) findViewById(R.id.updateLocalPackage);
        updateFirmware = (Button) findViewById(R.id.updateFirmware);
        upg_localversion = (TextView) findViewById(R.id.upg_localversion);
        upg_firmwareversion = (TextView) findViewById(R.id.upg_firmwareversion);
        upg_notify = (TextView) findViewById(R.id.upg_notify);

        updateLocalPackage.setClickable(false);
        updateFirmware.setClickable(false);

        updateLocalPackage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "click updateLocalPackage");

                if (upgradeModel.getUpgStatus() != UpgradeModel.UpgStatus.IDLE) {
                    notifySatus("Now is updating local package or updating firmware, please wait");
                    return;
                }

                if (upgradeModel != null)
                    upgradeModel.startLocalUpdateTimer(0, 1000*30);
            }
        });

        updateFirmware.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "click updateFirmware");

                    if (upgradeModel.getUpgStatus() != UpgradeModel.UpgStatus.IDLE) {
                        notifySatus("Now is updating local package or updating firmware, please wait");
                        return;
                    }

                    upgradeModel.setUpgStatus(UpgradeModel.UpgStatus.FIRMWAREUPDATE);

                    if (mainDevice != null)
                        mainDevice.getDeviceInfo();
            }
        });

    }

    public void notifySatus(String info) {
        Log.d(TAG, "notifySatus:" + info);
        mHandler.sendMessage(mHandler.obtainMessage(NOTIFY_NOTICE_UPDATE, info));
    }

    public void notifyLocalversion(String version) {
        Log.d(TAG, "notifyLocalversion:" + version);
        mHandler.sendMessage(mHandler.obtainMessage(NOTIFY_LOACLPACK_UPDATE, version));
    }

    public void notifyFirmwareversion(String version) {
        Log.d(TAG, "notifyFirmwareversion:" + version);
        mHandler.sendMessage(mHandler.obtainMessage(NOTIFY_FIRMWARE_READY, version));
    }

    public void notifySetCountStatus(int result) {
        Log.d(TAG, "notifySetCountStatus:" + result);
        mHandler.sendMessage(mHandler.obtainMessage(NOTIFY_DOWNLOAD_COUNT, result));
    }

    public void notifySetUpgPartStatus(int index, int result){
        Log.d(TAG, "notifySetUpgPartStatus index:"+index+"  result: " + result);
        if (result != 0)
            mHandler.sendMessage(mHandler.obtainMessage(NOTIFY_DOWNLOAD_PART_FAIL, index, result));
        else
            mHandler.sendMessage(mHandler.obtainMessage(NOTIFY_DOWNLOAD_PART, index, result));
    }

    public void notifyBurnStatus(int result) {
        Log.d(TAG, "notifyBurnStatus:" + result);
    }

    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NOTIFY_SESSION_READY:
                    Log.d(TAG, "NOTIFY_SESSION_READY");
                    upgradeModel = new UpgradeModel(mContext, session, upgNotify);
                    mainDevice = new DeviceInfo(session, upgNotify);

                    if (upgradeModel != null)
                        updateLocalPackage.setClickable(true);
                    break;
                case NOTIFY_FIRMWARE_READY:
                    Log.d(TAG, "NOTIFY_FIRMWARE_READY");
                    upg_firmwareversion.setText("firmwareversion: " + (String) msg.obj);

                    if (mainDevice!=null && upgPackage!=null) {
                        startUpgrade(mainDevice, upgPackage);
                    }
                    break;
                case NOTIFY_LOACLPACK_UPDATE:
                    upg_localversion.setText("localversion: " + (String) msg.obj);
                    upgPackage = upgradeModel.getUpgPackage();
                    if (mainDevice!=null && upgPackage!=null)
                        updateFirmware.setClickable(true);
                    break;
                case NOTIFY_NOTICE_UPDATE:
                    Log.d(TAG, "NOTIFY_NOTICE_UPDATE");
                    notifyStr += "\n" + (String) msg.obj;
                    upg_notify.setText(notifyStr);
                    break;
                case NOTIFY_DOWNLOAD_COUNT:
                    cur_index = 0;
                case NOTIFY_DOWNLOAD_PART:
                    if (cur_index >= upgPackage.getUpglists().size()) {
                        notifyStr += "\n" + "start to burn";
                        upg_notify.setText(notifyStr);
                        mainDevice.startBurnFirwmware();
                        upgradeModel.setUpgStatus(UpgradeModel.UpgStatus.IDLE);
                        break;
                    }
                    PartitionInfo part = upgPackage.getUpglists().get(cur_index);
                    Log.d(TAG, "partname:"+part.getName());
                    notifyStr += "\n" + "send part:" + part.getName();
                    upg_notify.setText(notifyStr);
                    mainDevice.setUpgpartinfo(cur_index, part);
                    cur_index++;
                    break;
                case NOTIFY_DOWNLOAD_PART_FAIL:
                    cur_index = 0;
                    notifyStr += "\n" +  "send part("+msg.arg1+") fail("+msg.arg2+")";
                    upg_notify.setText(notifyStr);
                    upgradeModel.setUpgStatus(UpgradeModel.UpgStatus.IDLE);
                    break;
            }
        }


    };

    private void updateNotify(String str) {
        notifyStr += "\n" + str;
        upg_notify.setText(notifyStr);
    }

    private void startUpgrade(DeviceInfo device,UpgPackage upgPackage) {
        if (!upgPackage.checkModelname(device.getModelname())) {
            updateNotify("Modelname is not match");
            return;
        }
        if (!upgPackage.checkBoardname(device.getBoardname())) {
            updateNotify("not find newer version");
            return;
        }
        if (upgPackage.compareVersion(device.getVersion()) <= 0) {
            Log.d(TAG, "not find newer version");
            notifyStr += "\n" + "not find newer version";
            upg_notify.setText(notifyStr);
            return;
        }

        updateNotify("parsering local upgpackage");
        upgPackage.parseScript();
        List<PartitionInfo> upgList = upgPackage.getUpglists();
        Log.d(TAG, "upgList.size():"+upgList.size());

        setUpgPath(upgPackage.getUnzipDir());

        //int ret;
        mainDevice.setUpgpartCount(upgList.size());
        //Log.d(TAG, "setUpgpartCount ret:"+ret);
    }
}
