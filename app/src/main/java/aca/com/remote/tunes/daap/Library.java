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

package aca.com.remote.tunes.daap;

import android.util.Log;

import aca.com.remote.tunes.PlaylistListener;
import aca.com.remote.tunes.TagListener;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Pattern;

public class Library {

   public final static String TAG = Library.class.toString();
   public final static int RESULT_INCREMENT = 50;
   public final static Pattern MLIT_PATTERN = Pattern.compile("mlit");

   // library keeps track of albums/tracks from itunes also caches requests as
   // needed
   protected final Session session;
   private ActionErrorListener errorListener;

   public Library(Session session) {
      this.session = session;
   }

   public Library(Session session,ActionErrorListener errorListener) {
      this.session = session;
      this.errorListener = errorListener;
   }

   /**
    * Performs a search of the DACP Server sending it search criteria and an
    * index of how many items to find.
    * <p>
    * @param listener the TagListener to capture records coming in for the UI
    * @param search the search criteria
    * @param start items to start with for paging (usually 0)
    * @param items the total items to return in this search
    * @return the count of records returned or -1 if nothing found
    */
   public long readSearch(TagListener listener, String search, long start, long items) {
      long total = -1;
      try {
         String encodedSearch = Library.escapeUrlString(search);
         String query = String
                  .format("%s/databases/%d/containers/%d/items?session-id=%s&meta=dmap.itemname,dmap.itemid,dmap.persistentid,daap.songartist,daap.songalbum,daap.songtime,daap.songuserrating,daap.songtracknumber&type=music&sort=name&include-sort-headers=1&query=(('com.apple.itunes.mediakind:1','com.apple.itunes.mediakind:4','com.apple.itunes.mediakind:8')+('dmap.itemname:*%s*','daap.songartist:*%s*','daap.songalbum:*%s*'))&index=%d-%d",
                           session.getRequestBase(), session.databaseId, session.libraryId, session.sessionId,
                           encodedSearch, encodedSearch, encodedSearch, start, items);
         byte[] raw = RequestHelper.request(query, false);
         Response resp = ResponseParser.performParse(raw, listener, MLIT_PATTERN);
         // apso or adbs
         Response nested = resp.getNested("apso");
         if (nested == null)
            nested = resp.getNested("adbs");
         if (nested != null)
            total = nested.getNumberLong("mtco");
      } catch (Exception e) {
          String errStr = e.getMessage();
          if((null != errStr) && errStr.contains("HTTP Error Response Code")){
              if(null != errorListener){
                  String[] splits = errStr.split(":");
                  errorListener.onActionError(Integer.parseInt(splits[1].trim()));
              }
          }
         Log.w(TAG, "readSearch Exception:" + e.getMessage());
      }

      Log.d(TAG, String.format("readSearch() finished start=%d, items=%d, total=%d", start, items, total));

      return total;
   }

   public void readArtists(TagListener listener) {
      // check if we have a local cache create a wrapping taglistener to create
      // local cache
      try {
         Log.d(TAG, "readArtists() requesting...");

         // request ALL artists for performance
         // GET
         // /databases/%d/browse/artists?session-id=%s&include-sort-headers=1&index=%d-%d
         byte[] raw = RequestHelper.request(
                  String.format("%s/databases/%d/browse/artists?session-id=%s&include-sort-headers=1",
                           session.getRequestBase(), session.databaseId, session.sessionId), false);

         // parse list, passing off events in the process
         int hits = ResponseParser.performSearch(raw, listener, MLIT_PATTERN, true);
         Log.d(TAG, String.format("readArtists() total=%d", hits));
         raw = null;

      } catch (Exception e) {
          String errStr = e.getMessage();
          if((null != errStr) && errStr.contains("HTTP Error Response Code")){
              if(null != errorListener){
                  String[] splits = errStr.split(":");
                  errorListener.onActionError(Integer.parseInt(splits[1].trim()));
              }
          }
          e.printStackTrace();
          Log.w(TAG, "readArtists Exception:" + e.getMessage());
      }
   }

