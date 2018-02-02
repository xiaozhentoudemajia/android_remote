package aca.com.remote.tunes.util;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Created by jim.yu on 2017/9/22.
 */

public class ShoutCastRequest {
    private String LogTag = ShoutCastRequest.class.getName();
    private String DeviceID = "hUNsgQY47tdTYkNp";
    private final int SHOUTCAST_RESPONSE_XML = 0;
    private final int SHOUTCAST_RESPONSE_JSON = 1;
    private final int SHOUTCAST_RESPONSE_M3U = 2;

    public static final int eSHOUTCAST_MSG_STATUS_CODE = 0;
    public static final int eSHOUTCAST_MSG_STATUS_TEXT = 1;
    public static final int eSHOUTCAST_MSG_TUNE_BASE = 2;
    public static final int eSHOUTCAST_MSG_STATION = 3;
    public static final int eSHOUTCAST_MSG_GENRE = 4;
    public static final int eSHOUTCAST_MSG_URL = 5;
    public static final int eSHOUTCAST_MSG_XML_PARSER_START = 7;
    public static final int eSHOUTCAST_MSG_XML_PARSER_END = 8;

    private XmlPullParserFactory factory;
    private XmlPullParser parser;

    private RadioRequestCallback mCallback = null;
    private String base_m3u = null;

//    private ArrayList<radioStation> stationList = new ArrayList<radioStation>();
//    private ArrayList<radioGenre> genreList = new ArrayList<radioGenre>();

