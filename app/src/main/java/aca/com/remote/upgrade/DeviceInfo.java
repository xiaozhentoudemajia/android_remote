package aca.com.remote.upgrade;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.health.TimerStat;
import android.util.Log;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import aca.com.remote.tunes.TagListener;
import aca.com.remote.tunes.daap.Response;
import aca.com.remote.tunes.daap.Session;
import aca.com.remote.tunes.util.ThreadExecutor;

/**
 * Created by Gavin.Liu on 2018/1/19.
 */

public class DeviceInfo {
    private final String TAG = "DeviceInfo";

    private String modelname = null;
    private String boardname = null;
    private String version = null;
    private List<PartitionInfo> partitions;

    private LibraryFirmwareUpdate updateLibrary;
    protected static Session session;
    protected int upgpart_count;
    protected int cur_index;
    protected PartitionInfo cur_part;

    private UpgNotify notify;

    public DeviceInfo(Session session, UpgNotify notify){
        this.session = session;
        this.notify = notify;
        this.upgpart_count = 0;
        partitions = new ArrayList<>();
        //getDeviceInfo();
    }

    public void getDeviceInfo() {
        ThreadExecutor.runTask(new Runnable() {
            @Override
            public void run() {
                if (session == null)
                    return;

                notify.notifySatus("start to get deviceinfo");

                updateLibrary = new LibraryFirmwareUpdate(session);
                modelname = updateLibrary.getModelname();
                notify.notifySatus("modelname: "+modelname);
                boardname = updateLibrary.getBoardname();
				notify.notifySatus("boardname: "+boardname);
                version = updateLibrary.getFirmwareVersion();
				notify.notifySatus("version: "+version);

                partitions = updateLibrary.getPartitions(partitions);
				notify.notifySatus("partitions size: "+partitions.size());
				
				for (PartitionInfo part : partitions) {
					notify.notifySatus("    "+part.getName()+"    "+part.getOffset()+"    "+part.getSize());
				}

				if (version != null)
                	notify.notifyFirmwareversion(version);            }
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

    public void addPartitionInfo(String name, long offset, long size, int flash) {
        PartitionInfo part = new PartitionInfo(name, offset, size);
        partitions.add(part);
    }

    public void setUpgpartCount(int count) {
        upgpart_count = count;
        ThreadExecutor.runTask(new Runnable() {
            @Override
            public void run() {
                int ret;
                notify.notifySatus("upgrade part number is "+upgpart_count);
                ret = updateLibrary.setUpgpartCount(upgpart_count);
                notify.notifySetCountStatus(ret);
            }
        });
    }

    public void setUpgpartinfo(final int index, PartitionInfo info) {
        cur_index = index;
        cur_part = info;

        ThreadExecutor.runTask(new Runnable() {
            @Override
            public void run() {
                int ret;
                ret = updateLibrary.setUpgpartinfo(cur_index, cur_part);
                notify.notifySetUpgPartStatus(index,ret);
            }
        });
    }

    Timer timer = new Timer();

    public void startBurnFirwmware() {
        ThreadExecutor.runTask(new Runnable() {
            @Override
            public void run() {
                int ret;
                ret = updateLibrary.startBurnFirmware();
                notify.notifyBurnStatus(ret);
                timer.schedule(new checkBurnReplyTask(), 0, 500);
                Log.d(TAG, "checkBurnReplyTask start timer");
            }
        });
    }

    public final static int BURNSTATE_IDLE = 0;
    public final static int BURNSTATE_DOWNLOADING = 1;
    public final static int BURNSTATE_COMPLETE = 2;
    public final static int BURNSTATE_ABORT = 3;
    public final static int BURNSTATE_PART_PROCESS = 4;
    public final static int BURNSTATE_PART_UPDATING = 5;
    public final static int BURNSTATE_PART_SUCCESS = 6;

    public class BurnReply {
        int state;
        int partidx;
        int process;
    }

    private class checkBurnReplyTask extends TimerTask {
        private int processBak = 0;
        @Override
        public void run() {
            BurnReply reply = new BurnReply();
            updateLibrary.getUpgradeStatus(reply);
            if (reply.state == BURNSTATE_COMPLETE) {
                notify.notifySatus("BURNSTATE_COMPLETE");
                timer.cancel();
            } else if (reply.state == BURNSTATE_ABORT) {
                notify.notifySatus("BURNSTATE_ABORT");
                timer.cancel();
            }  else if (reply.state == BURNSTATE_PART_UPDATING) {
                notify.notifySatus("BURNSTATE_PART_UPDATING upgpart"+reply.partidx);
            } else if (reply.state == BURNSTATE_PART_SUCCESS) {
                notify.notifySatus("BURNSTATE_PART_SUCCESS upgpart"+reply.partidx);
            } else if (reply.state == BURNSTATE_PART_PROCESS) {
                if (processBak != reply.process) {
                    notify.notifySatus("upgpart" + reply.partidx +
                            " process:" + reply.process);
                    processBak = reply.process;
                }
            }
        }
    }
 }

