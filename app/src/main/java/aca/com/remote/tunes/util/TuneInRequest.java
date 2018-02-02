package aca.com.remote.tunes.util;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by jim.yu on 2017/10/26.
 */

public class TuneInRequest {
    private String LogTag = TuneInRequest.class.getName();
    private String BASE_URL = "http://opml.radiotime.com/";
    public static String PartnerID = "RadioTime";
    public static String serial = "35521ee6-a3c7-478c-9ca8-0b34d42bdb9c";

    private final static int TUNEIN_RESPONSE_XML = 0;
    private final static int TUNEIN_RESPONSE_TUNE = 1;
    private final static int TUNEIN_RESPONSE_PLS = 2;
    private final static int TUNEIN_RESPONSE_M3U = 3;
    private final static int TUNEIN_RESPONSE_URL = 4;
    private final static int TUNEIN_RESPONSE_ERROR = 5;

    public static final int eTUNEIN_MSG_STATUS_CODE = 0;
    public static final int eTUNEIN_MSG_TITLE = 1;
    public static final int eTUNEIN_MSG_LINK = 2;
    public static final int eTUNEIN_MSG_STATION_AUDIO = 3;
    public static final int eTUNEIN_MSG_STATION_SHOW = 4;
    public static final int eTUNEIN_MSG_STATION_TOPIC = 5;
    public static final int eTUNEIN_MSG_URL = 6;
    public static final int eTUNEIN_MSG_XML_PARSER_START = 7;
    public static final int eTUNEIN_MSG_XML_PARSER_END = 8;
    public static final int eTUNEIN_MSG_ERROR = 9;

    private Map<String, String> globleParam;
    private RadioRequestCallback mCallback = null;

    private XmlPullParserFactory factory;
    private XmlPullParser parser;

    private void addLink(XmlPullParser p) {
        TuneInLink link;
        String attributeName, attributeValue;
        int num = p.getAttributeCount();

        if (num <= 0)
            return;

        link = new TuneInLink();
        for(int i = 0; i < num; i++) {
            attributeName = p.getAttributeName(i);
            attributeValue = p.getAttributeValue(i);
            if (attributeName.equals("type")) {
                link.setType(attributeValue);
            } else if (attributeName.equals("text")) {
                link.setText(attributeValue);
            } else if (attributeName.equals("URL")) {
                link.setUrl(attributeValue);
            } else if (attributeName.equals("key")) {
                link.setKey(attributeValue);
            } else if (attributeName.equals("guide_id")) {
                link.setGuide_id(attributeValue);
            }else {
                Log.e(LogTag, "Unsupport attribute in outline for LINK!! attribute: " + attributeName);
            }
        }
        if (null != mCallback)
            mCallback.messageCallback(eTUNEIN_MSG_LINK, link);
    }

    private void addStation(XmlPullParser p, TuneInElement station) {
        String attributeName;
        int num = p.getAttributeCount();

        if (num <= 0)
            return;

//        station = new TuneInElement();
        for (int i = 0; i < num; i++) {
            attributeName = p.getAttributeName(i);
            if (attributeName.equals("type")) {
                station.setType(p.getAttributeValue(i));
            } else if (attributeName.equals("text")) {
                station.setText(p.getAttributeValue(i));
            } else if (attributeName.equals("URL")) {
                station.setUrl(p.getAttributeValue(i));
            } else if (attributeName.equals("bitrate")) {
                station.setBitrate(Integer.valueOf(p.getAttributeValue(i)));
            } else if (attributeName.equals("reliability")) {
                station.setReliability(Integer.valueOf(p.getAttributeValue(i)));
            } else if (attributeName.equals("guide_id")) {
                station.setGuide_id(p.getAttributeValue(i));
            } else if (attributeName.equals("subtext")) {
                station.setSubtext(p.getAttributeValue(i));
            } else if (attributeName.equals("genre_id")) {
                station.setGenre_id(p.getAttributeValue(i));
            } else if (attributeName.equals("formats")) {
                station.setFormat(p.getAttributeValue(i));
            } else if (attributeName.equals("show_id")) {
                station.setShow_id(p.getAttributeValue(i));
            } else if (attributeName.equals("item")) {
                station.setItem(p.getAttributeValue(i));
            } else if (attributeName.equals("image")) {
                station.setImage(p.getAttributeValue(i));
            } else if (attributeName.equals("current_track")) {
                station.setCurrent_track(p.getAttributeValue(i));
            } else if (attributeName.equals("now_playing_id")) {
                station.setNow_playing_id(p.getAttributeValue(i));
            } else if (attributeName.equals("preset_id")) {
                station.setPreset_id(p.getAttributeValue(i));
            } else if (attributeName.equals("subscription_required")) {
                station.setSubscription_required(Boolean.valueOf(p.getAttributeValue(i)));
            } else if (attributeName.equals("playing_image")) {
                station.setPlaying_image(p.getAttributeValue(i));
            } else if (attributeName.equals("playing")) {
                station.setPlaying(p.getAttributeValue(i));
            } else {
                Log.e(LogTag, "Unsupport attribute in outline for STATION!! attribute: " + attributeName);
            }
        }

    }


    //Process data return by ShoutCast server
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            String response = null;

