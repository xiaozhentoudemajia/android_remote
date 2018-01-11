package aca.com.remote.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import aca.com.remote.R;
import aca.com.remote.tunes.util.SmartLinkEncoderEx;
import aca.com.remote.uitl.CommonUtils;

/**
 * Created by gavin.liu on 2017/10/13.
 */
public class SmartLinkExActivity extends AppCompatActivity {
    private EditText mSSIDEditText;
    private EditText mPasswordEditText;
    private int mIp;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
        ActionBar mActionBar=getActionBar();
        mActionBar.setTitle(R.string.smartlink);
        mActionBar.setHomeButtonEnabled(true);
        mActionBar.setDisplayHomeAsUpEnabled(true);*/
        setContentView(R.layout.activity_smartlink);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        ((AppCompatActivity) this).setSupportActionBar(toolbar);
        toolbar.setPadding(0, CommonUtils.getStatusHeight(this), 0, 0);


        android.support.v7.app.ActionBar ab = ((AppCompatActivity) this).getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.actionbar_back);
        ab.setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mSSIDEditText = (EditText)findViewById(R.id.ssidEditText);
        mPasswordEditText = (EditText)findViewById(R.id.passwordEditText);

        Context context = getApplicationContext();
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            mIp = connectionInfo.getIpAddress();
            if (connectionInfo != null) {
                String ssid = connectionInfo.getSSID();
                if (Build.VERSION.SDK_INT >= 17 && ssid.startsWith("\"") && ssid.endsWith("\""))
                    ssid = ssid.replaceAll("^\"|\"$", "");
                mSSIDEditText.setText(ssid);
                mSSIDEditText.setEnabled(false);
            }
        }
    }

    public void onConnectBtnClick(View view) {
        String ssid = mSSIDEditText.getText().toString();
        String password = mPasswordEditText.getText().toString();
        if (ssid.isEmpty()) {
            Context context = getApplicationContext();
            CharSequence text = "Please input ssid and password.";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return;
        }

        new SmartLinkTask(this, new SmartLinkEncoderEx(ssid, password, mIp)).execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return false;
    }

    private class SmartLinkTask extends AsyncTask<Void, Void, Void> implements DialogInterface.OnDismissListener {
        private static final int PORT = 10000;
        private final byte DUMMY_DATA[] = new byte[1500];
        private static final int REPLY_BYTE_CONFIRM_TIMES = 5;

        private ProgressDialog mDialog;
        private Context mContext;
//        private DatagramSocket mSocket;
        private InetAddress group;
        private MulticastSocket mSocket;

        private char mRandomChar;
        private SmartLinkEncoderEx mSmartLinkEncoder;

        private volatile boolean mDone = false;

        public SmartLinkTask(Activity activity, SmartLinkEncoderEx encoder) {
            mContext = activity;
            mDialog = new ProgressDialog(mContext);
            mDialog.setOnDismissListener(this);
//            mRandomChar = encoder.getRandomChar();
            mRandomChar = 33;
            mSmartLinkEncoder = encoder;
        }

        @Override
        protected void onPreExecute() {
            this.mDialog.setMessage("Connecting :)");
            this.mDialog.show();

            new Thread(new Runnable() {
                public void run() {
                    byte[] buffer = new byte[15000];
                    try {
                        DatagramSocket udpServerSocket = new DatagramSocket(PORT);
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        int replyByteCounter = 0;
                        udpServerSocket.setSoTimeout(1000);
                        while (true) {
                            if (getStatus() == Status.FINISHED)
                                break;
                            try {
                                udpServerSocket.receive(packet);
                                byte receivedData[] = packet.getData();
                                for (byte b : receivedData) {
                                    if (b == mRandomChar)
                                        replyByteCounter++;
                                }
                                if (replyByteCounter > REPLY_BYTE_CONFIRM_TIMES) {
                                    mDone = true;
                                    break;
                                }
                            } catch (SocketTimeoutException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        udpServerSocket.close();
                    } catch (SocketException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        private void sendPacketAndSleep_bak(int length) {
            try {
                /*
                DatagramPacket pkg = new DatagramPacket(DUMMY_DATA,
                        length,
                        InetAddress.getByName("239.0.0.27"),
                        PORT);
                        */
                byte data[] = new byte[4];
                DatagramPacket pkg = new DatagramPacket(data,
                        4,
                        group,
                        30001);
                mSocket.send(pkg);
                Thread.sleep(40);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void sendPacketAndSleep(int length) {
            try {
                /*
                DatagramPacket pkg = new DatagramPacket(DUMMY_DATA,
                        length,
                        InetAddress.getByName("239.0.0.27"),
                        PORT);
                        */
                DatagramPacket pkg = new DatagramPacket(DUMMY_DATA,
                        length,
                        group,
                        30001);
                mSocket.send(pkg);
                Thread.sleep(4);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Void doInBackground(Void... params) {

            for (int d = 0; d < 300; ++d) {
                try {

//                mSocket = new DatagramSocket();
                    mSocket = new MulticastSocket(30001);
//                mSocket.setBroadcast(true);
                    group = InetAddress.getByName("239.0.0.0");
                    mSocket.joinGroup(group);
                    sendPacketAndSleep_bak(4);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                String pkg[] = mSmartLinkEncoder.getPkg();

                for (int i = 0; i < pkg.length; i++) {

                    try {

//                mSocket = new DatagramSocket();
                        Log.i("wwj", "sendPacketAndSleep_bak : " + pkg[i]);
                        mSocket = new MulticastSocket(30001);
//                mSocket.setBroadcast(true);
                        group = InetAddress.getByName(pkg[i]);
                        mSocket.joinGroup(group);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    sendPacketAndSleep_bak(4);
                }

                if (d % 200 == 0) {
                    if (isCancelled() || mDone)
                        return null;
                }
            }
            return null;
        }

        @Override
        protected void onCancelled(Void params) {
            Toast.makeText(getApplicationContext(), "Smart Link Cancelled.", Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onPostExecute(Void params) {
            if (mDialog.isShowing()) {
                mDialog.dismiss();
            }

            String result;
            if (mDone) {
                result = "Smart Link Successfully Done!";
            } else {
                result = "Smart Link Timeout.";
            }
            Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            if (mDone)
                return;

            this.cancel(true);
        }
    }

}