    //Process data return by ShoutCast server
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);

            String response = null;
            switch (msg.what) {
                case SHOUTCAST_RESPONSE_XML:
                    response = msg.obj.toString();
                    Log.i(LogTag, "Response is :::" + response);
                    try {
                        factory = XmlPullParserFactory.newInstance();
                        parser = factory.newPullParser();
                        parser.setInput(new ByteArrayInputStream(response.getBytes()), "UTF-8");
                        boolean b_needClearGenreList = true;

                        String tag;
                        String attributeName;
                        ShoutCastRadioStation rs = null;
                        ShoutCastRadioGenre rg = null;
//                        builder = factory.newDocumentBuilder();
//                        Document doc = builder.parse(new ByteArrayInputStream(response.getBytes()));

//                        NodeList nList = doc.getElementsByTagName("tunein");

                        int eventType = parser.getEventType();
                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            switch (eventType) {
                                case XmlPullParser.START_DOCUMENT:
                                    if (null != mCallback)
                                        mCallback.messageCallback(eSHOUTCAST_MSG_XML_PARSER_START, null);
                                    break;
                                case XmlPullParser.START_TAG:
                                    tag = parser.getName();
                                    if (tag.equals("stationlist")) {
                                        //station list
//                                        stationList.clear();
                                    } else if (tag.equals("station")) {
                                        //station
                                        rs = new ShoutCastRadioStation();
                                        int num = parser.getAttributeCount();
                                        for(int i = 0; i < num; i++){
                                            attributeName = parser.getAttributeName(i);
                                            if (attributeName.equals("name")) {
                                                rs.setName(parser.getAttributeValue(i));
                                            } else if (attributeName.equals("mt")) {
                                                rs.setMediaType(parser.getAttributeValue(i));
                                            } else if (attributeName.equals("id")) {
                                                rs.setId(Integer.valueOf(parser.getAttributeValue(i)));
                                            } else if (attributeName.equals("br")) {
                                                rs.setBitrate(Integer.valueOf(parser.getAttributeValue(i)));
                                            } else if (attributeName.equals("ct")) {
                                                rs.setCt(parser.getAttributeValue(i));
                                            } else if (attributeName.equals("lc")) {
                                                rs.setLc(Integer.valueOf(parser.getAttributeValue(i)));
                                            } else if (attributeName.equals("ml")) {
                                                rs.setMl(Integer.valueOf(parser.getAttributeValue(i)));
                                            }else if (attributeName.startsWith("genre")) {
                                                rs.setGenre(parser.getAttributeValue(i));
                                            } else if (attributeName.equals("logo")){
                                                rs.setLogo(parser.getAttributeValue(i));
                                            } else {
                                                Log.e("XML", "xml station attribute contain unknow tag:" + attributeName);
                                            }
                                        }
                                        if (null != mCallback)
                                            mCallback.messageCallback(eSHOUTCAST_MSG_STATION, rs);
                                    } else if (tag.equals("tunein")) {
                                        //tunein m3u/pls
                                        int num = parser.getAttributeCount();
                                        for(int i = 0; i < num; i++) {
                                            attributeName = parser.getAttributeName(i);
                                            if (attributeName.equals("base-m3u")) {
                                                base_m3u = parser.getAttributeValue(null, "base-m3u");
                                                if (null != mCallback)
                                                    mCallback.messageCallback(eSHOUTCAST_MSG_TUNE_BASE, base_m3u);
                                                break;
                                            }
                                        }

                                    } else if (tag.equals("genrelist")) {
                                        //genre list
                                        if (b_needClearGenreList) {
                                            b_needClearGenreList = false;
//                                            genreList.clear();
                                        } else {
                                            Log.i("XML", "Sub genre list");
                                        }
                                    } else if (tag.equals("genre")) {
                                        rg = new ShoutCastRadioGenre();
                                        int num = parser.getAttributeCount();
                                        for (int i = 0; i < num; i++) {
                                            attributeName = parser.getAttributeName(i);
                                            if (attributeName.equals("name")) {
                                                rg.setName(parser.getAttributeValue(i));
                                            } else if (attributeName.equals("id")) {
                                                rg.setId(Integer.valueOf(parser.getAttributeValue(i)));
                                            } else if (attributeName.equals("parentid")) {
                                                rg.setParent_id(Integer.valueOf(parser.getAttributeValue(i)));
                                            } else if (attributeName.equals("haschildren")) {
                                                rg.setHasChildren(Boolean.valueOf(parser.getAttributeValue(i)));
                                            } else {
                                                Log.e("XML", "xml genre attribute contain unknow tag:" + attributeName);
                                            }
                                        }
                                        if (null != mCallback)
                                            mCallback.messageCallback(eSHOUTCAST_MSG_GENRE, rg);
                                    } else if (tag.equals("statusCode")) {
                                        String str = parser.nextText();
                                        Log.i("XML", "xml response code " + str);
                                        if (null != mCallback)
                                            mCallback.messageCallback(eSHOUTCAST_MSG_STATUS_CODE
                                                    , str);
                                    } else if (tag.equals("statusText")) {
                                        String str = parser.nextText();
                                        Log.i("XML", "xml response text " + str);
                                        if (null != mCallback)
                                            mCallback.messageCallback(eSHOUTCAST_MSG_STATUS_TEXT
                                                    , str);
                                    } else {
                                        Log.e("XML", "xml genre attribute contain unknow tag:" + tag);
                                    }

                                    break;
                                case XmlPullParser.END_TAG:
                                    tag = parser.getName();
                                    if (tag.equals("stationlist")) {

                                    } else if (tag.equals("station")) {
//                                        stationList.add(rs);
                                        rs = null;
                                    } else if (tag.equals("genre")) {
                                        if (0 == rg.getParent_id()) {
                                            //primary genre
//                                            genreList.add(rg);
                                        } else {
                                            //secondary genre, need add to parent genre
//                                            radioGenre pg = genreList.get(genreList.size() - 1);
//                                            if (pg.getId() == rg.getParent_id()) {
//
//                                            } else {
//                                                //need find parent genre
//                                                Log.e("XML", "Parent genre is not " + (genreList.size() - 1));
//                                                for (int i = 0; i < genreList.size(); i++) {
//                                                    if (genreList.get(i).getId() == rg.getParent_id()) {
//                                                        pg = genreList.get(i);
//                                                        Log.e("XML", "Parent genre is " + i);
//                                                        break;
//                                                    }
//                                                }
//                                            }
//                                            pg.setChildren(rg);
                                        }
                                        rg = null;
                                    } else if (tag.equals("genrelist")) {

                                    }

                                    break;
                            }
                            eventType = parser.next();
                        }
                        if (null != mCallback)
                            mCallback.messageCallback(eSHOUTCAST_MSG_XML_PARSER_END, null);
                        Log.i(LogTag, "Finish XML parse");
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                    break;
                case SHOUTCAST_RESPONSE_M3U:
                    response = msg.obj.toString();
                    Log.i(LogTag, "Response is :::" + response);
                    List<String> strList = Arrays.asList(response.split("\n"));
                    for (String str:strList) {
                        if (str.contains("http")) {
                            Log.i(LogTag, "Radio URL is :::" + str);
                            if (null != mCallback)
                                mCallback.messageCallback(eSHOUTCAST_MSG_URL, str);
                            break;
                        }
                    }
                    break;
            }
        }
    };

    //Send request to ShoutCast server and get server response
    private void sendRequestWithHttpClient(final String url, final int responseType){
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection httpConn = null;
                try {
                    URL con_url = new URL(url);
                    httpConn  = (HttpURLConnection)con_url.openConnection();
                    httpConn.setRequestMethod("GET");
                    httpConn.setDoInput(true);
                    httpConn.setUseCaches(false);
                    httpConn.setRequestProperty("Connection", "Keep-Alive");
                    httpConn.setRequestProperty("User-Agent",
                            "Mozilla/5.0 (Windows; U; Windows NT 5.1; " + Locale.getDefault()
                                    + "; rv:1.9.0.3) Gecko/2008092417 Firefox/3.0.3");

                    //get response
                    int responseCode = httpConn.getResponseCode();
                    if(HttpURLConnection.HTTP_OK == responseCode){//Connection OK
                        StringBuffer sb = new StringBuffer();
                        String readLine;
                        BufferedReader responseReader;

                        responseReader = new BufferedReader(new InputStreamReader(httpConn.getInputStream(), Charset.forName("UTF-8")));
                        while ((readLine = responseReader.readLine()) != null) {
                            sb.append(readLine).append("\n");
//                            Log.e(LogTag, "XML: " + readLine);
                        }
                        responseReader.close();

                        Message message = Message.obtain();
                        message.what = responseType;
                        message.obj = sb;
                        handler.sendMessage(message);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (null != httpConn)
                        httpConn.disconnect();
                }
            }
        }).start();
    }

    public ShoutCastRequest(String deviceID) {
        if (null != deviceID)
            DeviceID = deviceID;
    }

    public void setShoutCastCallback(RadioRequestCallback callback) {
        mCallback = callback;
    }

    //get top 500 station
    public int getTop500Stations(int limit, int bitrate, String mediaType){

        String requestStr = "http://api.shoutcast.com/legacy/Top500?k=" + DeviceID;

        if(limit > 0 && limit <501)
            requestStr += "&limit=" + Integer.toString(limit);
        else if (limit < 0 || limit > 500)
            Log.e(LogTag, "Param limit error in getTop500Station func!");

        if(bitrate > 0)
            requestStr += "&br=" + Integer.toString(bitrate);
        else if (bitrate < 0)
            Log.e(LogTag, "Param bitrate error in getTop500Station func!");

        if(mediaType != null && !mediaType.isEmpty()) {
            if (mediaType.equalsIgnoreCase("mp3"))
                requestStr += "&mt=audio/mpeg";
            else if (mediaType.equalsIgnoreCase("aac+"))
                requestStr += "&mt=audio/aacp";
            else
                Log.e(LogTag, "Param mediaType error in getTop500Station func!");
        }

        Log.i(LogTag, "Request String is " + requestStr);

        sendRequestWithHttpClient(requestStr, SHOUTCAST_RESPONSE_XML);
        return 0;
    }

    //get station by keyword search
    public int getStationsByKeyword(int offset, int limit, int bitrate, String mediaType, String searchStr){
        String requestStr = "http://api.shoutcast.com/legacy/stationsearch?k" + DeviceID;

        if(searchStr != null && !searchStr.isEmpty()){
            requestStr += "&search=" + searchStr;
        } else {
            Log.e(LogTag, "Param searchStr error in getStationsByKeyword func!");
            return -1;
        }

        if(-1 == offset){//no offset
            if(limit > 0){
                requestStr += "&limit=" + Integer.toString(limit);
            } else if (limit < 0){
                Log.e(LogTag, "Param limit error in getStationsByKeyword func!");
            }
        } else if(offset >= 0) {
            if(limit > 0){
                requestStr += "&limit=" + Integer.toString(offset) + "," + Integer.toString(limit);
            } else {
                Log.e(LogTag, "Param limit error in getStationsByKeyword func!");
            }
        } else {
            Log.e(LogTag, "Param offset error in getStationsByKeyword func!");
        }

        if(bitrate > 0)
            requestStr += "&br=" + Integer.toString(bitrate);
        else if (bitrate < 0)
            Log.e(LogTag, "Param bitrate error in getStationsByKeyword func!");

        if(mediaType != null && !mediaType.isEmpty()) {
            if (mediaType.equalsIgnoreCase("mp3"))
                requestStr += "&mt=audio/mpeg";
            else if (mediaType.equalsIgnoreCase("aac+"))
                requestStr += "&mt=audio/aacp";
            else
                Log.e(LogTag, "Param mediaType error in getStationsByKeyword func!");
        }

        Log.i(LogTag, "Request String is " + requestStr);

        sendRequestWithHttpClient(requestStr, SHOUTCAST_RESPONSE_XML);
        return 0;
    }

    //get station by Genre
    public int getStationsByGenre(int offset, int limit, int bitrate, String mediaType, String genreStr){
        String requestStr = "http://api.shoutcast.com/legacy/genresearch?k=" + DeviceID;

        if(genreStr != null && !genreStr.isEmpty()){
            requestStr += "&genre=" + genreStr;
        } else {
            Log.e(LogTag, "Param genreStr error in getStationsByGenre func!");
            return -1;
        }

        if(-1 == offset){//no offset
            if(limit > 0){
                requestStr += "&limit=" + Integer.toString(limit);
            } else if (limit < 0){
                Log.e(LogTag, "Param limit error in getStationsByKeyword func!");
            }
        } else if(offset >= 0) {
            if(limit > 0){
                requestStr += "&limit=" + Integer.toString(offset) + "," + Integer.toString(limit);
            } else {
                Log.e(LogTag, "Param limit error in getStationsByKeyword func!");
            }
        } else {
            Log.e(LogTag, "Param offset error in getStationsByKeyword func!");
        }

        if(bitrate > 0)
            requestStr += "&br=" + Integer.toString(bitrate);
        else if (bitrate < 0)
            Log.e(LogTag, "Param bitrate error in getStationsByKeyword func!");

        if(mediaType != null && !mediaType.isEmpty()) {
            if (mediaType.equalsIgnoreCase("mp3"))
                requestStr += "&mt=audio/mpeg";
            else if (mediaType.equalsIgnoreCase("aac+"))
                requestStr += "&mt=audio/aacp";
            else
                Log.e(LogTag, "Param mediaType error in getStationsByKeyword func!");
        }

        Log.i(LogTag, "Request String is " + requestStr);

        sendRequestWithHttpClient(requestStr, SHOUTCAST_RESPONSE_XML);
        return 0;
    }

    //get station by Now playing
    public int getStationsBaseOnNowPlayingInfo(int limit, int bitrate, String mediaType, String  genre, String ct){
        String requestStr = "http://api.shoutcast.com/station/nowplaying?k=" + DeviceID;

        if(ct != null && !ct.isEmpty()){
            requestStr += "&ct=" + ct;
        } else {
            Log.e(LogTag, "Param genreStr error in getStationsBaseOnNowPlayingInfo func!");
            return -1;
        }

        requestStr += "&f=xml";//default response xml

        if(limit > 0){
            requestStr += "&limit=" + Integer.toString(limit);
        } else if(limit < 0){
            Log.e(LogTag, "Param limit error in getStationsBaseOnNowPlayingInfo func!");
        }

        if(bitrate > 0)
            requestStr += "&br=" + Integer.toString(bitrate);
        else if (bitrate < 0)
            Log.e(LogTag, "Param bitrate error in getStationsBaseOnNowPlayingInfo func!");

        if(mediaType != null && !mediaType.isEmpty()) {
            if (mediaType.equalsIgnoreCase("mp3"))
                requestStr += "&mt=audio/mpeg";
            else if (mediaType.equalsIgnoreCase("aac+"))
                requestStr += "&mt=audio/aacp";
            else
                Log.e(LogTag, "Param mediaType error in getStationsBaseOnNowPlayingInfo func!");
        }

        if(genre != null && !genre.isEmpty()){
            requestStr += "&genre=" + genre;
        }

        Log.i(LogTag, "Request String is " + requestStr);

        sendRequestWithHttpClient(requestStr, SHOUTCAST_RESPONSE_XML);
        return 0;
    }

    //get station by GenreID
    public int getStationsByBitrateOrCodecTypeOrGenreID(int bitrate, String mediaType, int genreID, int limit, String genre){
        String requestStr = "http://api.shoutcast.com/station/advancedsearch?k=" + DeviceID + "&f=xml";

        if(bitrate > 0)
            requestStr += "&br=" + Integer.toString(bitrate);
        else if (bitrate < 0)
            Log.e(LogTag, "Param bitrate error in getStationsByBitrateOrCodecTypeOrGenreID func!");

        if(mediaType != null && !mediaType.isEmpty()) {
            if (mediaType.equalsIgnoreCase("mp3"))
                requestStr += "&mt=audio/mpeg";
            else if (mediaType.equalsIgnoreCase("aac+"))
                requestStr += "&mt=audio/aacp";
            else
                Log.e(LogTag, "Param mediaType error in getStationsByBitrateOrCodecTypeOrGenreID func!");
        }

        if(genreID > 0)
            requestStr += "&genre_id=" + Integer.toString(genreID);
        else if (genreID < 0)
            Log.e(LogTag, "Param genreID error in getStationsByBitrateOrCodecTypeOrGenreID func!");

        if(limit > 0){
            requestStr += "&limit=" + Integer.toString(limit);
        } else if(limit < 0){
            Log.e(LogTag, "Param limit error in getStationsByBitrateOrCodecTypeOrGenreID func!");
        }

        if(genre != null && !genre.isEmpty()){
            requestStr += "&genre=" + genre;
        }

        Log.i(LogTag, "Request String is " + requestStr);

        sendRequestWithHttpClient(requestStr, SHOUTCAST_RESPONSE_XML);
        return 0;
    }

    //get station by random
    public int getRandomStations(int limit, int bitrate, String mediaType, String  genre){
        String requestStr = "http://api.shoutcast.com/station/randomstations?k=" + DeviceID + "&f=xml";

        if(limit > 0){
            requestStr += "&limit=" + Integer.toString(limit);
        } else if(limit < 0){
            Log.e(LogTag, "Param limit error in getRandomStations func!");
        }

        if(bitrate > 0)
            requestStr += "&br=" + Integer.toString(bitrate);
        else if (bitrate < 0)
            Log.e(LogTag, "Param bitrate error in getRandomStations func!");

        if(mediaType != null && !mediaType.isEmpty()) {
            if (mediaType.equalsIgnoreCase("mp3"))
                requestStr += "&mt=audio/mpeg";
            else if (mediaType.equalsIgnoreCase("aac+"))
                requestStr += "&mt=audio/aacp";
            else
                Log.e(LogTag, "Param mediaType error in getRandomStations func!");
        }

        if(genre != null && !genre.isEmpty()){
            requestStr += "&genre=" + genre;
        }

        Log.i(LogTag, "Request String is " + requestStr);

        sendRequestWithHttpClient(requestStr, SHOUTCAST_RESPONSE_XML);
        return 0;
    }

    public int getAllGenre(){
        String requestStr = "http://api.shoutcast.com/legacy/genrelist?k=" + DeviceID;

        Log.i(LogTag, "Request String is " + requestStr);

        sendRequestWithHttpClient(requestStr, SHOUTCAST_RESPONSE_XML);
        return 0;
    }

    public int getPrimaryGenre(){
        String requestStr = "http://api.shoutcast.com/genre/primary?k=" + DeviceID + "&f=xml";

        Log.i(LogTag, "Request String is " + requestStr);

        sendRequestWithHttpClient(requestStr, SHOUTCAST_RESPONSE_XML);
        return 0;
    }

    public int getSecondGenre(int parentID){
        String requestStr = "http://api.shoutcast.com/genre/secondary?k=" + DeviceID + "&f=xml";

        if(parentID >= 0){
            requestStr += "&parentid=" + Integer.toString(parentID);
        } else {
            Log.e(LogTag, "Param parentID error in getSecondGenre func!");
            return -1;
        }

        Log.i(LogTag, "Request String is " + requestStr);

        sendRequestWithHttpClient(requestStr, SHOUTCAST_RESPONSE_XML);
        return 0;
    }

    public int getGenreDetailByGenreID(int genreID){
        String requestStr = "http://api.shoutcast.com/genre/secondary?k=" + DeviceID + "&f=xml";

        if(genreID >= 0){
            requestStr += "&id=" + Integer.toString(genreID);
        } else {
            Log.e(LogTag, "Param genreID error in getGenreDetailByGenreID func!");
            return -1;
        }

        Log.i(LogTag, "Request String is " + requestStr);

        sendRequestWithHttpClient(requestStr, SHOUTCAST_RESPONSE_XML);
        return 0;
    }

    public int getGenreBaseOnAvialabilitySubGenres(boolean hasChildren){
        String requestStr = "http://api.shoutcast.com/genre/secondary?k=" + DeviceID + "&f=xml";

        requestStr += "&haschildren=" + (hasChildren?"true":"false");

        Log.i(LogTag, "Request String is " + requestStr);

        sendRequestWithHttpClient(requestStr, SHOUTCAST_RESPONSE_XML);
        return 0;
    }

    //tune in to station
    public void tuneIntoStation(String base, int stationID){
        String requestStr = "http://yp.shoutcast.com";
        if(null == base || base.isEmpty() || stationID < 0){
            Log.e(LogTag, "Param error in tuneIntoStation Func");
        }
        if(base.startsWith("/")){
            requestStr += base + "?";
        } else {
            requestStr += "/" + base + "?";
        }
        requestStr += "id=" + stationID;

        Log.i(LogTag, "Request String is " + requestStr);

        sendRequestWithHttpClient(requestStr, SHOUTCAST_RESPONSE_M3U);
    }

    public void sendRequest(String requestStr) {
        sendRequestWithHttpClient(requestStr, SHOUTCAST_RESPONSE_XML);
    }
}
