package aca.com.remote.tunes.daap;

/**
 * Created by gavin.liu on 2017/10/11.
 */
public class WifiAp {
    private String ssid;
    private String mac;
    private int channel;

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

}
