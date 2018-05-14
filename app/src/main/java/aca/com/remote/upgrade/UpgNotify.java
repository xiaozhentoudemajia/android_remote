package aca.com.remote.upgrade;

/**
 * Created by Gavin.Liu on 2018/1/22.
 */

public interface UpgNotify {
    public void notifyCheckUpgResult(String result);
    public void notifyFirmwareversion(String version);
    public void notifySatus(String info);
    public void notifyDownloadSatus(int percent);
    public void notifyBurnStatus(int percent);
    public void notifyBurnRootfs();
}
