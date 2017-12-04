package aca.com.remote.tunes.util;

/**
 * Created by gavin.liu on 2017/8/10.
 */
public class Constants {
    public final static String LIBRARYSERVICE = "Service";
    public final static String MUSICSERVICE = "MusicService";
    public final static String SPOTIFYSERVICE = "Spotify";
    public final static String SHOUTCASTSERVICE = "SHOUTcast";
    public final static String DLNASERVICE = "DLNA";
    public final static String MYMUSICSERVICE = "MyMusic";
    public final static String BLUETOOTHSERVICE = "Bluetooth";

    public final static String EXTRA_LIBRARY = "library" ;
    public final static String EXTRA_ADDRESS = "address";
    public final static String EXTRA_CODE = "code";

    public final static int MSG_WHAT_POBR_START  =           0;
    public final static int MSG_WHAT_POBR_CACHE_LIBRARY  =  1;
    public final static int MSG_WHAT_POBR_ADD_LIBRARY  =    2;
    public final static int MSG_WHAT_POBR_RM_LIBRARY  =     3;
    public final static int MSG_WHAT_SESSION_READY = 0x10;

}
