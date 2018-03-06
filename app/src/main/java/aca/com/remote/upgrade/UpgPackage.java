package aca.com.remote.upgrade;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.tools.zip.*;

/**
 * Created by gavin.liu on 2018/1/17.
 */

public class UpgPackage {
    private final String TAG = "UpgPackage";
    private String localDir;
    private String modelname;
    private String boardname;
    private String version;
    private String filePath;
    private String versionFile = "version";
    private String unZipPack;
    private List<PartitionInfo> upglists;
    private List<PartitionInfo> nor_parts;
    private PartitionInfo part;

    public UpgPackage(String dir){
        this.localDir = dir;
        this.modelname = null;
        this.boardname = null;
        this.version = null;
        this.filePath = null;

        this.upglists = new ArrayList<>();
        this.nor_parts = new ArrayList<>();
        initVersionInfo();
    }

    public int initVersionInfo() {
        try {
            String verPath = localDir + versionFile;
            File verFile = new File(verPath);
            if (!verFile.exists()) {
                Log.d(TAG, "verFile:"+verPath+" not exist");
                return -1;
            }

            FileInputStream fis = new FileInputStream(verFile);
            Properties pps = new Properties();
            pps.load(fis);
            modelname = pps.getProperty("modelname");
            boardname = pps.getProperty("boardname");
            version = pps.getProperty("version");
            filePath = pps.getProperty("file");

            Log.d(TAG, "version:"+version);
            Log.d(TAG, "filePath:"+filePath);

            fis.close();
            return 0;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public boolean isInited() {
        if (filePath != null)
            return true;
        return false;
    }

    public boolean checkModelname(String name) {
		if (name == null || modelname == null)
			return false;

        return modelname.equals(name);
    }

    public boolean checkBoardname(String name) {
		if (name == null || boardname == null)
			return false;

        return boardname.equals(name);
    }

    public int compareVersion(String ver) {
        int i;
        int ret;

        String verStr1 = this.version;
        String verStr2 = ver;

        Log.d(TAG, "compareVersion "+verStr1+" : "+verStr2);

	if (verStr1==null || verStr1.isEmpty())
		return -1;
	if (verStr2==null || verStr2.isEmpty())
		return -1;

        String[] verParts1 = verStr1.split("_");
        String[] verParts2 = verStr2.split("_");

        String[] vers1 = verParts1[verParts1.length-1].split("\\.");
        String[] vers2 = verParts2[verParts2.length-1].split("\\.");

        for (i=0; i<vers1.length || i<vers2.length; i++) {
            if (vers1[i] == null || vers1[i].isEmpty())
                return -1;
            if (vers2[i] == null || vers2[i].isEmpty())
                return 1;

            ret = vers1[i].compareTo(vers2[i]);
            if (ret != 0)
                return ret;
        }
        return 0;
    }

    public String getModelname() {
        return modelname;
    }

    public String getBoardname() {
        return boardname;
    }

    public String getVersion() {
        return version;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getUnzipDir() {
        if (unZipPack == null)
            return null;

        File zipOut = new File(unZipPack);
        if (zipOut.isDirectory())
            return (unZipPack+File.separatorChar);

        return null;
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
            Log.d(TAG, "|- "+file.getName());
        }
        Log.d(TAG, "show dir: "+path+" over");
    }

    private void unzipPackage() {
        try {
            if (getUnzipDir() != null)
                return;

            String zipFilePath = localDir + filePath;
            AntZip antZip = new AntZip();
            antZip.unZip(zipFilePath);

            unZipPack = zipFilePath.substring(0,zipFilePath.lastIndexOf('.'));
            String tempPath = localDir + "upgrade";

            File zipOut = new File(unZipPack);
            File zipTemp = new File(tempPath);
            FileUtils.deleteDirectory(zipOut);
            FileUtils.moveDirectory(zipTemp, zipOut);

            antZip.close();

            showDir(localDir);
            showDir(unZipPack);
        } catch (IOException e) {
            Log.d(TAG, "updatePackage IOException");
            e.printStackTrace();
        }
    }

    public void parseScript() {
        try {
            unzipPackage();

            if (upglists.size() > 0)
                return;

            String dir = getUnzipDir();
            if (dir == null)
                return;

            String scriptPath = dir + "upgrade.script";
            File script = new File(scriptPath);
            if (!script.exists()) {
                Log.d(TAG, "script:"+script+" not exist");
                return;
            }

            INIUtils iniUtils = new INIUtils(scriptPath);
            String section;
            String name;
            String offset;
            String size;
            String file;
            String md5;

            String countStr = iniUtils.getValue("UPGPART_COUNT", "count");
            int count = Integer.valueOf(countStr);

            for (int i=1; i<=count; i++) {
                section = "UPGPART"+i;
                name = iniUtils.getValue(section, "name");
                offset = iniUtils.getValue(section, "offset");
                size = iniUtils.getValue(section, "size");
                file = iniUtils.getValue(section, "file");
                md5 = iniUtils.getValue(section, "md5");
                part = new PartitionInfo(name, Integer.valueOf(offset), Integer.valueOf(size));
                part.setFilePath(file);
                part.setFileMd5(md5);
                upglists.add(part);
            }

            countStr = iniUtils.getValue("NOR_PARTITION_COUNT", "count");
            count = Integer.valueOf(countStr);

            for (int i=0; i<count; i++) {
                section = "NOR_PARTITION"+i;
                name = iniUtils.getValue(section, "name");
                offset = iniUtils.getValue(section, "offset");
                size = iniUtils.getValue(section, "size");
                part = new PartitionInfo(name, Integer.valueOf(offset), Integer.valueOf(size));
                nor_parts.add(part);
            }


        } catch (IOException e) {
            Log.d(TAG, "updatePackage IOException");
            e.printStackTrace();
        }

        return;
    }

    public List<PartitionInfo> getUpglists() {
        return upglists;
    }

    public List<PartitionInfo> getNorParts() {
        return nor_parts;
    }
}
