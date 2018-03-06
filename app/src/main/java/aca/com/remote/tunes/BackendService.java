/*
    TunesRemote+ - http://code.google.com/p/tunesremote-plus/
    
    Copyright (C) 2008 Jeffrey Sharkey, http://jsharkey.org/
    Copyright (C) 2010 TunesRemote+, http://code.google.com/p/tunesremote-plus/
    
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
    
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    
    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    
    The Initial Developer of the Original Code is Jeffrey Sharkey.
    Portions created by Jeffrey Sharkey are
    Copyright (C) 2008. Jeffrey Sharkey, http://jsharkey.org/
    All Rights Reserved.
 */

package aca.com.remote.tunes;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import aca.com.remote.tunes.jmdns.JmDNS;
import aca.com.remote.tunes.jmdns.ServiceEvent;
import aca.com.remote.tunes.jmdns.ServiceInfo;
import aca.com.remote.tunes.jmdns.ServiceListener;
import aca.com.remote.tunes.daap.Session;
import aca.com.remote.tunes.util.PairingDatabase;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BackendService extends Service implements ServiceListener {

   public final static String TAG = BackendService.class.toString();
   public final static String PREF_LASTADDR = "lastaddress";
    public final static String PREF_LASTPLAYINTERNAL = "lastplayinternal";
    public final static String PREF_LASTSEARCHINTERNAL = "lastsearchinternal";
    public final static String PREF_CURHOST = "curhost";
    public final static String PREF_CURLIBRARY = "curlibrary";
    public final static String PREF_CURSERVICE = "curservice";

   public final static int MAX_SESSION_HOLD = 10;
   public final static int DELAY = 500;
   protected final  static int PROBE_STOP         = 0x80;
   protected final  static int PROBE_START        = 0x81;
   protected final  static int PROBE_FORCE_START = 0x82;
   protected final  static int PROBE_ADD_UPDATE  = 0x83;
   protected final  static int PROBE_RM_UPDATE   = 0x84;

    public final static String TOUCH_ABLE_TYPE = "_touch-able._tcp.local.";
    public final static String DACP_TYPE = "_dacp._tcp.local.";
    public final static String REMOTE_TYPE = "_touch-remote._tcp.local.";
    public final static String HOSTNAME = "tunesremote";
    public final static int NOTIFY_POBR_START  = 0x10;
    public final static int NOTIFY_POBR_CACHE_LIBRARY  = 0x11;
    public final static int NOTIFY_POBR_ADD_LIBRARY  = 0x12;
    public final static int NOTIFY_POBR_RM_LIBRARY  = 0x13;

   // this service keeps a single session active that others can attach to
   // also handles incoming control information from libraryactivity

   protected Session session = null;
   protected String lastaddress = null;
    protected String lastPlayInternal = null;
    protected String lastSearchInternal = null;
   protected static SharedPreferences prefs;
   public PairingDatabase pairdb;
    protected String curHost;
    protected String curHostLibrary;
    protected String curHostServices;
   private HandlerThread mProbeThead;
   protected Handler mProbeHandle;
   private static JmDNS zeroConf = null;
   private static WifiManager.MulticastLock mLock = null;
   ProbeListener probeListener;
   private final IBinder binder = new BackendBinder();
   private static HashMap<String,SessionWrapper> sessionHashMap = new HashMap<String, SessionWrapper>();
   private static HashMap<String,ServiceInfo> serviceInfoHashMap = new HashMap<String, ServiceInfo>();
   public static JmDNS getZeroConf() {
      return zeroConf;
   }

   public Session getSession() {
      // make sure we have an active session
      // FIRST, try and create the session from the last known connection
      //synchronized (this) {
         if (session == null) {
            // try finding last library opened by user
            this.lastaddress = prefs.getString(PREF_LASTADDR, null);

            if (this.lastaddress != null) {
               try {
                  Log.d(TAG, String.format("tried looking for lastaddr=%s", lastaddress));
                  this.setLibrary(this.lastaddress, null, null);
               } catch (Exception e) {
                  Log.w(TAG, "getSession:" + e.getMessage());
               }
            }

            // SECOND, if session is still NULL try and loop through all known
            // servers stopping at the first one connected
            if (session == null) {
               Cursor cursor = pairdb.fetchAllServers();
               try {
                  cursor.moveToFirst();
                  while (cursor.isAfterLast() == false) {
                     try {
                        final String address = cursor.getString(cursor
                                .getColumnIndexOrThrow(PairingDatabase.FIELD_PAIR_ADDRESS));
                        final String library = cursor.getString(cursor
                                .getColumnIndexOrThrow(PairingDatabase.FIELD_PAIR_LIBRARY));
                        final String code = cursor
                                .getString(cursor.getColumnIndexOrThrow(PairingDatabase.FIELD_PAIR_GUID));
                        this.setLibrary(address, library, code);
                        break;
                     } catch (Exception e) {
                        Log.w(TAG, "getSession Failed trying next server:" + e.getMessage());
                     }
                     cursor.moveToNext();
                  }
               } finally {
                  cursor.close();
               }
            }
         }else{
            if(sessionHashMap.containsKey(session.getHost())){
               return sessionHashMap.get(session.getHost()).getSession();
            }
         }
      //}
      return session;

   }

//   public Session getSession(String host) {
//       SessionWrapper mySession;
//        if(sessionHashMap.containsKey(host)){
//            mySession = sessionHashMap.get(host);
//            if(mySession.isTimeout()){
//
//            }
//            return sessionHashMap.get(host).getSession();
//        }
//       return null;
//    }

    public void clearSession() {
        this.session =null;
        if (prefs != null) {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(BackendService.PREF_LASTADDR, null);
            edit.commit();
        }

        pairdb.deleteAll();
        curHost = null;
        curHostLibrary = null;
        curHostServices = null;
    }

    public void updateCurSession(Session session){
        this.session =session;
    }

    public SessionWrapper getSession(String host) {
        if(sessionHashMap.containsKey(host)){
            return sessionHashMap.get(host);
        }
        return null;
    }

    public Session getSession(String host, String library) {
        if(sessionHashMap.containsKey(host)){
            return sessionHashMap.get(host).getSession();
        }

        try {
            Log.d(TAG, String.format("created session for host(%s)=%s",library, host));
            this.setLibrary(host, library, null);
        } catch (Exception e) {
            Log.w(TAG, "getSession:" + e.getMessage());
        }

        if(sessionHashMap.containsKey(host)){
            return sessionHashMap.get(host).getSession();
        }
        //prefs

        if (!sessionHashMap.containsKey(host)) {
            Cursor cursor = pairdb.fetchAllServers();
            try {
                cursor.moveToFirst();
                while (cursor.isAfterLast() == false) {
                    try {
                        final String address = cursor.getString(cursor
                                .getColumnIndexOrThrow(PairingDatabase.FIELD_PAIR_ADDRESS));
                        final String library2 = cursor.getString(cursor
                                .getColumnIndexOrThrow(PairingDatabase.FIELD_PAIR_LIBRARY));
                        final String code = cursor
                                .getString(cursor.getColumnIndexOrThrow(PairingDatabase.FIELD_PAIR_GUID));
                        this.setLibrary(address, library2, code);
                        break;
                    } catch (Exception e) {
                        Log.w(TAG, "getSession Failed trying next server:" + e.getMessage());
                    }
                    cursor.moveToNext();
                }
            } finally {
                cursor.close();
            }
        }
        if(sessionHashMap.containsKey(host)){
            return sessionHashMap.get(host).getSession();
        }
        return null;
    }

  public void setLibrary(String address, String library, String code) throws Exception {

      Session sessionTmp;
      SessionWrapper mySession;
      // try looking up code in database if null
      if (code == null) {
         if (library != null) {
            code = pairdb.findCodeLibrary(library);
         } else if (address != null) {
            code = pairdb.findCodeAddress(address);
         }
      }
      if(sessionHashMap.containsKey(address)){
         mySession = sessionHashMap.get(address);
         mySession.writeLock();
         if(mySession.isTimeout()) {
            Log.i(TAG, "session tiemout,recreate :"+address);
             try {
                 sessionTmp = new Session(address, code);
                 mySession.setSession(sessionTmp);
                 this.session = mySession.getSessionUnlock();
             }catch (Exception e){
                e.printStackTrace();
                 sessionHashMap.remove(address);
                 mySession.writeUnlock();
                 throw  e;
             }
         }

         mySession.writeUnlock();
         return;
      }

      if(sessionHashMap.size() > MAX_SESSION_HOLD){
         SessionWrapper sessionRm = null;
         long mixT = System.currentTimeMillis();
         Iterator iter = sessionHashMap.entrySet().iterator();
         while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            SessionWrapper val = (SessionWrapper) entry.getValue();
            if(val.lastTime < mixT){
               mixT = val.lastTime;
               sessionRm = val;
            }
         }
         sessionHashMap.remove(sessionRm.host);
         Log.i(TAG, "discard session of "+sessionRm.host);
      }
      Log.d(TAG, String.format("Session with address=%s, library=%s, code=%s", address, library, code));
      mySession = new SessionWrapper(address);
      mySession.writeLock();
      try {
          mySession.setSession(new Session(address, code));
          this.session = mySession.getSessionUnlock();
        }catch (Exception e){
          e.printStackTrace();
          mySession.writeUnlock();
          throw e;
      }
      mySession.writeUnlock();
      sessionHashMap.put(address,mySession);
      // if we made it past this point, then we logged in successfully yay
      Log.d(TAG, "yay found session!  were gonna update our db a new code maybe?");

      // if we have a library, we should make sure that its stored in our db
      // create a new entry, otherwise just update the ip address
      if (library != null) {
         if (!pairdb.libraryExists(library)) {
            pairdb.insertCode(address, library, code);
         } else {
            pairdb.updateAddressAndCode(library, address, code);
         }
      }

      // save this ip address to help us start faster
      if (prefs != null) {
         Editor edit = prefs.edit();
         edit.putString(PREF_LASTADDR, address);
         edit.commit();
      }
   }

   public void startProbe(boolean force){
      if(force){
          Log.i(TAG, " PROBE_FORCE_START ");
         mProbeHandle.sendEmptyMessageDelayed(PROBE_FORCE_START,DELAY);
      }else {
          Log.i(TAG, " PROBE_START ");
         mProbeHandle.sendEmptyMessageDelayed(PROBE_START,DELAY);
      }
   }

   public void stopProbe(){
      mProbeHandle.sendEmptyMessage(PROBE_STOP);
   }

   public void registerProbeListener(ProbeListener listener)
   {
      probeListener = listener;
   }

   public void unregisterProbeListener(ProbeListener listener)
   {
      //if(probeListener == listener)
         probeListener = null;
   }

   @Override
   public void onCreate() {
      Log.d(TAG, "starting backend service");
      this.pairdb = new PairingDatabase(this);
      mProbeThead = new HandlerThread("discovery");
      mProbeThead.start();
      mProbeHandle = new Handler(mProbeThead.getLooper())
      {
         @Override
         public void handleMessage(Message msg) {
            switch (msg.what){

               case PROBE_START:
                   Log.i(TAG,"msg PROBE_START...");
                  if((zeroConf != null)&&(serviceInfoHashMap.size() > 0)){
                      if(null != probeListener){
                          probeListener.onServiceinfoCache(serviceInfoHashMap);
                      }
                     break;
                  }
                   // no break
               case PROBE_FORCE_START:
                  Log.i(TAG,"msg PROBE_FORCE_START...");
                  try {
                      BackendService.this.startProbeInternal();
                      serviceInfoHashMap.clear();
                  } catch (Exception e) {
                     Log.d(TAG, String.format("startProbe Error: %s", e.getMessage()));
                     e.printStackTrace();
                  }
                  Log.i(TAG,"msg PROBE_START process finish");
                  break;
               case PROBE_STOP:
                  Log.i(TAG,"msg PROBE_STOP...");
                  BackendService.this.stopProbeInternal();
                  Log.i(TAG,"msg PROBE_STOP process finish");
                  break;
               case PROBE_ADD_UPDATE:
                  BackendService.this.notifyFoundLibrary((String)msg.obj);
                  break;
               case PROBE_RM_UPDATE:
                  BackendService.this.notifyRemoveLibrary((String)msg.obj);
                  break;
            };
         }
      };
   }

   @Override
   public void onDestroy() {
      // close any dns services and current status threads
      // store information about last-connected library

      Log.d(TAG, "stopping backend service");

      this.pairdb.close();
      mProbeHandle.sendEmptyMessage(PROBE_STOP);
      mProbeThead.quit();
   }

   @Override
   public void serviceAdded(ServiceEvent event) {
      final String name = event.getName();
      Log.d(TAG, String.format("serviceAdded :%s", name));
//      mProbeHandle.sendMessageDelayed(Message.obtain(mProbeHandle,PROBE_ADD_UPDATE, name), DELAY);
      mProbeHandle.sendMessage(Message.obtain(mProbeHandle,PROBE_ADD_UPDATE, name));

   }

   @Override
   public void serviceRemoved(ServiceEvent event) {
      final String name = event.getName();
      Log.d(TAG, String.format("serviceRemoved :%s", name));
      mProbeHandle.sendMessage(Message.obtain(mProbeHandle,PROBE_ADD_UPDATE, name));
   }

   @Override
   public void serviceResolved(ServiceEvent event) {

   }

   public void notifyFoundLibrary(String library) {
      try {
         Log.i(TAG, String.format("DNS Name: %s", library));
         if(null == getZeroConf()) {
            Log.w(TAG, "getZeroConf is null");
            return ;
         }
         ServiceInfo serviceInfo = getZeroConf().getServiceInfo(TOUCH_ABLE_TYPE, library);
         // try and get the DACP type only if we cannot find any touchable
         if (serviceInfo == null) {
//            serviceInfo = getZeroConf().getServiceInfo(DACP_TYPE, library);
         }

         if (serviceInfo == null) {
            Log.w(TAG, "serviceInfo is null");
            return ; // nothing to add since serviceInfo is NULL
         }
//         String libraryName = serviceInfo.getPropertyString("CtlN");
//         if (libraryName == null) {
//            libraryName = serviceInfo.getName();
//         }
//         serviceInfoHashMap.put(libraryName,serviceInfo);
         serviceInfoHashMap.put(library,serviceInfo);
         if(null != probeListener){
            probeListener.onServiceinfoFound(serviceInfo);
         }

      } catch (Exception e) {
         Log.w(TAG, String.format("Problem getting ZeroConf information %s", e.getMessage()));
      }

      return ;
   }

   public void notifyRemoveLibrary(String library) {
      if(serviceInfoHashMap.containsKey(library)){
         if(null != probeListener){
            ServiceInfo serviceInfo = serviceInfoHashMap.get(library);
//            probeListener.
         }
         serviceInfoHashMap.remove(library);
      }

   }
   protected void checkWifiState() {
      if(null != probeListener)
         probeListener.OnCheckWifiState();
   }

   // this screen will run a network query of all libraries
   // upon selection it will try authenticating with that library, and launch
   // the pairing activity if failed
   protected void startProbeInternal() throws Exception {

      if (zeroConf != null)
         BackendService.this.stopProbeInternal();

      // figure out our wifi address, otherwise bail
      WifiManager wifi = (WifiManager) BackendService.this.getSystemService(Context.WIFI_SERVICE);

      WifiInfo wifiinfo = wifi.getConnectionInfo();
      int intaddr = wifiinfo.getIpAddress();

      if (intaddr != 0) { // Only worth doing if there's an actual wifi
         // connection

         byte[] byteaddr = new byte[] { (byte) (intaddr & 0xff), (byte) (intaddr >> 8 & 0xff),
                 (byte) (intaddr >> 16 & 0xff), (byte) (intaddr >> 24 & 0xff) };
         InetAddress addr = InetAddress.getByAddress(byteaddr);

         Log.i(TAG, String.format("found intaddr=%d, addr=%s", intaddr, addr.toString()));
         // start multicast lock
         mLock = wifi.createMulticastLock("TunesRemote lock");
         mLock.setReferenceCounted(true);
         mLock.acquire();
         Log.i(TAG,"startProbe");
         zeroConf = JmDNS.create(addr, HOSTNAME);
         zeroConf.addServiceListener(TOUCH_ABLE_TYPE,  BackendService.this);
         zeroConf.addServiceListener(DACP_TYPE,  BackendService.this);
/*
          if (prefs != null) {
              Editor edit = prefs.edit();
              edit.putString(PREF_LASTINTERNAL, wifiinfo.getSSID());
              edit.commit();
          }*/
      } else
         checkWifiState();

   }

   protected void stopProbeInternal() {
      if(null == zeroConf)
         return;
      zeroConf.removeServiceListener(TOUCH_ABLE_TYPE, BackendService.this);
      zeroConf.removeServiceListener(DACP_TYPE, BackendService.this);

      try {
         zeroConf.close();
         zeroConf = null;
      } catch (IOException e) {
         Log.d(TAG, String.format("ZeroConf Error: %s", e.getMessage()));
      }
      if(null != mLock)
         mLock.release();
      mLock = null;
   }

   public interface  ProbeListener{
       public boolean  onServiceinfoCache(HashMap<String, ServiceInfo> map);
       public boolean  onServiceinfoFound(ServiceInfo serviceInfo);
       public void OnCheckWifiState();
   }

   public class BackendBinder extends Binder {
      public BackendService getService() {
         return BackendService.this;
      }
   }

   @Override
   public IBinder onBind(Intent intent) {
      return binder;
   }

   /**
    * Gets the prefs.
    * <p>
    * @return Returns the prefs.
    */
   public SharedPreferences getPrefs() {
      return prefs;
   }

   /**
    * Sets the prefs.
    * <p>
    * @param prefs The prefs to set.
    */
   public void setPrefs(SharedPreferences prefs) {
      if (prefs == null) {
         return;
      }
      BackendService.prefs = prefs;
   }

    public String getCurHost(){
        if (prefs != null) {
            this.curHost = prefs.getString(PREF_CURHOST, null);
        }
        return this.curHost;
    }

    public void setCurHost(String host){
        this.curHost = host;
        if (prefs != null) {
            Editor edit = prefs.edit();
            edit.putString(PREF_CURHOST, host);
            edit.commit();
        }
    }

    public String getCurHostLibrary(){
        if (prefs != null) {
            this.curHostLibrary = prefs.getString(PREF_CURLIBRARY, null);
        }
        return this.curHostLibrary;
    }

    public void setCurHostLibrary(String library){
        this.curHostLibrary = library;
        if (prefs != null) {
            Editor edit = prefs.edit();
            edit.putString(PREF_CURLIBRARY, library);
            edit.commit();
        }
    }

    public String getCurHostServices(){
        if (prefs != null) {
            this.curHostServices = prefs.getString(PREF_CURSERVICE, null);
        }
        return this.curHostServices;
    }

    public void setCurHostServices(String services){
        this.curHostServices = services;
        if (prefs != null) {
            Editor edit = prefs.edit();
            edit.putString(PREF_CURSERVICE, services);
            edit.commit();
        }
    }

    public void setLastPlayInternal() {
        if (prefs != null) {
            WifiManager wifi = (WifiManager) BackendService.this.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiinfo = wifi.getConnectionInfo();

            Editor edit = prefs.edit();
            edit.putString(PREF_LASTPLAYINTERNAL, wifiinfo.getSSID());
            edit.commit();
        }
    }

    public String getLastPlayInternal() {
        if (prefs != null) {
            lastPlayInternal = prefs.getString(PREF_LASTPLAYINTERNAL, null);
        }

        return lastPlayInternal;
    }

    public boolean checkPlayInternal() {
        boolean ret = true;

        if (prefs != null) {
            WifiManager wifi = (WifiManager) BackendService.this.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiinfo = wifi.getConnectionInfo();

            Log.i(TAG, "getSSID() :"+wifiinfo.getSSID());
            lastPlayInternal = prefs.getString(PREF_LASTPLAYINTERNAL, null);
            Log.i(TAG, "lastPlayInternal :"+lastPlayInternal);

            if (!wifiinfo.getSSID().equals(lastPlayInternal))
            {
                Log.i(TAG, "different PlayInternal");
                ret = false;
            } else {
                Log.i(TAG, "same PlayInternal");
                ret = true;
            }
        }

        return ret;
    }

    public void setLastSearchInternal() {
        if (prefs != null) {
            WifiManager wifi = (WifiManager) BackendService.this.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiinfo = wifi.getConnectionInfo();

            Editor edit = prefs.edit();
            edit.putString(PREF_LASTSEARCHINTERNAL, wifiinfo.getSSID());
            edit.commit();
        }
    }

    public String getLastSearchInternal() {
        if (prefs != null) {
            lastSearchInternal = prefs.getString(PREF_LASTSEARCHINTERNAL, null);
        }

        return lastSearchInternal;
    }

    public boolean checkSearchInternal() {
        boolean ret = true;

        if (prefs != null) {
            WifiManager wifi = (WifiManager) BackendService.this.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiinfo = wifi.getConnectionInfo();

            Log.i(TAG, "getSSID() :"+wifiinfo.getSSID());
            lastSearchInternal = prefs.getString(PREF_LASTSEARCHINTERNAL, null);
            Log.i(TAG, "lastSearchInternal :"+lastSearchInternal);

            if (!wifiinfo.getSSID().equals(lastSearchInternal))
            {
                Log.i(TAG, "different SearchInternal");
                ret = false;
            } else {
                Log.i(TAG, "same SearchInternal");
                ret = true;
            }
        }

        return ret;
    }
}