   public void readAlbums(TagListener listener, String artist) {

      final String encodedArtist = Library.escapeUrlString(artist);

      try {

         // make albums request for this artist
         // http://192.168.254.128:3689/databases/36/groups?session-id=1034286700&meta=dmap.itemname,dmap.itemid,dmap.persistentid,daap.songartist&type=music&group-type=albums&sort=artist&include-sort-headers=1

         byte[] raw = RequestHelper
                  .request(String
                           .format("%s/databases/%d/groups?session-id=%s&meta=dmap.itemname,dmap.itemid,dmap.persistentid,daap.songartist&type=music&group-type=albums&sort=artist&include-sort-headers=1&query='daap.songartist:%s'",
                                    session.getRequestBase(), session.databaseId, session.sessionId, encodedArtist),
                           false);

         // parse list, passing off events in the process
         ResponseParser.performSearch(raw, listener, MLIT_PATTERN, false);

      } catch (Exception e) {
          String errStr = e.getMessage();
          if((null != errStr) && errStr.contains("HTTP Error Response Code")){
              if(null != errorListener){
                  String[] splits = errStr.split(":");
                  errorListener.onActionError(Integer.parseInt(splits[1].trim()));
              }
          }
          e.printStackTrace();
          Log.w(TAG, "readAlbums Exception:" + e.getMessage());
      }

   }

   public void readAlbums(TagListener listener) {
      try {
         byte[] raw = null;

         // make partial album list request
         // http://192.168.254.128:3689/databases/36/groups?session-id=1034286700&meta=dmap.itemname,dmap.itemid,dmap.persistentid,daap.songartist&type=music&group-type=albums&sort=artist&include-sort-headers=1&index=0-50
         raw = RequestHelper
                  .request(String
                           .format("%s/databases/%d/groups?session-id=%s&meta=dmap.itemname,dmap.itemid,dmap.persistentid,daap.songartist&type=music&group-type=albums&sort=album&include-sort-headers=1",
                                    session.getRequestBase(), session.databaseId, session.sessionId), false);

         // parse list, passing off events in the process
         final int hits = ResponseParser.performSearch(raw, listener, MLIT_PATTERN, false);
         Log.i(TAG, "readAlbums Total:" + hits);
      } catch (Exception e) {
          String errStr = e.getMessage();
          if((null != errStr) && errStr.contains("HTTP Error Response Code")){
              if(null != errorListener){
                  String[] splits = errStr.split(":");
                  errorListener.onActionError(Integer.parseInt(splits[1].trim()));
              }
          }
         Log.w(TAG, "readAlbums Exception:" + e.getMessage());
      }
   }

   public void readTracks(String albumid, TagListener listener) {
      try {
         String temp = String
                  .format("%s/databases/%d/containers/%d/items?session-id=%s&meta=dmap.itemname,dmap.itemid,daap.songartist,daap.songalbum,daap.songalbum,daap.songtime,daap.songuserrating,daap.songtracknumber&type=music&sort=album&query='daap.songalbumid:%s'",
                           session.getRequestBase(), session.databaseId, session.libraryId, session.sessionId, albumid);

         // make tracks list request
         // http://192.168.254.128:3689/databases/36/containers/113/items?session-id=1301749047&meta=dmap.itemname,dmap.itemid,daap.songartist,daap.songalbum,daap.songalbum,daap.songtime,daap.songtracknumber&type=music&sort=album&query='daap.songalbumid:11624070975347817354'
         byte[] raw = RequestHelper.request(temp, false);

         // parse list, passing off events in the process
         ResponseParser.performSearch(raw, listener, MLIT_PATTERN, false);

      } catch (Exception e) {
          String errStr = e.getMessage();
          if((null != errStr) && errStr.contains("HTTP Error Response Code")){
              if(null != errorListener){
                  String[] splits = errStr.split(":");
                  errorListener.onActionError(Integer.parseInt(splits[1].trim()));
              }
          }
          e.printStackTrace();
          Log.w(TAG, "readTracks Exception:" + e.getMessage());
      }

   }

   public void readAllTracks(String artist, TagListener listener) {

      // check if we have a local cache create a wrapping taglistener to create
      // local cache
      final String encodedArtist = Library.escapeUrlString(artist);

      try {
         // make tracks list request
         // http://192.168.254.128:3689/databases/36/containers/113/items?session-id=1301749047&meta=dmap.itemname,dmap.itemid,daap.songartist,daap.songalbum,daap.songalbum,daap.songtime,daap.songtracknumber&type=music&sort=album&query='daap.songalbumid:11624070975347817354'
         byte[] raw = RequestHelper
                  .request(String
                           .format("%s/databases/%d/containers/%d/items?session-id=%s&meta=dmap.itemname,dmap.itemid,daap.songartist,daap.songalbum,daap.songalbum,daap.songtime,daap.songuserrating,daap.songtracknumber&type=music&sort=album&query='daap.songartist:%s'",
                                    session.getRequestBase(), session.databaseId, session.libraryId, session.sessionId,
                                    encodedArtist), false);

         // parse list, passing off events in the process
         ResponseParser.performSearch(raw, listener, MLIT_PATTERN, false);

      } catch (Exception e) {
          String errStr = e.getMessage();
          if((null != errStr) && errStr.contains("HTTP Error Response Code")){
              if(null != errorListener){
                  String[] splits = errStr.split(":");
                  errorListener.onActionError(Integer.parseInt(splits[1].trim()));
              }
          }
         Log.w(TAG, "readTracks Exception:" + e.getMessage());
      }
   }

