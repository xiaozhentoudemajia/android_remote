package aca.com.remote.upgrade;

/**
 * Created by Gavin.Liu on 2018/1/22.
 */

public interface UpgNotify {
    public void notifyLocalversion(String version);
    public void notifyFirmwareversion(String version);
    public void notifySatus(String info);
    public void notifySetCountStatus(int result);
    public void notifySetUpgPartStatus(int index, int result);
    public void notifyBurnStatus(int result);
}
