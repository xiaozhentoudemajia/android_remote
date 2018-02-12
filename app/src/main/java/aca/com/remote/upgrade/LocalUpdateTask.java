package aca.com.remote.upgrade;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by gavin.liu on 2018/1/5.
 */

public class LocalUpdateTask extends TimerTask {
    private final String TAG = "LocalUpdateTask";
    private static final long PERIOD = 10*1000;
    //private String urlBase = "http://192.168.137.127/";
    private String urlBase = "https://github.com/hongzz/upgpackage/raw/master/";
    private String versionUrl = urlBase + "version_WIFISPEAKER";
    private enum DownloadType {VERSION, UPGPACKAGE};
    private Context mContext;
    private String localPath = null;
    private String localTmpPath = null;
    private String localVersionFile = null;

    private String localVersionPath = null;
    private String localVersionPathTmp = null;
    private String localPackagePathTmp = null;

    private UpgPackage upgPackage;
    private UpgPackage tmpPackage;

    private List<LocalUpdateTaskListener> listeners;

    public enum LocalStatus {
        IDLE,
        DOWNLOADVERSION,
        DOWNLOADPACKKAGE,
        UPDATING,
        UPDATESUCCESS,
    };
    private LocalStatus localStatus;

    public LocalUpdateTask(Context context, String localPath, String tmpPath, String verName){
        this.mContext = context;
        this.localPath = localPath;
        this.localTmpPath = tmpPath;
        this.localVersionFile = verName;

        localVersionPath = this.localPath + localVersionFile;
        localVersionPathTmp = this.localTmpPath + localVersionFile;

        Log.d(TAG, "localVersionPath："+localVersionPath);
        Log.d(TAG, "localVersionPathTmp："+localVersionPathTmp);
/*
        try {
            Log.d(TAG, "del "+ this.localPath);
            // test
            File dir = new File(this.localPath);
            FileUtils.deleteDirectory(dir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
        upgPackage = new UpgPackage(this.localPath);
        localStatus = LocalStatus.IDLE;
        listeners = new ArrayList<>();
    }

    @Override
    public void run() {        
        if (isLocalUpdateEnable()) {
			notifyEnterLocalUpdate();
            downloadVersion();
        } else {
            //notifyExitWithoutUpdate();
        }
    }

    public void addListener(LocalUpdateTaskListener listener) {
        if (listeners != null)
            listeners.add(listener);
    }

    public void removeListener(LocalUpdateTaskListener listener) {
        if (listener == null)
            listeners.clear();
        else
            listeners.remove(listener);
    }

    private boolean isLocalUpdateEnable() {
        if (listeners == null)
            return false;

        for (LocalUpdateTaskListener listener : listeners) {
            if(listener.isLocalUpdateEnable(this))
                return true;
        }
        return false;
    }

    private void notifyEnterLocalUpdate() {
        if (listeners == null)
            return;

        for (LocalUpdateTaskListener listener : listeners) {
            listener.enterLocalUpdate(this);
        }
    }

    private void notifyFindNewerVersion() {
        if (listeners == null)
            return;

        for (LocalUpdateTaskListener listener : listeners) {
            listener.findNewerVersion(this);
        }
    }

    private void notifyExitWithoutUpdate() {
        if (listeners == null)
            return;

        for (LocalUpdateTaskListener listener : listeners) {
            listener.exitWithoutUpdate(this);
        }
    }

    private void showDir(String path) {
        Log.d(TAG, "show dir: "+path);
        File dir = new File(path);
        if (!dir.exists()) {
            Log.d(TAG, path+" is not exist");
            return;
        }

        Collection<File> listFiles = FileUtils.listFiles(dir, FileFilterUtils.sizeFileFilter(0),null);
        for (File file:listFiles) {
            Log.d(TAG, file.getName());
        }
        Log.d(TAG, "show dir: "+path+" over");
    }

    public void downloadVersion() {
        Log.d(TAG, "enter downloadVersion");

        localStatus = LocalUpdateTask.LocalStatus.DOWNLOADVERSION;

        try {
            Log.d(TAG, "del "+ localTmpPath);
            File tmpDir = new File(localTmpPath);
            FileUtils.deleteDirectory(tmpDir);
            Log.d(TAG, "del "+ localTmpPath+" ok");
            // test
        } catch (IOException e) {
            e.printStackTrace();
        }

        LocalUpdateTask.DownloadFileTask downloadTask = new LocalUpdateTask.DownloadFileTask(
                LocalUpdateTask.DownloadType.VERSION, versionUrl, localVersionPathTmp);
        downloadTask.execute(10);
        Log.d(TAG, "exit downloadVersion");
    }

    public void downloadPackage() {
        if (tmpPackage == null)
            return;
        if (!tmpPackage.isInited())
            return;
        Log.d(TAG, "enter downloadPackage");
        localPackagePathTmp = localTmpPath + tmpPackage.getFilePath();

        String url = urlBase + tmpPackage.getFilePath();

        localStatus = LocalUpdateTask.LocalStatus.DOWNLOADPACKKAGE;
        LocalUpdateTask.DownloadFileTask downloadTask = new LocalUpdateTask.DownloadFileTask(
                LocalUpdateTask.DownloadType.UPGPACKAGE, url, localPackagePathTmp);
        downloadTask.execute(10);
        Log.d(TAG, "exit downloadPackage");
    }

    private UpgPackage getTmpVersion() {
        UpgPackage tmpPack = null;
        boolean needUpdated = false;
        int ret;

        Log.d(TAG, "enter getTmpVersion");

        tmpPack = new UpgPackage(localTmpPath);
        if (!tmpPack.isInited()) {
            Log.d(TAG, "version file not exist");
            return null;
        }

        if (!tmpPack.checkModelname("WIFISPEAKER"))
            return null;

        if (!tmpPack.checkBoardname("M3627_DEMO"))
            return null;

        if (!upgPackage.isInited()) {
            needUpdated = true;
        } else {
            ret = upgPackage.compareVersion(tmpPack.getVersion());
            if (ret < 0) {
                needUpdated = true;
            }
        }

        if (needUpdated) {
            Log.d(TAG, "need Update version");
            return tmpPack;
        }

        Log.d(TAG, "need not Update version");
        return null;
    }

    private void updatePackage() {
        Log.d(TAG, "enter updatePackage");
        localStatus = LocalUpdateTask.LocalStatus.UPDATING;

        try {
            File dir = new File(localPath);
            File tmpDir = new File(localTmpPath);
            Log.d(TAG, "clean "+localPath);
            FileUtils.deleteDirectory(dir);
            FileUtils.moveDirectory(tmpDir, dir);
            Log.d(TAG, "move "+localTmpPath+" to "+localPath);
            FileUtils.deleteDirectory(tmpDir);
            Log.d(TAG, "del "+localTmpPath);
            showDir(localTmpPath);
            showDir(localPath);
            localStatus = LocalStatus.UPDATESUCCESS;
			upgPackage = tmpPackage;
			tmpPackage = null;
            notifyFindNewerVersion();
            Log.d(TAG, "isLocalUpdated = true");
        } catch (IOException e) {
            Log.d(TAG, "updatePackage IOException");
            e.printStackTrace();
        }

        Log.d(TAG, "exit updatePackage");
    }

    private void onPostDownload(LocalUpdateTask.DownloadType type, int result) {
        Log.d(TAG, "onPostDownload typ:"+ type +"  result:"+result);
        switch (type) {
            case VERSION:
                if (result == 0) {
                    tmpPackage = getTmpVersion();
                    downloadPackage();
                }
                if (localStatus != LocalStatus.DOWNLOADPACKKAGE)
                    notifyExitWithoutUpdate();
                break;
            case UPGPACKAGE:
                if (result == 0) {
                    updatePackage();
                }

                if (localStatus != LocalStatus.UPDATESUCCESS) {
                    notifyExitWithoutUpdate();
                }
                break;
            default:
                break;
        }

    }

    private class DownloadFileTask extends AsyncTask<Integer, Integer, Integer> {
        private LocalUpdateTask.DownloadType downType;
        private String urlStr = null;
        private String filePath = null;
        public DownloadFileTask(LocalUpdateTask.DownloadType type, String url, String path) {
            downType = type;
            urlStr = url;
            filePath = path;
            Log.d(TAG, "DownloadFileTask filePath:"+ filePath);
        }
        @Override
        protected void onPreExecute(){

        }

        private class TrustAnyHostnameVerifier implements HostnameVerifier {

            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        }
        @Override
        protected Integer doInBackground(Integer... params){
            URL url = null;
            File file = null;
            int ret;
            try{
                Log.d(TAG, "doInBackground:"+urlStr);
                url = new URL(urlStr);
                //HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();

                TrustManager[] trustManagers = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                            }

                            @Override
                            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                            }

                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }
                        }
                };


                SSLContext ctx = SSLContext.getInstance("TLS");
                ctx.init(null, trustManagers, null);
                conn.setHostnameVerifier(new TrustAnyHostnameVerifier());
                conn.setSSLSocketFactory(ctx.getSocketFactory());
                conn.setRequestProperty("Accept-Encoding", "identity");
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.connect();

                //if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                    int totalLength = conn.getContentLength();
                    Log.d(TAG, "getContentLength:"+totalLength);

                    BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
                    file = new File(filePath);
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    Log.d(TAG, "file:"+file);
                    FileOutputStream out = new FileOutputStream(file);
                    int size = 0;
                    int len = 0;
                    byte[] buffer = new byte[1024];
                    while ((size = in.read(buffer)) != -1) {
                        len += size;
                        String sss = new String(buffer);
                        //Log.d(TAG, "read:"+size+"  "+sss);
                        out.write(buffer, 0, size);
                    }
                    in.close();
                    out.close();

                    return 0;
                } else
                    return -1;


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            }

            return -1;
        }

        @Override
        protected void onPostExecute(Integer result){
            onPostDownload(downType, result);
        }

    }

}