   public void readPlaylists(PlaylistListener listener) {
      for (Playlist ply : this.session.playlists) {
         listener.foundPlaylist(ply);
      }
      listener.searchDone();
   }

   public void readPlaylist(String playlistid, TagListener listener) {
      Log.d(TAG, " in readPlaylists");
      try {
         // http://192.168.254.128:3689/databases/36/containers/1234/items?session-id=2025037772&meta=dmap.itemname,dmap.itemid,daap.songartist,daap.songalbum,dmap.containeritemid,com.apple.tunes.has-video
         byte[] raw = RequestHelper
                  .request(String
                           .format("%s/databases/%d/containers/%s/items?session-id=%s&meta=dmap.itemname,dmap.itemid,daap.songartst,daap.songalbum,daap.songtime,dmap.containeritemid,com.apple.tunes.has-video",
                                    session.getRequestBase(), session.databaseId, playlistid, session.sessionId), false);

         // parse list, passing off events in the process
         ResponseParser.performSearch(raw, listener, MLIT_PATTERN, false);

      } catch (Exception e) {
          String errStr = e.getMessage();
          if((null != errStr) && errStr.contains("HTTP Error Response Code")){
              if(null != errorListener){
                  String[] splits = errStr.split(":");
                  errorListener.onActionError(Integer.parseInt(splits[1].trim()));
              }
          }
         Log.w(TAG, "readPlaylists Exception:" + e.getMessage());
      }
   }

   public void readRadioPlaylists(PlaylistListener listener) {
      if (this.session.supportsRadio()) {
         for (Playlist ply : this.session.getRadioGenres()) {
            listener.foundPlaylist(ply);
         }
      }
      listener.searchDone();
   }

   public void readRadioPlaylist(String playlistid, TagListener listener) {
      Log.d(TAG, " in readRadioPlaylist");
      try {
         // GET /databases/24691/containers/24699/items?
         // meta=dmap.itemname,dmap.itemid,daap.songartist,daap.songalbum,
         // dmap.containeritemid,com.apple.itunes.has-video,daap.songdisabled,
         // com.apple.itunes.mediakind,daap.songdescription
         // &type=music&session-id=345827905
         byte[] raw = RequestHelper.request(
                  String.format("%s/databases/%d/containers/%s/items?"
                           + "meta=dmap.itemname,dmap.itemid,daap.songartist,daap.songalbum,"
                           + "dmap.containeritemid,com.apple.itunes.has-video,daap.songdisabled,"
                           + "com.apple.itunes.mediakind,daap.songdescription" + "&type=music&session-id=%s",
                           session.getRequestBase(), session.radioDatabaseId, playlistid, session.sessionId), false);

         // parse list, passing off events in the process
         ResponseParser.performSearch(raw, listener, MLIT_PATTERN, false);

      } catch (Exception e) {
          String errStr = e.getMessage();
          if((null != errStr) && errStr.contains("HTTP Error Response Code")){
              if(null != errorListener){
                  String[] splits = errStr.split(":");
                  errorListener.onActionError(Integer.parseInt(splits[1].trim()));
              }
          }
         Log.w(TAG, "readRadioPlaylist Exception:" + e.getMessage());
      }
   }

   public boolean readNowPlaying(String albumid, TagListener listener) {

      // Try Wilco (Alex W)'s nowplaying extension /ctrl-int/1/items
      try {
         String query = String
                  .format("%s/ctrl-int/1/items?session-id=%s&meta=dmap.itemname,dmap.itemid,daap.songartist,daap.songalbum,daap.songalbum,daap.songtime,daap.songuserrating,daap.songtracknumber&type=music&sort=album&query='daap.songalbumid:%s'",
                           session.getRequestBase(), session.sessionId, albumid);

         byte[] raw = RequestHelper.request(query, false);

         // parse list, passing off events in the process
         ResponseParser.performSearch(raw, listener, MLIT_PATTERN, false);
         return false;

      } catch (Exception e) {
         // Fall back to reading album
         if (albumid != null && albumid.length() > 0)
            readTracks(albumid, listener);
         else
            readCurrentSong(listener);

         return true;
      }

   }

