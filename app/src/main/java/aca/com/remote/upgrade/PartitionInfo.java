package aca.com.remote.upgrade;

import android.util.Log;

/**
 * Created by Gavin.Liu on 2018/1/19.
 */

public class PartitionInfo {
    private final String TAG = "PartitionInfo";
    private String name;
    private long offset;
    private long size;
    private String filePath;
    private String md5;

    public PartitionInfo(String name, long offset, long size) {
        this.name = name;
        this.offset = offset;
        this.size = size;
        this.filePath = null;
        this.md5 = null;
        Log.d(TAG, "newpart name:"+name+"  offset:"+offset+"   size:"+size);
    }

    public void setFilePath(String path) {
        filePath = path;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFileMd5(String md5sum) {
        md5 = md5sum;
    }

    public String getFileMd5() {
        return md5;
    }

    public String getName() {
        return name;
    }

    public long getOffset() {
        return offset;
    }

    public long getSize() {
        return size;
    }
}
