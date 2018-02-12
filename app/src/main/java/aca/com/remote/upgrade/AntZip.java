package aca.com.remote.upgrade;

import org.apache.tools.zip.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

/**
 * Created by Gavin.Liu on 2018/1/18.
 */

public class AntZip {
    private ZipFile zipFile;
    //private ZipOutputStream zipOut;
    //private ZipEntry zipEntry;
    private static int bufSize;
    private byte[] buf;
    private int readedBytes;

    public AntZip() {
        this(512);
    }

    public AntZip(int bufSize) {
        this.bufSize = bufSize;
        this.buf = new byte[this.bufSize];
    }

    public void unZip(String zipfileNmae) {
        FileOutputStream fileOut;
        File file;
        InputStream inputStream;
        String basePath;

        try {
            zipFile = new ZipFile(zipfileNmae);
            basePath = zipfileNmae.substring(0,zipfileNmae.lastIndexOf(File.separatorChar)+1);
            Enumeration entries = zipFile.getEntries();
            ZipEntry entry = null;

            while (entries.hasMoreElements()) {
                entry = (ZipEntry) entries.nextElement();
                String entryName = entry.getName();
                //String path = extPlace + File.separatorChar + entryName;
                //String names[] = entryName.split(""+File.separatorChar);

                file = new File(basePath+entry.getName());

                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    File parent = file.getParentFile();
                    if (!parent.exists()) {
                        parent.mkdirs();
                    }

                    inputStream = zipFile.getInputStream(entry);
                    fileOut = new FileOutputStream(file);
                    while ((this.readedBytes = inputStream.read(this.buf))>0) {
                        fileOut.write(this.buf, 0, this.readedBytes);
                    }
                    fileOut.close();

                    inputStream.close();
                }
            }
            this.zipFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        buf = null;
    }
}
