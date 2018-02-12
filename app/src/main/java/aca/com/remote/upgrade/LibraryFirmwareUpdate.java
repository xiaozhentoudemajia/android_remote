package aca.com.remote.upgrade;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.util.StringBuilderPrinter;

import com.sun.mail.iap.ResponseHandler;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//import java.util.concurrent.locks.StampedLock;

import aca.com.remote.activity.BaseActivity;
import aca.com.remote.activity.MainActivity;
import aca.com.remote.tunes.BackendService;
import aca.com.remote.tunes.TagListener;
import aca.com.remote.tunes.daap.Library;
import aca.com.remote.tunes.daap.RequestHelper;
import aca.com.remote.tunes.daap.Response;
import aca.com.remote.tunes.daap.ResponseParser;
import aca.com.remote.tunes.daap.Session;
import aca.com.remote.tunes.jmdns.impl.ListenerStatus;
import aca.com.remote.uitl.L;

/**
 * Created by gavin.liu on 2018/1/12.
 */
public class LibraryFirmwareUpdate {
    private final String TAG = "Update";
    protected static Session session;

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

    public List<PartitionInfo> getPartitions(final List<PartitionInfo> parts) {
        try {
            Log.d(TAG, "getPartitions() requesting...");

            byte[] raw = RequestHelper.request(
                    String.format("%s/upgrade?request=partitions", session.getRequestBase()), false);
            Response response = ResponseParser.performParse(raw);

            Response mupd = response.getNested("mupd");
            if (mupd != null)
                parts.clear();

            int count = mupd.getNumber("mrco").intValue();
            Log.d(TAG, "getPartitions() get count:"+ count);

            List<Response> mlitList = mupd.getNested("mlcl").findArray("mlit");

            for (Response mlit : mlitList) {
                PartitionInfo part = new PartitionInfo(mlit.getString("minm"),
                        mlit.getNumberLong("mupo"), mlit.getNumberLong("mups"));
                parts.add(part);
            }

        } catch (Exception e) {
            Log.w(TAG, "getPartitions Exception:" + e.getMessage());
            parts.clear();
        }
        return parts;
    }

    public int setUpgpartCount(int count) {
        try {
            String url = String.format("%s/upgrade?count=%d", session.getRequestBase(),count);
            Log.d(TAG, "setUpgpartCount: "+ url);
            byte[] raw = RequestHelper.request(url,false);
            Response response = ResponseParser.performParse(raw);
        }catch (Exception e) {
            Log.w(TAG, "setUpgpartCount Exception:" + e.getMessage());
        }
        return 0;
    }

    public int setUpgpartinfo(int index, PartitionInfo info){
        try {
            String url = String.format("%s/upgrade/%d/items?name=%s&offset=%d&size=%d&file=%s",
                    session.getRequestBase(), index,
                    info.getName(), info.getOffset(), info.getSize(), info.getFilePath());
            Log.d(TAG, "setUpgpartinfo: "+ url);
            byte[] raw = RequestHelper.request(url,false);
            Response response = ResponseParser.performParse(raw);
        } catch (Exception e) {
            Log.w(TAG, "setUpgpartinfo Exception:" + e.getMessage());
        }
        return 0;
    }

    public int startBurnFirmware() {
        try {
            String url = String.format("%s/upgrade?burn=0",session.getRequestBase());
            Log.d(TAG, "startBurnFirmware: "+ url);
            byte[] raw = RequestHelper.request(url,false);
            Response response = ResponseParser.performParse(raw);
        } catch (Exception e) {
            Log.w(TAG, "startBurnFirmware Exception:" + e.getMessage());
        }
        return 0;
    }

    public int postUpgpart(int index, PartitionInfo info){
        try {
            String url = String.format("%s/upgrade/%d/part",
                    session.getRequestBase(), index);
            Log.d(TAG, "postUpgpart: "+ url);
            Map<String, String> params = new HashMap<String, String>();
            params.put("name", info.getName());
            params.put("offset", String.valueOf(info.getOffset()));
            params.put("size", String.valueOf(info.getSize()));

            Map<String, File> files = new HashMap<String, File>();
            File file = new File(info.getFilePath());
            files.put(file.getName(), file);

            LibraryFirmwareUpdate.post(url,params,files);
        } catch (Exception e) {
            Log.w(TAG, "setUpgpartinfo Exception:" + e.getMessage());
        }
        return 0;
    }

    public int downloadPackage() {
        try{
            Log.d(TAG, "downloadPackage");

            String file = "testpath/temp";
            String url = String.format("%s/upgrade?download=%s", session.getRequestBase(),file);
            byte[] raw = RequestHelper.request(url,false);
            Response response = ResponseParser.performParse(raw);

        } catch (Exception e) {
            Log.w(TAG, "downloadPackage Exception:" + e.getMessage());
        }
        return 1;
    }

    public static void post(String actionUrl, Map<String, String> params, Map<String, File> files)
            throws IOException {
        String BOUNDARY = java.util.UUID.randomUUID().toString();
        String PREFIX = "--";
        String LINEND = "\r\n";

        URL uri = new URL(actionUrl);
        HttpURLConnection conn = (HttpURLConnection) uri.openConnection();
        conn.setReadTimeout(5*1000);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Charsert", "UTF-8");
        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary="+BOUNDARY);

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            sb.append(PREFIX);
            sb.append(BOUNDARY);
            sb.append(LINEND);
            sb.append("Content-Disposition: form-data; name=\""+entry.getKey()+"\""+LINEND);
            sb.append("Content-Type: text/plain; charset=UTF-8"+LINEND);
            sb.append("Content-Transfer-Encoding: 8bit"+LINEND);
            sb.append(LINEND);
            sb.append(entry.getValue());
            sb.append(LINEND);
        }

        Log.d("post", "sb: "+ sb);
        DataOutputStream outStream = new DataOutputStream(conn.getOutputStream());
        outStream.write(sb.toString().getBytes());
        InputStream in = null;
        if (files != null) {
            for (Map.Entry<String, File> file : files.entrySet()) {
                StringBuilder sbf = new StringBuilder();
                sbf.append(PREFIX);
                sbf.append(BOUNDARY);
                sbf.append(LINEND);
                sbf.append("Content-Disposition: form-data; name=\"file\"; filename=\""
                        +file.getKey()+"\""+LINEND);
                sbf.append("Content-Type: application/octet-stream; charset=UTF-8"+LINEND);
                sbf.append(LINEND);
                Log.d("post", "sbf: "+ sbf);
                outStream.write(sbf.toString().getBytes());

                InputStream is = new FileInputStream(file.getValue());
                byte[] buffer = new byte[1024];
                int len = 0;
                while ((len = is.read(buffer)) != -1) {
                    Log.d("post", "send("+len+"): "+buffer[0]+" "+ buffer[1]);
                    outStream.write(buffer, 0, len);
                }

                is.close();
                outStream.write(LINEND.getBytes());
            }

            byte[] end_data = (PREFIX+BOUNDARY+PREFIX+LINEND).getBytes();
            Log.d("post", "end_data: "+ end_data);
            outStream.write(end_data);
            outStream.flush();

            StringBuilder sb2 = new StringBuilder();
            int res = conn.getResponseCode();
            if (res == 200) {
                in = conn.getInputStream();
                int ch;

                while ((ch = in.read()) != -1) {
                    sb2.append((char) ch);
                }

                Log.d("post", "Response: "+ sb2);
            }
            outStream.close();
            conn.disconnect();
            Log.d("post", "exit");
        }
    }
}
