package aca.com.remote.upgrade;

import android.util.Log;

import aca.com.remote.tunes.daap.ConnectionResponseListener;
import aca.com.remote.tunes.daap.RequestHelper;
import aca.com.remote.tunes.daap.Response;
import aca.com.remote.tunes.daap.ResponseParser;
import aca.com.remote.tunes.daap.Session;

//import java.util.concurrent.locks.StampedLock;

/**
 * Created by gavin.liu on 2018/1/12.
 */
public class LibraryFirmwareUpdate {
    private final String TAG = "Update";
    protected static Session session;
    private int errCode;
    private String msg;

    private ConnectionResponseListener listener = new ConnectionResponseListener() {
        @Override
        public void oResponse(int code, String message) {
            errCode = 0;
            msg = null;
            Log.d(TAG, "setUpgpartinfo code:"+code+" msg: "+message);
            if (code != 417)
                return;

            String[] strs = message.split("#");
            errCode = Integer.parseInt(strs[0]);
            msg = strs[1];
            Log.d(TAG, "setUpgpartinfo errCode:"+errCode+" msg: "+msg);
        }
    };

    public LibraryFirmwareUpdate(Session session){
        Log.d(TAG, "LibraryFirmwareUpdate");
        this.session = session;
    }

    public String getModelname() {
        try {
            Log.d(TAG, "getModelname() requesting...");

            byte[] raw = RequestHelper.request(
                    String.format("%s/upgrade?request=modelname", session.getRequestBase()), false);
            Response response = ResponseParser.performParse(raw);
            return response.getNested("mupd").getString("minm");
        } catch (Exception e) {
            Log.w(TAG, "getModelname Exception:" + e.getMessage());
        }
        return null;
    }

    public String getBoardname() {
        try {
            Log.d(TAG, "getBoardname() requesting...");

            byte[] raw = RequestHelper.request(
                    String.format("%s/upgrade?request=boardname", session.getRequestBase()), false);
            Response response = ResponseParser.performParse(raw);
            return response.getNested("mupd").getString("minm");
        } catch (Exception e) {
            Log.w(TAG, "getBoardname Exception:" + e.getMessage());
        }
        return null;
    }

    public String getFirmwareVersion() {
        try {
            Log.d(TAG, "getFirmwareVersion() requesting...");

            byte[] raw = RequestHelper.request(
                    String.format("%s/upgrade?request=version", session.getRequestBase()), false);
            Response response = ResponseParser.performParse(raw);
            return response.getNested("mupd").getString("minm");
        } catch (Exception e) {
            Log.w(TAG, "getFirmwareVersion Exception:" + e.getMessage());
        }
        return null;
    }

    public String checkUpgVersion() {
        try {
            Log.d(TAG, "getFirmwareVersion() requesting...");

            byte[] raw = RequestHelper.request(
                    String.format("%s/upgrade?request=checkupg", session.getRequestBase()), false);
            Response response = ResponseParser.performParse(raw);
            return response.getNested("mupd").getString("minm");
        } catch (Exception e) {
            Log.w(TAG, "getFirmwareVersion Exception:" + e.getMessage());
        }
        return null;
    }

    public int startUpgradeFirmware() {
        try {
            String url = String.format("%s/upgrade?startupg=1",session.getRequestBase());
            Log.d(TAG, "startUpgradeFirmware: "+ url);
            byte[] raw = RequestHelper.request(url,false);
            Response response = ResponseParser.performParse(raw);
        } catch (Exception e) {
            Log.w(TAG, "startBurnFirmware Exception:" + e.getMessage());
        }
        return 0;
    }

    public void getUpgradeStatus(DeviceInfo.BurnReply reply) {
        try {
            Log.d(TAG, "getUpgradeStatus() requesting...");

            byte[] raw = RequestHelper.request(
                    String.format("%s/upgrade?getstatus=0", session.getRequestBase()), false);
            Response response = ResponseParser.performParse(raw);
            reply.state = response.getNested("mupd").getNumber("mstt").intValue();
            reply.partidx = response.getNested("mupd").getNumber("mupi").intValue();
            reply.process = response.getNested("mupd").getNumber("mupp").intValue();;
        } catch (Exception e) {
            Log.w(TAG, "getFirmwareVersion Exception:" + e.getMessage());
        }
    }
}
