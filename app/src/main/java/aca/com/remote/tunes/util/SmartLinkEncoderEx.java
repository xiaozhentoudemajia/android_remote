package aca.com.remote.tunes.util;

import android.util.Log;

import java.util.Arrays;
import java.util.Random;

public class SmartLinkEncoderEx {
    private int mPkgLength = 0;
    private String[] mPkg;

    // Random char should be in range [0, 127).
    private char mRandomChar = (char)(new Random().nextInt(0x7F));

    public SmartLinkEncoderEx(String ssid, String password, int ip) {
        generalPkg(ssid, password, ip);
    }

    public String[] getPkg() {
        Log.i("wwj", "getPkg: "+mPkg.length);
        return mPkg;
    }

    private void addPayload(String[] data, int start, String payload) {
        int end = start + payload.length()+1;
        int cnt=0;
        String bEvenMask;
        int bAsc;
        char[] chars=payload.toCharArray();

        for(int i=start; i<end; i++) {
            bEvenMask = (i%2 == 0)?"170":"85";

            if (i == end-1){
                bAsc = 0;
            } else {
                bAsc = (int)(chars[cnt++]);
            }

            data[i] = "239" +"." + i + "." +bEvenMask+"."+bAsc;
            Log.i("wwj", "data "+i+": "+data[i]);
        }
    }

    private void addIp(String[] data, int start, int ip) {
        int end = start + 4;
        int cnt=0;
        String bEvenMask;
        String bDataMask;

        for(int i=start; i<end; i++) {
            bEvenMask = (i%2 == 0)?"170":"85";
            bDataMask = String.valueOf((ip >> 8*cnt++) & 0xff);

            data[i] = "239" +"." + i + "." +bEvenMask+"."+bDataMask;
            Log.i("wwj", "data "+i+": "+data[i]);
        }
    }

    private String intToIpAddr(int ip) {
        return (ip & 0xff) + "." + ((ip>>8)&0xff) + "." + ((ip>>16)&0xff) + "." + ((ip>>24)&0xff);
    }

    private void generalPkg(String ssid, String password, int ip) {
        mPkgLength = 2 + ssid.length() + 1 + password.length() + 1 + 4 + 1;
        Log.i("wwj", "ssid len:"+ssid.length() + " password len:"+password.length()+" mTotalLength:"+mPkgLength);
        Log.i("wwj", "ip: "+intToIpAddr(ip));

        int length = mPkgLength;
        mPkg = new String[length];
        String bEvenMask = "mask";
        String bDataMask = "mask";

        for(int i=0; i<length; i++) {
            bEvenMask = (i%2 == 0)?"170":"85";

            if (i == 0) {
                bDataMask = String.valueOf(mPkgLength);
            } else if (i == 1) {
                bDataMask = String.valueOf(mPkgLength-3);
            } else if (i == 2) {
                addPayload(mPkg, i, password);
                i += password.length()+1;

                addPayload(mPkg, i, ssid);
                i += ssid.length()+1;

                addIp(mPkg, i, ip);
                i += 4;

                bDataMask = String.valueOf(33);
                mPkg[i] = "239" +"." + i + "." +bEvenMask+"."+bDataMask;
                Log.i("wwj", "data "+i+": "+mPkg[i]);

                continue;
            }

            mPkg[i] = "239" +"." + i + "." +bEvenMask+"."+bDataMask;
            Log.i("wwj", "data "+i+": "+mPkg[i]);
        }
    }
}

