package aca.com.remote.upgrade;

/**
 * Created by Gavin.Liu on 2018/1/18.
 */

public interface LocalUpdateTaskListener {
    boolean isLocalUpdateEnable(LocalUpdateTask localUpdateTask);
    void enterLocalUpdate(LocalUpdateTask localUpdateTask);
    void findNewerVersion(LocalUpdateTask localUpdateTask);
    void exitWithoutUpdate(LocalUpdateTask localUpdateTask);
}
