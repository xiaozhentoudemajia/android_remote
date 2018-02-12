package aca.com.remote.upgrade;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import aca.com.remote.activity.BaseActivity;
import aca.com.remote.tunes.BackendService;
import aca.com.remote.tunes.daap.Library;
import aca.com.remote.tunes.daap.Session;
import aca.com.remote.tunes.util.ThreadExecutor;

/**
 * Created by gavin.liu on 2018/1/5.
 */

public class UpgradeModel {
    private final String TAG = "UpgradeModel";
    private Context mContext;
    private LocalUpdateTask localUpdateTask;
    private DeviceInfo mainDevice;
    private LibraryFirmwareUpdate updateLibrary;
    private String localPath;
    private String localTmpPath;
    private String localVersionFile;
    protected static Session session;
    private UpgPackage upgPackage;
    private UpgNotify notify;

    public enum UpgStatus {
        IDLE,
        LOCALUPDATE,
        VERSIONCHECK,
        FIRMWAREUPDATE};
    private UpgStatus status;
    private Timer localUpdateTimer;
    private static final long PERIOD = 10*1000;

    private LocalUpdateTaskListener listener = new LocalUpdateTaskListener() {
        @Override
        public boolean isLocalUpdateEnable(LocalUpdateTask localUpdateTask) {
            Log.d(TAG, "isLocalUpdateEnable");
            if (status == UpgStatus.IDLE)
                return true;

            return false;
        }

        @Override
        public void enterLocalUpdate(LocalUpdateTask localUpdateTask) {
            Log.d(TAG, "enterLocalUpdate");
            if (status == UpgStatus.IDLE) {
				notify.notifySatus("start to check remote version");
                status = UpgStatus.LOCALUPDATE;
            }
        }

        @Override
        public void findNewerVersion(LocalUpdateTask localUpdateTask) {
            Log.d(TAG, "findNewerVersion");
            notify.notifySatus("find newer version");

            //status = UpgStatus.VERSIONCHECK;
            upgPackage.initVersionInfo();

            if (upgPackage.isInited())
                notify.notifyLocalversion(upgPackage.getVersion());

			status = UpgStatus.IDLE;
        }

        @Override
        public void exitWithoutUpdate(LocalUpdateTask localUpdateTask) {
            Log.d(TAG, "exitWithoutUpdate");
			if (status == UpgStatus.LOCALUPDATE)
				notify.notifySatus("not find newer version");

			status = UpgStatus.IDLE;
            
            //startLocalUpdateTimer(PERIOD);
        }
    };

    public UpgPackage getUpgPackage() {
        return upgPackage;
    }

	public UpgStatus getUpgStatus() {
		return status;
	}

	public void setUpgStatus(UpgStatus upgstatus) {
		status = upgstatus;
	}
	
    private void addLocalUpdateTask() {
        localUpdateTask = new LocalUpdateTask(mContext, localPath, localTmpPath, localVersionFile);
        localUpdateTask.addListener(listener);
    }

    public void startLocalUpdateTimer(long delay, long period) {
		if (status == UpgStatus.IDLE)
        	localUpdateTimer.schedule(localUpdateTask,delay,period);
    }

    public UpgradeModel(Context context, Session session, UpgNotify notify) {
        this.mContext = context;
        this.session = session;
        this.status = UpgStatus.IDLE;
        this.notify = notify;

        localUpdateTimer = new Timer();
        Log.d(TAG, "UpgradeModel mContext:"+mContext);
        String path = mContext.getFilesDir().getPath() + File.separatorChar;
        Log.d(TAG, "UpgradeModel path:"+path);
        localPath = path + "upgrade" + File.separatorChar;
        localTmpPath = path + "upgradeTmp" + File.separatorChar;
        localVersionFile = "version";
        upgPackage = new UpgPackage(this.localPath);
        if (upgPackage.isInited())
            notify.notifyLocalversion(upgPackage.getVersion());

        addLocalUpdateTask();
        //startLocalUpdateTimer(0);
        //Log.d(TAG, "UpgradeModel session:"+session);
        //getMainDeviceInfo();
    }
}