   public void readCurrentSong(TagListener listener) {
      // reads the current playing song as a one-item playlist
      try {
         String temp = String.format("%s/ctrl-int/1/playstatusupdate?revision-number=1&session-id=%s",
                  session.getRequestBase(), session.sessionId);

         // Refactor response into one that looks like a normal items request
         // and trigger listener
         Response resp = RequestHelper.requestParsed(temp, false).getNested("cmst");
         if (resp.containsKey("cann")) {
            Response new_item = new Response();
            new_item.put("minm", resp.getString("cann"));
            new_item.put("asal", resp.getString("canl"));
            new_item.put("asar", resp.getString("cana"));
            new_item.put("astm", resp.getString("cast"));

            listener.foundTag("mlit", new_item);
         }
         listener.searchDone();
      } catch (Exception e) {
          String errStr = e.getMessage();
          if((null != errStr) && errStr.contains("HTTP Error Response Code")){
              if(null != errorListener){
                  String[] splits = errStr.split(":");
                  errorListener.onActionError(Integer.parseInt(splits[1].trim()));
              }
          }
         Log.w(TAG, "readCurrentSong Exception:" + e.getMessage());
      }
   }

   public void readNameList(TagListener listener) {
      try {
         Log.d(TAG, "readNameList() requesting...");

         // GET
         byte[] raw = RequestHelper.request(
                 String.format("%s/rename?name-list=1", session.getRequestBase()), false);
         Log.w(TAG, "readNameList start performSearch");
         // parse list, passing off events in the process
         int hits = ResponseParser.performSearch(raw, listener, MLIT_PATTERN, true);
         Log.d(TAG, String.format("readNameList() total=%d", hits));
         raw = null;

      } catch (Exception e) {
          String errStr = e.getMessage();
          if((null != errStr) && errStr.contains("HTTP Error Response Code")){
              if(null != errorListener){
                  String[] splits = errStr.split(":");
                  errorListener.onActionError(Integer.parseInt(splits[1].trim()));
              }
          }
         Log.w(TAG, "readNameList Exception:" + e.getMessage());
      }
   }

   public void setShoutcastUrl(String url) {
      Log.d(TAG, " in setShoutcastUrl");
      try {
         RequestHelper.request(String
                         .format("%s/ctrl-int/1/setproperty?shoutcast-url=%s&com.apple.itunes.extended-media-kind=1"+ "&session-id=%s",
                                 session.getRequestBase(), url, session.sessionId), false);
         } catch (Exception e) {
          String errStr = e.getMessage();
          if((null != errStr) && errStr.contains("HTTP Error Response Code")){
              if(null != errorListener){
                  String[] splits = errStr.split(":");
                  errorListener.onActionError(Integer.parseInt(splits[1].trim()));
              }
          }
         Log.w(TAG, "setShoutcastUrl Exception:" + e.getMessage());
      }
   }

    public void setRadioTunesUrl(String url) {
        Log.d(TAG, " in setRadioTunesUrl");
        if (null == url || url.isEmpty())
            return;
        try {
            String data = null;
            if (url.contains("?")) {
                //url contain parameter, transfer parameter by payload
                data = url.substring(url.indexOf("?"));
                url = url.substring(0, url.indexOf("?"));
            }
            RequestHelper.request(String.format("%s/ctrl-int/1/seturl?url=%s&com.apple.itunes.extended-media-kind=1"+ "&session-id=%s",
                            session.getRequestBase(), url, session.sessionId), data, false);
        } catch (Exception e) {
            String errStr = e.getMessage();
            if((null != errStr) && errStr.contains("HTTP Error Response Code")){
                if(null != errorListener){
                    String[] splits = errStr.split(":");
                    errorListener.onActionError(Integer.parseInt(splits[1].trim()));
                }
            }
            Log.w(TAG, "setRadioTunesUrl Exception:" + e.getMessage());
        }
    }

   /**
    * URL encode a string escaping single quotes first.
    * <p>
    * @param input the string to encode
    * @return the URL encoded string value
    */
   public static String escapeUrlString(final String input) {
      String encoded = "";
      try {
         encoded = URLEncoder.encode(input, "UTF-8");
         encoded = encoded.replaceAll("\\+", "%20");
         encoded = encoded.replaceAll("%27", "%5C'");
      } catch (UnsupportedEncodingException e) {
         Log.w(TAG, "escapeUrlString Exception:" + e.getMessage());
      }
      return encoded;
   }
}
