package aca.com.remote.upgrade;

import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import aca.com.remote.tunes.daap.Session;
import aca.com.remote.tunes.util.ThreadExecutor;

/**
 * Created by Gavin.Liu on 2018/1/19.
 */

public class DeviceInfo {
    private final String TAG = "DeviceInfo";

    public final static int BURNSTATE_IDLE = 0;
    public final static int BURNSTATE_DOWNLOADING = 1;
    public final static int BURNSTATE_COMPLETE = 2;
    public final static int BURNSTATE_ABORT = 3;
    public final static int BURNSTATE_PART_PROCESS = 4;
    public final static int BURNSTATE_PART_UPDATING = 5;
    public final static int BURNSTATE_PART_SUCCESS = 6;
    public final static int BURNSTATE_BURN_ROOTFS = 7;

    private String modelname = null;
    private String boardname = null;
    private String version = null;
    private String checkupg = null;
    private int mState =0 ;
    private LibraryFirmwareUpdate updateLibrary;
    protected static Session session;
    protected int upgpart_count;
    private UpgNotify notify;

    public DeviceInfo(Session session, UpgNotify notify){
        this.session = session;
        this.notify = notify;
        this.upgpart_count = 0;
        updateLibrary = new LibraryFirmwareUpdate(session);
        //getDeviceInfo();
    }

    public void getDeviceInfo() {
        ThreadExecutor.runTask(new Runnable() {
            @Override
            public void run() {
                if (session == null)
                    return;

                notify.notifySatus("start to get deviceinfo");
                modelname = updateLibrary.getModelname();
                notify.notifySatus("modelname: "+modelname);
                boardname = updateLibrary.getBoardname();
				notify.notifySatus("boardname: "+boardname);
                version = updateLibrary.getFirmwareVersion();
				notify.notifySatus("version: "+version);
                BurnReply reply = new BurnReply();
                updateLibrary.getUpgradeStatus(reply);
                mState = reply.state;
				if (version != null)
                	notify.notifyFirmwareversion(version);            }
        });
    }

    public void checkUpgVersion() {
        ThreadExecutor.runTask(new Runnable() {
            @Override
            public void run() {
                if (session == null)
                    return;
                checkupg = updateLibrary.checkUpgVersion();
                if (checkupg != null) {
                    Log.i(TAG,"checkupg:"+checkupg);
                    notify.notifyCheckUpgResult(checkupg);
                }
            }
        });
    }
    public void startUpgrade() {
        ThreadExecutor.runTask(new Runnable() {
            @Override
            public void run() {
                if (session == null)
                    return;
                updateLibrary.startUpgradeFirmware();
                timer.schedule(new checkBurnReplyTask(), 0, 500);
                Log.d(TAG, "checkBurnReplyTask start timer");
            }
        });
    }
    public boolean isInfoInited() {
        if (version == null)
            return false;

        return true;
    }

    public String getModelname() {
        return modelname;
    }

    public String getBoardname() {
        return boardname;
    }

    public String getVersion() {
        return version;
    }

    public int getState(){
        return mState;
    }
    Timer timer = new Timer();

    public class BurnReply {
        int state;
        int partidx;
        int process;
    }

    private class checkBurnReplyTask extends TimerTask {
        private int processBak = 0;
        private int lastDownloadprocess = 0;
        @Override
        public void run() {
            Log.d(TAG, "checkBurnReplyTask run");
            BurnReply reply = new BurnReply();
            updateLibrary.getUpgradeStatus(reply);
            mState = reply.state;
            Log.d(TAG, "checkBurnReplyTask state:"+reply.state);
            if(reply.state == BURNSTATE_DOWNLOADING){
                if(lastDownloadprocess != reply.process) {
                    notify.notifyDownloadSatus(reply.process);
                    lastDownloadprocess = reply.process;
                }
            }
            if (reply.state == BURNSTATE_COMPLETE) {
                notify.notifySatus("BURNSTATE_COMPLETE");
                timer.cancel();
            } else if (reply.state == BURNSTATE_ABORT) {
                notify.notifySatus("BURNSTATE_ABORT");
                timer.cancel();
            } else if (reply.state == BURNSTATE_PART_PROCESS) {
                if (processBak != reply.process) {
                    notify.notifyBurnStatus(reply.process);
                    processBak = reply.process;
                }
            } else if (reply.state == BURNSTATE_BURN_ROOTFS) {
                notify.notifyBurnRootfs();
                timer.cancel();
            }
        }
    }
 }