            switch (msg.what) {
                case TUNEIN_RESPONSE_XML:
                    response = msg.obj.toString();
                    try {
                        factory = XmlPullParserFactory.newInstance();
                        parser = factory.newPullParser();
                        parser.setInput(new ByteArrayInputStream(response.getBytes()), "UTF-8");

                        String tag;
                        String element_type;
                        String element_item;
                        String element_guide_id;
                        String element_key = null;

                        int eventType = parser.getEventType();
                        while (eventType != XmlPullParser.END_DOCUMENT) {

                            switch (eventType) {
                                case XmlPullParser.START_DOCUMENT:
                                    if (null != mCallback)
                                        mCallback.messageCallback(eTUNEIN_MSG_XML_PARSER_START, null);
                                    break;

                                case XmlPullParser.START_TAG:
                                    tag = parser.getName();
                                    if (tag.equals("outline")) {// outline tag
                                        /**
                                         * <opml version="1">
                                         *     <head>
                                         *         <title>Browse</title>
                                         *         <status>200</status>
                                         *     </head>
                                         *     <body>
                                         *         <outline XXXXXXXXXXXXXXXX/>
                                         *
                                         *         <outline XXXXXXXXXXXXXXXX>
                                         *             <outline XXXXXXXXXXXXXXXX/>
                                         *         </outline>
                                         *     </body>
                                         * </opml>
                                         *
                                         * May be outline will have child tag outline, top level
                                         * outline depth is 3, if outline is child of topper level
                                         * outline, the depth is parent outline depth + 1
                                         * */
                                        element_type = parser.getAttributeValue(null, "type");
                                        element_guide_id = parser.getAttributeValue(null, "guide_id");
                                        element_item = parser.getAttributeValue(null, "item");

                                        int depth = parser.getDepth();
                                        TuneInElement station = new TuneInElement();
                                        if (3 == depth) {//top outline, judge sequence item->type
                                            element_key = parser.getAttributeValue(null, "key");
                                            if (null != element_item
                                                    && element_item.equals("station")) {
                                                //audio station
                                                addStation(parser, station);
                                                if (null != mCallback)
                                                    mCallback.messageCallback(eTUNEIN_MSG_STATION_AUDIO, station);
                                            } else if (null != element_item
                                                    && element_item.equals("show")) {
                                                //show station
                                                addStation(parser, station);
                                                if (null != mCallback)
                                                    mCallback.messageCallback(eTUNEIN_MSG_STATION_SHOW, station);
                                            } else if (null != element_type
                                                    && element_type.equals("audio")
                                                    && null != element_guide_id
                                                    && is_station_id(element_guide_id)) {
                                                //audio station
                                                addStation(parser, station);
                                                if (null != mCallback)
                                                    mCallback.messageCallback(eTUNEIN_MSG_STATION_AUDIO, station);
                                            } else if (null != element_type
                                                    && element_type.equals("audio")
                                                    && null != element_guide_id
                                                    && is_show_id(element_guide_id)) {
                                                //show station
                                                addStation(parser, station);
                                                if (null != mCallback)
                                                    mCallback.messageCallback(eTUNEIN_MSG_STATION_SHOW, station);
                                            } else if (null != element_type
                                                    && element_type.equals("audio")
                                                    && null != element_guide_id
                                                    && is_topic_id(element_guide_id)) {
                                                //topic station
                                                addStation(parser, station);
                                                if (null != mCallback)
                                                    mCallback.messageCallback(eTUNEIN_MSG_STATION_TOPIC, station);
                                            } else if (null != element_type
                                                    && element_type.equals("link")) {
                                                addLink(parser);
                                            } else {
//                                                if (null == element_key)
//                                                    Log.e(LogTag, "Un-procedure outline item !!");
                                            }
                                        } else if (4 == depth) {//has parent outline
                                            if (null != element_key
                                                    && element_key.equals("stations")) {
                                                if (null != element_item
                                                        && element_item.equals("station")) {
                                                    //audio station
                                                    addStation(parser, station);
                                                    if (null != mCallback)
                                                        mCallback.messageCallback(eTUNEIN_MSG_STATION_AUDIO, station);
                                                } else if (null != element_type
                                                        && element_type.equals("link")) {
                                                    addLink(parser);
                                                } else {
                                                    Log.e(LogTag, "Un-procedure outline item !!");
                                                }
                                            } else if (null != element_key
                                                    && element_key.equals("shows")) {
                                                if (null != element_item
                                                        && element_item.equals("show")) {
                                                    //show station
                                                    addStation(parser, station);
                                                    if (null != mCallback)
                                                        mCallback.messageCallback(eTUNEIN_MSG_STATION_SHOW, station);
                                                } else if (null != element_type
                                                        && element_type.equals("link")) {
                                                    addLink(parser);
                                                } else {
                                                    Log.e(LogTag, "Un-procedure outline item !!");
                                                }
                                            } else if (null != element_key
                                                    && element_key.equals("topics")) {
                                                if (null != element_item
                                                        && element_item.equals("topic")) {
                                                    //topic station
                                                    addStation(parser, station);
                                                    if (null != mCallback)
                                                        mCallback.messageCallback(eTUNEIN_MSG_STATION_TOPIC, station);
                                                } else if (null != element_type
                                                        && element_type.equals("link")) {
                                                    addLink(parser);
                                                } else {
                                                    Log.e(LogTag, "Un-procedure outline item !!");
                                                }
                                            } else {// no specific key
                                                if (null != element_type
                                                        && element_type.equals("audio")) {
                                                    if (null != element_item
                                                            && element_item.equals("station")) {
                                                        //audio station
                                                        addStation(parser, station);
                                                        if (null != mCallback)
                                                            mCallback.messageCallback(eTUNEIN_MSG_STATION_AUDIO, station);
                                                    } else if (null != element_item
                                                            && element_item.equals("show")) {
                                                        //show station
                                                        addStation(parser, station);
                                                        if (null != mCallback)
                                                            mCallback.messageCallback(eTUNEIN_MSG_STATION_SHOW, station);
                                                    } else if (null != element_guide_id
                                                            && is_station_id(element_guide_id)) {
                                                        //audio station
                                                        addStation(parser, station);
                                                        if (null != mCallback)
                                                            mCallback.messageCallback(eTUNEIN_MSG_STATION_AUDIO, station);
                                                    } else if (null != element_guide_id
                                                            && is_show_id(element_guide_id)) {
                                                        //show station
                                                        addStation(parser, station);
                                                        if (null != mCallback)
                                                            mCallback.messageCallback(eTUNEIN_MSG_STATION_SHOW, station);
                                                    } else if (null != element_guide_id
                                                            && is_topic_id(element_guide_id)) {
                                                        //topic station
                                                        addStation(parser, station);
                                                        if (null != mCallback)
                                                            mCallback.messageCallback(eTUNEIN_MSG_STATION_TOPIC, station);
                                                    } else {
                                                        Log.e(LogTag, "Un-procedure outline item !!");
                                                    }
                                                } else if (null != element_type
                                                        && element_type.equals("link")) {
                                                 addLink(parser);
                                                } else {
                                                    Log.e(LogTag, "Un-procedure outline item !!");
                                                }
                                            }
                                        }

                                    } else if (tag.equals("status")) {
                                        String str = parser.nextText();
                                        Log.i(LogTag, "xml response status " + str);
                                        if (null != mCallback)
                                            mCallback.messageCallback(eTUNEIN_MSG_STATUS_CODE
                                                    , str);
                                    } else if (tag.equals("title")) {
                                        String str = parser.nextText();
                                        Log.i(LogTag, "xml response title " + str);
                                        if (null != mCallback)
                                            mCallback.messageCallback(eTUNEIN_MSG_TITLE
                                                    , str);
                                    }
                                    break;

                                case XmlPullParser.END_TAG:
                                    break;
                            }

                            eventType = parser.next();
                        }
                        if (null != mCallback)
                            mCallback.messageCallback(eTUNEIN_MSG_XML_PARSER_END, null);
                        Log.i(LogTag, "Finish XML parse");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

                case TUNEIN_RESPONSE_TUNE:
                    Log.i(LogTag, "Tune : " + msg.obj.toString());
                    String str = msg.obj.toString();
                    List<String> list = Arrays.asList(str.split("\n"));
                    boolean get_info = false;
                    for (String s: list) {
                        Log.i(LogTag, "----- : " + s);
                        if (s.startsWith("#"))
                            continue;
                        if (s.contains(".pls")) {
                            sendRequestWithHttpClient(s, TUNEIN_RESPONSE_PLS);
                        } else if (s.contains(".m3u")){
                            sendRequestWithHttpClient(s, TUNEIN_RESPONSE_M3U);
                        } else {
                            if (null != mCallback)
                                mCallback.messageCallback(eTUNEIN_MSG_URL, s);
                        }
                        get_info = true;
                        break;
                    }
                    if (!get_info && null != mCallback)
                        mCallback.messageCallback(eTUNEIN_MSG_ERROR, "No response!!");
                    break;
                case TUNEIN_RESPONSE_PLS:
                    Log.i(LogTag, "PLS : " + msg.obj.toString());
                    String pls_str = msg.obj.toString();
                    List<String> pls_list = Arrays.asList(pls_str.split("\n"));
                    for (String s: pls_list) {
                        if (s.startsWith("File")) {
                            Log.i(LogTag, "-- : " + s.substring(s.indexOf("=")+1));
                            if (null != mCallback)
                                mCallback.messageCallback(eTUNEIN_MSG_URL, s.substring(s.indexOf("=")+1));
                            break;
                        }
                    }
                    break;
                case TUNEIN_RESPONSE_M3U:
                    Log.i(LogTag, "M3U : " + msg.obj.toString());
                    String m3u_str = msg.obj.toString();
                    List<String> m3u_list = Arrays.asList(m3u_str.split("\n"));
                    for (String s: m3u_list) {
                        Log.i(LogTag, "-- : " + s);
                        if (null != mCallback)
                            mCallback.messageCallback(eTUNEIN_MSG_URL, s);
                        break;
                    }
                    break;
                case TUNEIN_RESPONSE_URL:
                    if (null != msg.obj) {
                        Log.i(LogTag, "URL is " + msg.obj);
                        if (null != mCallback)
                            mCallback.messageCallback(eTUNEIN_MSG_URL, msg.obj);
                    }
                    break;
                case TUNEIN_RESPONSE_ERROR:
                    if (null != mCallback)
                        mCallback.messageCallback(eTUNEIN_MSG_ERROR, msg.obj);
                    break;
                default:
                    Log.e(LogTag, "Unsupport message type!!");
            }
        }
    };

    //Send request to ShoutCast server and get server response
    private void sendRequestWithHttpClient(final String url, final int responseType){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(LogTag, "URL:::" + url);
                HttpURLConnection httpConn = null;
                try {
                    URL con_url = new URL(url);
                    httpConn = (HttpURLConnection)con_url.openConnection();
                    httpConn.setRequestMethod("GET");
                    httpConn.setDoInput(true);
                    httpConn.setUseCaches(false);
                    httpConn.setRequestProperty("Connection", "Keep-Alive");
                    httpConn.setRequestProperty("User-Agent",
                            "Mozilla/5.0 (Windows; U; Windows NT 5.1; " + Locale.getDefault()
                                    + "; rv:1.9.0.3) Gecko/2008092417 Firefox/3.0.3");
                    String language = Locale.getDefault().getLanguage();
                    if (null != language && !language.isEmpty())
                        httpConn.setRequestProperty("Accept-Language", language);

                    //get response
                    int responseCode = httpConn.getResponseCode();
                    if(HttpURLConnection.HTTP_OK == responseCode){//Connection OK
                        StringBuffer sb = new StringBuffer();
                        String readLine;
                        BufferedReader responseReader;

                        responseReader = new BufferedReader(new InputStreamReader(httpConn.getInputStream(), Charset.forName("UTF-8")));
                        while ((readLine = responseReader.readLine()) != null) {
                            sb.append(readLine).append("\n");
//                            Log.e(LogTag, "RESPONSE:" + readLine);
                        }
                        responseReader.close();

                        Message message = Message.obtain();
                        message.what = responseType;
                        message.obj = sb;
                        handler.sendMessage(message);
                    } else {
                        Log.e(LogTag, "RESPONSE:" + responseCode);
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

    public TuneInRequest (String partner_id, String mSerial, String formats) {
        globleParam = new HashMap<String, String>();
        globleParam.clear();
        globleParam.put("partnerId", partner_id);
        globleParam.put("serial", mSerial);
        if (null != formats && !formats.isEmpty())
            globleParam.put("formats", formats);
    }

    public void setTuneInCallback(RadioRequestCallback callback) {
        mCallback = callback;
    }

//    public void setPartnerID(String partner) {
//        PartnerID = partner;
//        globleParam.put("partnerId", partner);
//    }
//
//    public void setSerial(String mSerial) {
//        serial = mSerial;
//        globleParam.put("serial", mSerial);
//    }

    private String getTuneInURL(String method, Map<String, String> param, boolean needSerial) {
        String url = BASE_URL;

        if (null == method || method.isEmpty()) {
            Log.e(LogTag, "Invalid TuneIn method!");
            return null;
        }
        url += method + ".ashx?";

        for (Map.Entry<String, String> entry:param.entrySet()) {
            url += entry.getKey() + "=" + entry.getValue() + "&";
        }

        for (Map.Entry<String, String> entry:globleParam.entrySet()) {
            if (!needSerial && entry.getKey().equals("serial"))
                continue;
            else if (entry.getKey().equals("formats") && param.containsKey("formats"))
                continue;//skip formats in globleParam
            else
                url += entry.getKey() + "=" + entry.getValue() + "&";
        }

        return url.substring(0, url.length()-1);//remove the last char '&'
    }

    private boolean is_category_id(String id) {
        if (null == id || id.isEmpty() || 'c' != id.charAt(0)
                || !TextUtils.isDigitsOnly(id.substring(1)))
            return false;
        return true;
    }

    private boolean is_folder_id(String id) {
        if (null == id || id.isEmpty() || 'f' != id.charAt(0)
                || !TextUtils.isDigitsOnly(id.substring(1)))
            return false;
        return true;
    }

    private boolean is_genre_id(String id) {
        if (null == id || id.isEmpty() || 'g' != id.charAt(0)
                || !TextUtils.isDigitsOnly(id.substring(1)))
            return false;
        return true;
    }

    private boolean is_artist_id(String id) {
        if (null == id || id.isEmpty() || 'm' != id.charAt(0)
                || !TextUtils.isDigitsOnly(id.substring(1)))
            return false;
        return true;
    }

    private boolean is_region_id(String id) {
        if (null == id || id.isEmpty() || 'r' != id.charAt(0)
                || !TextUtils.isDigitsOnly(id.substring(1)))
            return false;
        return true;
    }

    private boolean is_show_id(String id) {
        if (null == id || id.isEmpty() || 'p' != id.charAt(0)
                || !TextUtils.isDigitsOnly(id.substring(1)))
            return false;
        return true;
    }

    private boolean is_station_id(String id) {
        if (null == id || id.isEmpty() || 's' != id.charAt(0)
                || !TextUtils.isDigitsOnly(id.substring(1)))
            return false;
        return true;
    }

    private boolean is_topic_id(String id) {
        if (null == id || id.isEmpty() || 't' != id.charAt(0)
                || !TextUtils.isDigitsOnly(id.substring(1)))
            return false;
        return true;
    }

    private boolean is_custom_url_id(String id) {
        if (null == id || id.isEmpty() || 'u' != id.charAt(0)
                || !TextUtils.isDigitsOnly(id.substring(1)))
            return false;
        return true;
    }

    /**        Account Method        **/
    /**
     * Allows clients to create or work with RadioTime accounts. The action to take is specified by
     * the c command parameter. All calls to account methods must be made over HTTP/S. We strongly
     * recommend all parameters be POSTed to the service rather than specified in a query string.
     * Any account operation involving serial numbers (i.e., anonymous accounts) falls under our
     * reserved services. Please contact development@radiotime.com to enable the services for
     * your application.
     */

    /**
     * Description:
     *      Verifies credentials associated with a RadioTime account.
     */
    public int radioTimeAccountAuthenticate(String userName, String password) {
        Map<String, String> param = new HashMap<String, String>();

        param.put("c", "auth");

        if (null == userName) {
            Log.e(LogTag, "userName is invalid!!");
            return -1;
        }
        param.put("username", userName);
        param.put("password", password);
        return 0;
    }

    /**        Browse Method        **/
    /**
     * The browse method produces navigation and audio content from our radio directory. It covers
     * several different structures – presets, popular channels, locations – that are distinguished
     * by an input browse classifier.
     * All versions of the Browse method accept (and in some cases require) the global variables
     * for OPML.
     * Be sure to glance at our browse solution for general considerations.
     */

    /**
     * Description:
     *      When invoked without a classifier, the browse method returns a list of the available
     *  navigation structures. We strongly recommend you use this index as a “launch” point and
     *  follow the navigation links provided, rather than deep linking to an internal browse URL.
     *
     * Output:
     * 	<opml version="1">
     * 	    <head>
     * 	        <title>RadioTime</title>
     * 	        <status>200</status>
     * 	    </head>
     * 	    <body>
     * 	        <outline type="link" text="Local Radio" URL="http://opml.radiotime.com/Browse.ashx?c=local" key="local"/>
     * 	        <outline type="link" text="Talk"URL="http://opml.radiotime.com/Browse.ashx?c=talk" key="talk"/>
     * 	        <outline type="link" text="Sports" URL="http://opml.radiotime.com/Browse.ashx?c=sports" key="sports"/>
     * 	        <outline type="link" text="Music" URL="http://opml.radiotime.com/Browse.ashx?c=music" key="music"/>
     * 	        <outline type="link" text="By Location" URL="http://opml.radiotime.com/Browse.ashx?id=r0"/>
     * 	        <outline type="link" text="By Language" URL="http://opml.radiotime.com/Browse.ashx?c=lang" key="language"/>
     * 	        <outline type="link" text="Podcasts" URL="http://opml.radiotime.com/Browse.ashx?c=podcast" key="podcast"/>
     * 	    </body>
     * 	</opml>
     */
    public int radioTimeBrowseIndex(){
        String requestStr = getTuneInURL("Browse", new HashMap<String, String>(), true);
        sendRequestWithHttpClient(requestStr, TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Description:
     *      Creates a list of radio stations local to the caller, typically using IP geo-location.
     *
     * Input:
     * userName: When provided, stations are based on the location defined by the account.
     *      Account location settings are managed on radiotime.com
     * latlon: Abbreviate for Latitude and Longitude.
     *      When provided, stations are based on proximity to the geo-coordinate.
     *      If the coordinate is in the US, results will be similar to a zipcode search
     * formats: A comma-separated list of compatible stream formats.
     *      See the overview for more details. Hardware tuners can also use “am”, “fm”, or “hd”
     *
     * Output:
     *      A complete list of outline elements for stations in the discovered location.
     *  If the location maps to an area with sub-locations (like a country), the child elements
     *  will be links to those sub-locations.
     *
     * Notes:
     *      Neither the username nor the latlon parameters are necessary; when they are not given,
     *  the system will use the incoming IP address to provide results.
     *      If your application proxies client calls, you must forward the originating address in
     *  an HTTP header labeled “X-Forwarded-For”. See the local considerations for more detail.
     *      The latlon parameter has precedence over any others; if specified, it will be used
     *  ahead of any user-associated region or IP detection.
     *      Very few video streams are mapped to local regions; if you are browsing only for video,
     *  the response may not contain any channels.
     */
    public int radioTimeBrowseLocal(String userName, String latlon, String formats) {
        Map<String, String> param = new HashMap<String, String>();

        param.put("c", "local");
        if (null != latlon) {
            param.put("latlon", latlon);
        } else if (null != userName) {
            param.put("username", userName);
        }
        if (null != formats) {
            param.put("formats", formats);
        }

        sendRequestWithHttpClient(getTuneInURL("Browse", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Description:
     *      Either a valid RadioTime username or an authorized serial number is required.
     *  The service will offer either a list of items (if there is a single preset folder),
     *  or a list of folders. Please see the Preset API method for more information
     *  on managing presets.
     *
     * Input:
     * userName: Required if no serial; set to the account whose presets you wish to browse.
     * formats: A comma-separated list of compatible stream formats.
     *      See the overview for more details.
     *
     * Output:
     *      If the account has a single preset folder, results will be returned directly.
     *  Otherwise, you will receive navigable links for each folder.
     *
     * Notes:
     *      While presets are available by default, this call requires a valid partner ID,
     *  it cannot be invoked anonymously.
     *      A user may set a radio preset on RadioTime.com (or another device) that is not playable
     *  on your device. If this occurs, they will see the text “not supported” on
     *  your device’s presets.
     */
    public int radioTimeBrowsePresets(String userName, String formats) {
        Map<String, String> param = new HashMap<String, String>();
        boolean needSerial = false;

        param.put("c", "presets");
        if (null != userName) {
            param.put("username", userName);
        } else {
            needSerial = true;
        }
        if (null != formats) {
            param.put("formats", formats);
        }

        sendRequestWithHttpClient(getTuneInURL("Browse", param, needSerial), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Description:
     *      We organize our content into a few broad divisions. They are returned as part of
     *  the index menu but may also be addressed directly.
     *
     * Input:
     * categoey: Required, indicate to category you want to get, The following values are valid for
     *      the input parameter: "music", "talk", "sports", "world", "podcast", "popular", "best".
     *
     * Output:
     *      Navigable sub-categories for the given category
     *
     * Notes:
     *      The world channel is appropriate for finding a specific location by navigating
     *  countries, states, and cities. The popular channel produces a smart list of available
     *  stations relevant to your language and country. The podcast channel essentially traverses
     *  our genres, but displays only programs with on-demand or podcast content.
     *      Please keep in mind that all the global parameters apply to this call. The filter
     *  parameter can dramatically change the nature of results returned when browsing a category.
     *  For example, you can build a “jukebox” feature by setting a filter of random:
     *      Fetch a random available Electronic music station
     *  GET http://opml.radiotime.com/Browse.ashx?id=c57941&filter=s:random
     *          &partnerId=<partnerid>&serial=<serial>
     *      The links returned by this method are themselves deeper-level category browsing calls,
     *  which will contain groups of stations and shows. By default we return configured page sizes
     *  of results in these groups (10 for shows, 50 for stations). This may be customized.
     */
    public int radioTimeBrowseCategory(String category, String id, String filter
            , String offset, String pivot, String userName) {
        Map<String, String> param = new HashMap<String, String>();
        boolean needSerial = true;

        if (null != category && (category.equals("music") || category.equals("talk")
                || category.equals("sports") || category.equals("world")
                || category.equals("podcast") || category.equals("popular")
                || category.equals("best"))) {
            param.put("c", category);
        } else if (null != id && (is_category_id(id) || is_genre_id(id))) {
            param.put("id", id);
        } else {
            Log.e(LogTag, "Category type is Wrong!!");
            return -1;
        }

        if (null != filter) {
            param.put("filter", filter);
        }
        if (null != offset) {
            param.put("offset", offset);
        }
        if (null != pivot) {
            param.put("pivot", pivot);
        }
        if (null != userName) {
            param.put("username", userName);
            needSerial = false;
        }

        sendRequestWithHttpClient(getTuneInURL("Browse", param, needSerial), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Description:
     *      RadioTime offers radio content from around the world, which means we tag stations
     *  and shows in hundreds of languages. The browse language option offers an easy way to filter
     *  to a specific language.
     *
     * Input:
     * languageID: language id, RatioTime will return language menu when parameter is null.
     *
     * Output:
     *      Without the filter parameter, the service will return a list of languages available
     *  in the guide. From that point forward the experience is like a regular category browse,
     *  with content narrowed to stations and shows matching the language.
     */
    public int radioTimeBrowseLanguage(String languageID) {
        Map<String, String> param = new HashMap<String, String>();

        param.put("c", "lang");

        if (null != languageID)
            param.put("filter", languageID);

        sendRequestWithHttpClient(getTuneInURL("Browse", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Description:
     *      We maintain station recommendations for many of the stations in our guide.
     *  this method makes it possible to browse this content.
     *
     * Input:
     * stationId: Set to the guide ID of the station
     * detail: May be set to affiliate, genre, recommendation, or a combination of these three
     *      separated by commas (like “affiliate,genre”). When set, the service will return only
     *      the specified groups of content
     *
     * Output:
     *      If no detail parameter is provided, a list of stations similar to the given station,
     *  plus links for the station’s genre and affiliates. Otherwise, the specific requested groups
     *  of content.
     */
    public int radioTimeBrowseStation(String stationId, String detail) {
        Map<String, String> param = new HashMap<String, String>();

        if (null == stationId || !is_station_id(stationId)) {
            Log.e(LogTag, "stationId is invalid!!");
            return -1;
        }
        param.put("id", stationId);

        if (null != detail) {
            param.put("detail", detail);
        }

        sendRequestWithHttpClient(getTuneInURL("Browse", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Description:
     *      For those stations for which we maintain show schedules, you may browse a complete
     *  list for the current day or a specified date range.
     *
     * Input:
     * stationId: Set to the guide ID of the station
     * userName: A RadioTime account name; will affect the time zone of the response lineup
     * start: The start date for the lineup, in form yyyymmdd
     * stop: The end date for the lineup, in form yyyymmdd
     * forward: When set to true, the service will ignore the start and stop dates and instead
     *      provide a schedule looking forward over the next 24-36 hours.
     *      Also see the live parameter
     * live: When using the forward parameter, setting live to true will include the currently live
     *      show in the result. Otherwise the service will return the lineup starting with
     *      the show next on.
     * offset: The number of minutes the client timezone is offset from UTC
     * autodetect: When set to true, the service will attempt to determine the client timezone
     *
     * Output:
     *      A list of shows in chronological order, with start times in the user time zone (
     *  if a username is supplied), the timezone offset from UTC (is the offset parameter is
     *  supplied), or UTC. Each show will have an outline element like the following:
     *
     *  <outline type="link" text="Science in Action" URL="http://opml.radiotime.com/Browse.ashx?\
     *  c=topics&id=p440&title=Science+in+Action" guide_id="p440" start="2009-03-16T00:06:00" \
     *  duration="1440" image="http://radiotime-logos.s3.amazonaws.com/p440q.png" tz="Central"/>
     *
     *      The duration attribute is in seconds. The URL may be used to tune the show if it is
     *  currently broadcasting, or if it has previous topics available for listening. The tz
     *  attribute gives the descriptive name of the user time zone, if a username or offset is
     *  supplied.
     *
     *  Notes:
     *       retrieve a specific day, you may omit the stop date. The service currently limits
     *  each request to a maximum of 3 days.
     */
    public int radioTimeBrowseStationSchedules(String stationId, String userName, String start
            , String stop, boolean forward, boolean live, String offset, boolean autodetect) {
        Map<String, String> param = new HashMap<String, String>();
        boolean needSerial = true;

        param.put("c", "schedule");
        if(null == stationId || !is_station_id(stationId)) {
            Log.e(LogTag, "stationId is invalid!!");
            return -1;
        }
        param.put("id", stationId);
        if (null != userName) {
            param.put("username", userName);
            needSerial = false;
        }
        if (null != start) {
            param.put("start", start);
        }
        if (null != stop) {
            param.put("stop", stop);
        }
        if (forward) {
            param.put("forward", "true");
        }
        if (live) {
            param.put("live", "true");
        }
        if (null != offset) {
            param.put("offset", offset);
        }
        if (autodetect) {
            param.put("autodetect", "true");
        }
        sendRequestWithHttpClient(getTuneInURL("Browse", param, needSerial), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Description:
     *      For those stations for which we maintain song coverage, you may browse a list of songs
     *  played for the current day or a specified date range.
     *
     * Input:
     * stationId: Set to the guide ID of the station
     * userName: A RadioTime account name; will affect the time zone of the response lineup
     * start: The start date for the playlist, in form yyyymmdd.
     *      If not specified, the default is the current day.
     * stop: The end date for the playlist, in form yyyymmdd
     *
     * Output:
     *      A list of songs in chronological order, with start times in the detected time zone
     *  (account timezone if a username is supplied, otherwise the timezone from IP detection),
     *  and text and subtext set to the song title and artist, respectively.
     *
     *  <outline type="link" text="Tighten Up" URL="http://opml.radiotime.com/Browse.ashx?id=\
     *  m179923" guide_id="m179923" subtext="The Black Keys" start="2010-07-20T17:37:40" \
     *  tz="Central"/>
     *
     *      The outline elements are given as a link to browse content for the artist. The tz
     *  attribute gives the descriptive name of the time zone for which the start time applies.
     *
     * Notes:
     *      To retrieve a specific day, you may omit the stop date. The service currently limits
     *  each request to a maximum of 3 days.
     */
    public int radioTimeBrowseStationPlaylist(String stationId, String userName
            , String start, String stop) {
        Map<String, String> param = new HashMap<String, String>();

        param.put("c", "playlist");
        if (null == stationId || !is_station_id(stationId)) {
            Log.e(LogTag, "stationId is invalid!!");
            return -1;
        }
        param.put("id", stationId);
        if (null != userName) {
            param.put("username", userName);
        }
        if (null != start) {
            param.put("start", start);
        }
        if (null != stop) {
            param.put("stop", stop);
        }

        sendRequestWithHttpClient(getTuneInURL("Browse", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Description:
     *      This method offers browsing to related affiliate networks and genres for a
     *  given radio show.
     *
     * Input:
     * stationId: Set to the guide ID of the radio show.
     *
     * Output:
     *      By default this method returns applicable genres and affiliate networks associated
     *  with the show.
     */
    public int radioTimeBrowseShow(String stationId) {
        Map<String, String> param = new HashMap<String, String>();

        if (null == stationId || !is_station_id(stationId)) {
            Log.e(LogTag, "stationId is invalid!!");
            return -1;
        }
        param.put("id", stationId);

        sendRequestWithHttpClient(getTuneInURL("Browse", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**        Config Method        **/
    /**
     * Offers several client and server side configuration services
     */

    /**
     * Description:
     *      Retrieves the current server time and details of the client detected timezone.
     *  Client timezone detection is based on IP geolocation, unless a latlon parameter is
     *  specified or a RadioTime account name is passed.
     *
     * Output:
     *
     * <outline text="time" utc_time="1265925455" server_time="1265903855" server_tz="Central" \
     * server_offset="-360" detected_time="1265903855" detected_tz="Central" detected_offset="-360"/>
     *
     * utc_time: The current time in UTC, expressed as seconds since the epoch (Unix time format)
     * server_time: The current time in the RadioTime server’s time zone, as seconds since the epoch
     * server_tz: The name of the RadioTime server’s time zone
     * server_offset: The offset, in minutes, of the server’s time zone from UTC
     * detected_time: The current time in the detected time zone of the client, as seconds since the epoch
     * detected_tz: The name of the detected time zone
     * detected_offset: The offset, in minutes, of the detected time zone from UTC
     */
    public int radioTimeConfigTime() {
        Map<String, String> param = new HashMap<String, String>();
        param.put("c", "time");

        sendRequestWithHttpClient(getTuneInURL("Config", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Description:
     *      This is a reserved service that allows an application to retrieve text resources
     *  in a particular locale.
     *
     * Output:
     * <outline text="content" key="settings" value="Settings"/>
     * <outline text="content" key="connecting" value="Connecting..."/>
     *
     * key: The name of the resource
     * value: The localized value of the resource
     */
    public int radioTimeConfigLocalizedStrings() {
        Map<String, String> param = new HashMap<String, String>();
        param.put("c", "contentQuery");

        sendRequestWithHttpClient(getTuneInURL("Config", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Description:
     *      Retrieves a list of streams using the various protocols, playlists, and codecs for
     *  player development and testing.
     *
     * Output:
     * <outline type="audio" text="Managed: MP3|HTTP|M3U" URL="http://www.motor.de/extern/motorfm/stream/motorfm.mp3"/>
     * <outline type="audio" text="Managed: AAC|ICY|M3U" URL="http://etn.fm/playlists/etn2-aac-high.m3u"/>
     */
    public int radioTimeConfigStreamSample() {
        Map<String, String> param = new HashMap<String, String>();
        param.put("c", "streamSampleQuery");

        sendRequestWithHttpClient(getTuneInURL("Config", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**        Describe Method        **/
    /**
     * The Describe method offers detailed information about an item in the radio directory in a
     * non-navigable form. This may be useful in constructing a richer user interface after an audio
     * item such as a station or show is reached in navigation. Callers may request alternate
     * metadata by specifying the query string parameter c. The following metadata are currently
     * available*/

    /**
     * Description:
     *      This method returns 2 or more text lines describing the content currently broadcast on
     *  a station or stream. The service does not support frequent or long polling to enable
     *  client-side display refresh. Please look for and use the cache-control HTTP response header
     *  to determine when to make the next call. Cache-Control: private, max-age=5964
     *
     * Input:
     * id: Set to the guide ID for which you need information; you can gather this from any previous
     *      outline element’s guide_id attribute
     *
     * Output:
     *      Currently we provide a descriptive station name, show title, and show genre for
     *  scheduled programming
     * <opml version="1">
     *     <head>
     *         <status>200</status>
     *     </head>
     *     <body>
     *         <outline type="text" text="KERA 90.1" image="http://radiotime-logos.s3.amazonaws.com/p38386q.png" preset_id="s32500"/>
     *         <outline type="text" text="Day to Day"/>
     *         <outline type="text" text="Magazine"/>
     *     </body>
     * </opml>
     *
     * Notes:
     *      If the station has song coverage in our guide, one of the text elements will be the name
     *  and artist associated with the song.
     */
    public int radioTimeDescribeNowPlaying(String id) {
        Map<String, String> param = new HashMap<String, String>();

        param.put("c", "nowplaying");
        if (null == id || !is_station_id(id)) {
            Log.e(LogTag, "stationId is invalid!!");
            return -1;
        }
        param.put("id", id);

        sendRequestWithHttpClient(getTuneInURL("Describe", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Description:
     *      Given only a station ID, the service will return detailed information about the
     *  corresponding station. The types of detail may be specified as input.
     *
     * Input:
     * id: Set to the guide ID for which you need information; you can gather this from any previous
     *      outline element’s guide_id attribute
     * detail; A comma-separated list of values indicating additional detail to retrieve. The
     *      allowable options are affiliate,genre, and recommendation, or a comma-separated
     *      combination of the three. This parameter is not required
     *
     * Output:
     *      Will return a single outline element of type object, containing a station. If the detail
     *  parameter is specified, the response will also contain the requested groups.
     */
    public int radioTimeDescribeStation(String id, String detail) {
        Map<String, String> param = new HashMap<String, String>();

        if (null == id || !is_station_id(id)) {
            Log.e(LogTag, "stationId is invalid!!");
            return -1;
        }
        param.put("id", id);

        if (null != detail)
            param.put("detail", detail);

        sendRequestWithHttpClient(getTuneInURL("Describe", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Description:
     *      Given only a show ID, the service will return detailed information about the
     *  corresponding show.
     *
     * Input:
     * id: Set to the guide ID for which you need information; you can gather this from any previous
     *      outline element’s guide_id attribute
     * detail: A comma-separated list of values indicating additional detail to retrieve. The
     *      allowable options are affiliate,genre, and recommendation, or a comma-separated
     *      combination of the three. This parameter is not required
     *
     * Output:
     *      Will return a single outline element of type object, containing a show. If the detail
     *  parameter is specified, the response will also contain the requested groups.
     */
    public int radioTimeDescribeShow(String id, String detail) {
        Map<String, String> param = new HashMap<String, String>();

        if (null == id || !is_show_id(id)) {
            Log.e(LogTag, "ID is invalid!!");
            return -1;
        }
        param.put("id", id);

        if (null != detail)
            param.put("detail", detail);

        sendRequestWithHttpClient(getTuneInURL("Describe", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Description:
     *      Retrieves metadata for a single radio show topic.
     *
     * Input:
     * id: Set to the guide ID for which you need information; you can gather this from a previous
     *      outline element’s guide_id attribute
     *
     * Output:
     *      Will return a single outline element of type object, containing a topic.
     */
    public int radioTimeDescribeTopic(String id) {
        Map<String, String> param = new HashMap<String, String>();

        if (null == id || !is_topic_id(id)) {
            Log.e(LogTag, "ID is invalid!!");
            return -1;
        }
        param.put("id", id);

        sendRequestWithHttpClient(getTuneInURL("Describe", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Description:
     *      Retrieves a list of all countries known to the RadioTime directory.
     *
     * Output:
     *      A list of outline elements whose guide_id attributes may be used in Search, Browse,
     *  and Account calls
     */
    public int radioTimeDescribeCountries(){
        Map<String, String> param = new HashMap<String, String>();

        param.put("c", "countries");

        sendRequestWithHttpClient(getTuneInURL("Describe", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Description:
     *      Retrieves a list of all languages broadcast by stations in the RadioTime directory. This
     *  is not the same as the languages in which we have content translated. For that, see the
     *  locales list below.
     *
     * Output:
     *      A list of outline elements whose guide_id attributes may be used as a language filter in
     *  Search and Browse calls. The language names will be localized based on the request.
     */
    public int radioTimeDescribeLanuages(){
        Map<String, String> param = new HashMap<String, String>();

        param.put("c", "languages");

        sendRequestWithHttpClient(getTuneInURL("Describe", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Description:
     *      Retrieves a list of all locales supported by the API. These are values appropriate for
     *  use with the locale query string parameter or the HTTP Accept-Language header.
     *
     * Output:
     *      The complete list of supported service locales. Look to the guide_id attribute for
     *  correct values.
     */
    public int radioTimeDescribeLocales(){
        Map<String, String> param = new HashMap<String, String>();

        param.put("c", "locales");

        sendRequestWithHttpClient(getTuneInURL("Describe", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Description:
     *      Retrieves a list of the media formats recognized by the API. These are values
     *  appropriate for use with the formats query string parameter.
     *
     * Output:
     *      The complete list of supported service formats. Look to the guide_id attribute for
     *  correct values.*/
    public int radioTimeDescribeFormats() {
        Map<String, String> param = new HashMap<String, String>();

        param.put("c", "formats");

        sendRequestWithHttpClient(getTuneInURL("Describe", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Description:
     *      Retrieves a list of all genres tagged in the RadioTime directory.
     *
     * Output:
     *      A list of outline elements whose guide_id attributes may be used in Search, Browse, and
     *  Account calls. The genre name will be localized based on the request.
     */
    public int radioTimeDescribeGenres() {
        Map<String, String> param = new HashMap<String, String>();

        param.put("c", "genres");

        sendRequestWithHttpClient(getTuneInURL("Describe", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**        Option Method        **/

    /**
     * Description:
     *      This method offers a context menu (such as might be displayed on right click in a
     *  desktop application, or “more options” in a navigable device) for a specific item.
     *
     * Input:
     * id: he guide ID of a single station or show
     *
     * Output:
     *      A list of outline elements corresponding to actions and additional content available for
     *  the item. Currently this consists of a problem reporting wizard, a now playing display,
     *  recommendations, and stream selection.
     */
    public int radioTimeOptions(String id) {
        Map<String, String> param = new HashMap<String, String>();

        if (null == id || (!is_station_id(id) && !is_show_id(id))) {
            Log.e(LogTag, "ID is invalid!!");
            return -1;
        }
        param.put("id", id);

        sendRequestWithHttpClient(getTuneInURL("Options", param, true), TUNEIN_RESPONSE_XML);
        return 0;

    }

    /**        Preset Method        **/
    /**
     * Description:
     *      Adds or removes a single preset, adds or removes a preset folder, or lists preset
     *  folders, from a named or anonymous RadioTime account. This call should be made over HTTP/S
     *  to protect the identity information. To browse presets, use the Browse Presets command.
     *  Presets are covered more completely in the RadioTime concepts section of the developer’s
     *  guide. Anonymous accounts are explained here
     *
     * Input:
     * userName: Required if no deviceSerial; names the RadioTime account
     * password: Required if username specified
     * folderId: Required for removeFolder or renameFolder, optional for add/remove; the guide ID
     *          of a specific preset folder in which to add or remove content, or the folder to
     *          remove or rename
     * id: Required for add/remove if no URL; set to the station, show, or url ID to add/remove as
     *      a preset url Required for add/remove if no ID; a string URL to save as a preset
     * presetNumber: Optional; the position into which the preset should be saved
     * name: Required for addFolder or renameFolder; the name to use for the folder
     *
     * Output:
     *      On folder creation, an outline element with the folder’s guide ID will be returned. On
     *  listFolders, will give a set of text outline elements with the guide ID of each folder.
     *      For all other actions, this call returns a simple status code. Check the response for
     *  fault details if not successful.
     *
     * Notes:
     *      The Preset method is either a folder operation (addFolder, removeFolder, renameFolder)
     *  or an item operation (add/remove). All RadioTime accounts have a default folder which will
     *  be used in the absence of a specific folder ID.
     *      To remove a preset URL, you will need to supply its guide ID. This is the value returned
     *  in the guide_id attribute of the outline element in the preset browse call.
     *      Currently, it is only possible to add presets that have either station or show set as
     *  their item attribute.
     *      If you overwrite a presetNumber, this action will shift the existing presets over.
     *  If you want to replace a preset, delete the existing preset in the occupied slot first.
     *      If you set a new presetNumber for a station that is already in your presets, this action
     *  should assign the new presetNumber to the existing preset.
     *      We describe the serial and username/account distinction more completely in the OPML
     *  security model section.
     *      Station and show presets may not be immediately visible to users on radiotime.com due
     *  to data propagation times; these vary but should be no longer than a few minutes.
     */

    /**
     * Adds the station KERA to a user's default preset folder
     *
     * GET http://opml.radiotime.com/Preset.ashx?c=add&id=s32500&partnerId=<partnerid>&username=<username>&password=<password>
     *
     * Adds the station KERA to a specific preset folder for an anonymous device account
     *
     * GET http://opml.radiotime.com/Preset.ashx?c=add&id=s32500&folderId=f123456&partnerId=<partnerid>&serial=<serial>
     */
    public int radioTimePresetAdd(String userName, String password, String folderId, String id
            , String url, String presetNumber, String name) {
        Map<String, String> param = new HashMap<String, String>();
        boolean needSerial = true;

        param.put("c", "add");

        if (null == id || (!is_station_id(id) && !is_show_id(id))) {
            Log.e(LogTag, "ID is invalid!!");
            return -1;
        }

        if (null != userName) {
            needSerial = false;
            param.put("username", userName);
            param.put("password", password);
        }
        if (null != folderId && is_folder_id(folderId)) {
            param.put("folderId", folderId);
        }

        param.put("id", id);

        if (null != url) {
            param.put("url", url);
        }
        if (null != presetNumber) {
            param.put("presetNumber", presetNumber);
        }
        if (null != name) {
            param.put("name", name.replaceAll("\\s", "+"));
        }

        sendRequestWithHttpClient(getTuneInURL("Preset", param, needSerial), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Removes the show Fresh Air from a device account's default folder
     *
     * GET http://opml.radiotime.com/Preset.ashx?c=remove&id=p17&partnerId=<partnerid>&serial=<serial>
     */
    public int radioTimePresetRemove(String userName, String password, String folderId, String id
            , String url, String presetNumber){
        Map<String, String> param = new HashMap<String, String>();
        boolean needSerial = true;

        param.put("c", "remove");

        if (null == id || (!is_station_id(id) && !is_show_id(id))) {
            Log.e(LogTag, "ID is invalid!!");
            return -1;
        }

        if (null != userName) {
            needSerial = false;
            param.put("username", userName);
            param.put("password", password);
        }
        if (null != folderId && is_folder_id(folderId)) {
            param.put("folderId", folderId);
        }

        param.put("id", id);

        if (null != url) {
            param.put("url", url);
        }
        if (null != presetNumber) {
            param.put("presetNumber", presetNumber);
        }

        sendRequestWithHttpClient(getTuneInURL("Preset", param, needSerial), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Adds a new preset folder to a named account
     *
     * GET http://opml.radiotime.com/Preset.ashx?c=addFolder&name=Rock+Stations&partnerId=<partnerid>&username=<username>&password=<password>
     */
    public int radioTimePresetAddFolder(String userName, String password, String name) {
        Map<String, String> param = new HashMap<String, String>();
        boolean needSerial = true;

        param.put("c", "addFolder");

        if (null == name ) {
            Log.e(LogTag, "name is invalid!!");
            return -1;
        }

        if (null != userName) {
            needSerial = false;
            param.put("username", userName);
            param.put("password", password);
        }
        param.put("name", name.replaceAll("\\s", "+"));

        sendRequestWithHttpClient(getTuneInURL("Preset", param, needSerial), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Remove folder
     */
    public int radioTimePresetRemoveFolder(String userName, String password
            , String folderId) {
        Map<String, String> param = new HashMap<String, String>();
        boolean needSerial = true;

        param.put("c", "removeFolder");

        if (null == folderId || !is_folder_id(folderId)) {
            Log.e(LogTag, "folderId is invalid!!");
            return -1;
        }

        if (null != userName) {
            needSerial = false;
            param.put("username", userName);
            param.put("password", password);
        }
        param.put("folderId", folderId);

        sendRequestWithHttpClient(getTuneInURL("Preset", param, needSerial), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Rename folder
     */
    public int radioTimePresetRenameFolder(String userName, String password
            , String folderId, String name) {
        Map<String, String> param = new HashMap<String, String>();
        boolean needSerial = true;

        param.put("c", "renameFolder");

        if (null == folderId || null == name || !is_folder_id(folderId)) {
            Log.e(LogTag, "folderId or name is invalid!!");
            return -1;
        }

        if (null != userName) {
            needSerial = false;
            param.put("username", userName);
            param.put("password", password);
        }
        param.put("folderId", folderId);
        param.put("name", name.replaceAll("\\s", "+"));

        sendRequestWithHttpClient(getTuneInURL("Preset", param, needSerial), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Lists all folders for a named account
     *
     * GET http://opml.radiotime.com/Preset.ashx?c=listFolders&partnerId=<partnerid>&username=<username>&password=<password>
     */
    public int radioTimePresetListFolder(String userName, String password) {
        Map<String, String> param = new HashMap<String, String>();
        boolean needSerial = true;

        param.put("c", "listFolders");

        if (null != userName) {
            needSerial = false;
            param.put("username", userName);
            param.put("password", password);
        }

        sendRequestWithHttpClient(getTuneInURL("Preset", param, needSerial), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**        Recommend Method        **/
    /**
     * Finds recommended stations based on a list of artists
     *
     * Input:
     *
     * GET http://opml.radiotime.com/Recommend.ashx?a[0]=Beatles&a[1]=The+Who&partnerId=<partnerid>&serial=<serial>
     *
     * a[0]: Up to 10 artists may be supplied, as parameters a[0] through a[10]. required!!
     * r[0]: If certain artists are more important than others, specify the rating as an integer between 1 and 100
     *
     * Output:
     *      A list of outline elements corresponding to each available, recommended station.
     *  They will be ordered by strength of the match
     */
    public int radioTimeRecommendStationByArtist(String artist, String recommend) {
        Map<String, String> param = new HashMap<String, String>();

        if (null == artist) {
            Log.e(LogTag, "artist is invalid!!");
            return -1;
        }

        String[] artistList = artist.split(",");
        for (int i = 0; i < artistList.length; i++) {
            param.put("a[" + i + "]", artistList[i].replaceAll("\\s", "+"));
        }

        if (null != recommend) {
            String[] r = recommend.split(",");
            for (int i = 0; i < r.length; i++) {
                param.put("r[" + i + "]", r[i]);
            }
        }

        sendRequestWithHttpClient(getTuneInURL("Recommend", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**        Report Method        **/
    /**
     * The report method offers a mechanism to send information about the guide back to RadioTime.
     * In addition to the described parameters, the global parameters also provide valuable detail
     * for troubleshooting.
     */

    /**
     * Description:
     *      An easy way to narrow informational or listening experience problems is to use our
     *  problem wizard. It is ideal for point/click or browse/select environments in which typing
     *  is unavailable or difficult. Often, users will get to a specific cause in less than three
     *  choices. The final option sends a ticket into our CRM for moderator review.
     *
     * GET http://opml.radiotime.com/Report.ashx?c=wizard&id=s32500&partnerId=<partnerid>&serial=<serial>
     *
     * Input:
     * id: The guide ID of the item that triggered the report
     * email: An email address for the user reporting the problem
     *
     * Output:
     *      Each call to the wizard generates a menu of narrower options until the end of a
     *  particular trail is reached, at which we point we provide a “thank you” message and create
     *  a ticket.
     *      The wizard also includes the option to change a station’s stream, as well as change the
     *  a player to a recommended station.
     *      Terminal elements (end of a wizard path) will be marked with an attribute nav set to the
     *  value stop. If your interface allows easy textual feedback, you may wish to provide the user
     *  a form to provide more detail, and send it to the feedback method.
     */
    public int radioTimeReportWizard(String id, String email) {
        Map<String, String> param = new HashMap<String, String>();

        param.put("c", "wizard");

        if (null == id || (!is_station_id(id) && !is_show_id(id))) {
            Log.e(LogTag, "id is invalid!!");
            return -1;
        }
        param.put("id", id);

        if (null != email) {
            param.put("email", email);
        }

        sendRequestWithHttpClient(getTuneInURL("Report", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Dewcription:
     *      Accepts free text comments/concerns/complaints about a specific show or station.
     *
     * Input:
     * id: The guide ID of the item that triggered the report
     * email: An email address for the user reporting the problem
     * text: The feedback text
     *
     * Output:
     *      None; check the status code in the document header for success or failure.
     *      200     Feedback accepted
     *      400     Invalid input – check the for details
     *      500     Feedback rejected – check the for details
     *
     * Notes:
     *      The example shows an HTTP GET request, but for long textual feedback we recommend POST
     *  instead. The parameter names should be the same.
     */
    public int radioTimeReportFeedback(String id, String email, String text) {
        Map<String, String> param = new HashMap<String, String>();

        param.put("c", "feedback");

        if (null == id || (!is_station_id(id) && !is_show_id(id))) {
            Log.e(LogTag, "id is invalid!!");
            return -1;
        }
        param.put("id", id);

        if (null != email) {
            param.put("email", email);
        }

        if (null != text) {
            param.put("text", text.replaceAll("\\s", "+"));
        }

        sendRequestWithHttpClient(getTuneInURL("Report", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Description:
     *      Accepts pass/fail and error information specific to a stream play attempt.
     *
     * Input:
     * id: This would be the id passed to Tune.ashx to get streams. Yes, if no URL provided
     * streamUrl: The stream URL returned by Tune.ashx that failed playback. Yes, if no id provided.
     *      Please send in addition to id if possible
     * error: Your numeric application error code
     * message: The textual error or playback message
     *
     * Output:
     *      None; check the status code in the document header for success or failure.
     *
     * Notes:
     *      In short, to report a successful stream status, omit the error parameter, otherwise
     *  provide it with the relevant detail from your player. Additionally, don’t forget the global
     *  parameters. The formats and partnerId parameters are particularly important.
     *      The parameter streamUrl should be the top level stream returned by Tune.ashx. Ex:
     *  Tune.ashx returns a M3U playlist containing stream ‘A’ and stream ‘B’. Stream ‘A’ is a mp3
     *  stream and stream ‘B’ is a pls playlist containing Stream ‘C’. If stream ‘A’ fails set the
     *  streamUrl parameter to ‘A’. If stream ‘C’ fails set the streamUrl parameter to ‘B’.
     *      The error code you send depends on your system setup and the player you are using.
     *  There is no standard list of errors. If you can’t distinguish between the errors, please
     *  send over the same error number (as long as it is not zero).
     */

    public int radioTimeReportStream(String id, String streamUrl
            , String error, String message) {
        Map<String, String> param = new HashMap<String, String>();

        param.put("c", "stream");

        if (null != id && (is_station_id(id) || is_show_id(id))) {
            param.put("id", id);
        }
        if (null != streamUrl) {
            param.put("streamUrl", streamUrl);
        }
        if (null != error) {
            param.put("error", error);
        }
        if (null != message) {
            param.put("message", message.replaceAll("\\s", "+"));
        }

        sendRequestWithHttpClient(getTuneInURL("Report", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**        Search Method        **/

    /**
     * Description:
     *      Offers free-text searching for stations, shows, topics, songs and artists, ]
     *  and stream URLs.
     *
     * Input:
     *  query: The query text to search. required
     *  filter: If off, will return a list of unsupported items. values:standard, off.
     *  types: If set to one of these values, will limit search results to only the specified type.
     *      values:station, show.
     *  call: If set, will force a match against station call sign. Limits the results to stations.
     *  name: If set, will force a match against station or show name.
     *  freq: If set, will force a match against station frequency. Limits the results to stations.
     *
     * Output:
     *      A list of outline elements corresponding to each station or show matching the query.
     *  They will be ordered by relevance to the term.
     *      If the term matches a browsable category, like “jazz” or “canada”, the response will
     *  contain a link to the category, in addition to the stations and shows matching the term.
     *      If the filter parameter is off and results are found but are not playable, an additional
     *  “Search results (not playable)” container will exist with the list of items not available.
     *  Otherwise, unplayable results will only appear if the total number of search results is
     *  less than 25.
     *
     * Notes:
     *      Unless the filter parameter is set to off, the response will contain only playable
     *  content.
     *      Additional search fields like call allow an advanced filter and may be used
     *  independently or in conjunction with query. Unless you specifically need this filtering
     *  to narrow a broad search, it’s better to place your query terms only in query, which will
     *  match against all fields.
     */
    public int radioTimeSearch(String query, String filter, String types
            , String call, String name, String freq) {
        Map<String, String> param = new HashMap<String, String>();

        if (null == query) {
            Log.e(LogTag, "query is invalid!!");
            return -1;
        }
        param.put("query", query.replaceAll("\\s", "+"));
        if (null != filter) {
            if (filter.equals("standard"))
                param.put("filter", "standard");
            else
                param.put("filter", "off");
        }
        if (null != types) {
            if (types.equals("station"))
                param.put("types", "station");
            else if (types.equals("show"))
                param.put("types", "show");
            else {
                Log.e(LogTag, "types is invalid!!");
                return -1;
            }
        }
        if (null != call) {
            param.put("call", call);
        }
        if (null != name) {
            param.put("name", name.replaceAll("\\s", "+"));
        }
        if (null != freq) {
            param.put("freq", freq);
        }

        sendRequestWithHttpClient(getTuneInURL("Search", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Description:
     *      To find stations currently broadcasting a specific song or artist.
     * GET http://opml.radiotime.com/Search.ashx?c=artist&query=gaga&partnerId=<id>&serial=<serial>
     *
     * Input:
     *  c: Set to song, artist, or song,artist to search both
     *  query: The song or artist name (does not need to be complete)
     *
     * Output:
     *      Any stations currently matching the song/artist and are available will be returned.
     *  Song coverage is only available for about 3000 stations in the United States,
     *  so results may vary.
     */
    public int radioTimeSearchArtists(String query) {
        Map<String, String> param = new HashMap<String, String>();

        param.put("c", "artist");

        if (null == query) {
            Log.e(LogTag, "query is invalid!!");
            return -1;
        }
        param.put("query", query.replaceAll("\\s", "+"));

        sendRequestWithHttpClient(getTuneInURL("Search", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    public int radioTimeSearchSongs(String query) {
        Map<String, String> param = new HashMap<String, String>();

        param.put("c", "song");

        if (null == query) {
            Log.e(LogTag, "query is invalid!!");
            return -1;
        }
        param.put("query", query.replaceAll("\\s", "+"));

        sendRequestWithHttpClient(getTuneInURL("Search", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    public int radioTimeSearchArtistSongs(String query) {
        Map<String, String> param = new HashMap<String, String>();

        param.put("c", "artist,song");

        if (null == query) {
            Log.e(LogTag, "query is invalid!!");
            return -1;
        }
        param.put("query", query.replaceAll("\\s", "+"));

        sendRequestWithHttpClient(getTuneInURL("Search", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**
     * Description:
     *      If your application allows users to enter their own streams, you can use our search
     *  method to “reverse-engineer” the station it belongs to.
     *
     * GET http://opml.radiotime.com/Search.ashx?c=stream&query=http://mystream.com/stream.asx&partnerId=<id>&serial=<serial>
     *
     * Input:
     *  query: A stream URL to find
     *
     * Output:
     *      f the stream is found in our guide, we will return the corresponding station as an
     *  outline element.
     *
     * Notes:
     *      The stream URL can omit the scheme
     *  (i.e., www.stream.com instead of http://www.stream.com) and it will be assumed “http”.
     */
    public int radioTimeSearchStreams(String query) {
        Map<String, String> param = new HashMap<String, String>();

        param.put("c", "stream");

        if (null == query) {
            Log.e(LogTag, "query is invalid!!");
            return -1;
        }
        param.put("query", query);

        sendRequestWithHttpClient(getTuneInURL("Search", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    /**        Tune Method        **/

    /**
     * Description:
     *      All content links that contain stations ultimately point to a station tune URL. This
     *  method creates a playlist from streams associated with the station.
     *
     * Input:
     * id: The guide ID for the station to tune
     * ebrowse: If set to true, will return individual tune links for streams. Otherwise will tune
     *      normally. Please see Notes regarding ebrowse before use.
     *
     * Output:
     *      Unless the c parameter is set, this method will return a media playlist, usually of
     *  type M3U. In other words, it will not be an OPML document.
     *      If the c parameter is set for a stream browse, you will receive individual OPML links
     *  for streams.
     *
     * Notes:
     *      Only use ebrowse in order to present the user with a list of tune links to choose from
     *  (e.g., a “Select Another Stream” button) after they have already Tuned a station. In all
     *  other cases, do not use ebrowse, as the link list is not sorted optimally for the listener.
     */
    public int radioTimeTuneStation(String id, boolean ebrowse){
        Map<String, String> param = new HashMap<String, String>();

        if (null == id || (!is_station_id(id) && !is_topic_id(id))) {
            Log.e(LogTag, "id is invalid!!");
            return -1;
        }
        param.put("id", id);
        if (ebrowse) {
            param.put("c", "ebrowse");
        }

        sendRequestWithHttpClient(getTuneInURL("Tune", param, true), TUNEIN_RESPONSE_TUNE);
        return 0;
    }

    /**
     * Description:
     *      Shows may air on multiple stations or have multiple episodes available for download on
     *  demand. The tuning call provides links to all available options.
     *
     * Input:
     * c: If set to pbrowse, lists recent episodes of a show along with a listen now link.
     *      If there are no recent episodes, a list of available stations will result. If set to
     *      sbrowse , lists available stations for a show, grouped by time remaining.
     * id: The guide ID of the show.
     * flatten: If set to true, the service will return a flat list of stations rather than
     *      time groups.
     *
     * Output:
     *      A set of outline groups containing individual stations currently airing the program,
     *  as well as a group containing recent topics, if available.
     */
    public int radioTimeTuneShow(String c, String id, boolean flatten) {
        Map<String, String> param = new HashMap<String, String>();

        if (null == id || !is_show_id(id)) {
            Log.e(LogTag, "id is invalid!!");
            return -1;
        }
        if (null == c || !c.equals("pbrowse") || !c.equals("sbrowse")) {
            Log.e(LogTag, "c is invalid!!");
            return -1;
        }
        param.put("id", id);
        param.put("c", c);
        if (flatten) {
            param.put("flatten", "true");
        }

        sendRequestWithHttpClient(getTuneInURL("Tune", param, true), TUNEIN_RESPONSE_XML);
        return 0;
    }

    public int radioTimeGetUrl(String url) {
        sendRequestWithHttpClient(url, TUNEIN_RESPONSE_XML);
        return 0;
    }
}
