package aca.com.remote.upgrade;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

/**
 * Created by Gavin.Liu on 2018/1/23.
 */

public class INIUtils {
    private final String TAG = "INIUtils";
    protected HashMap sections = new HashMap();
    private transient String currentSecion;
    private transient Properties current;

    public INIUtils(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        read(reader);
        reader.close();
    }

    protected void read(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            parseLine(line);
        }
    }

    private void parseLine(String line) {
        line = line.trim();
        if (line.matches("^\\[\\S+]$")) {
            currentSecion = line.replaceFirst("^\\[(\\S+)\\]$", "$1");
            current = new Properties();
            //Log.d(TAG, "add section " + currentSecion +"  properties:"+current);
            sections.put(currentSecion, current);
        } else if (line.matches(".*=.*")) {
            if (current != null) {
                int i = line.indexOf('=');
                String name = line.substring(0, i).trim();
                String value = line.substring(i + 1).trim();
                Log.d(TAG, "  add propertie " + name+" = "+value);
                current.setProperty(name, value);
            }
        }
    }

     public String getValue(String section, String name) {
         Log.d(TAG, "getValue section " + section+" "+name);
         Properties p = (Properties) sections.get(section);
         if (p == null) {
             return null;
         }

         String value = p.getProperty(name);
         Log.d(TAG, "value: " + value);
         return value;
     }
}
