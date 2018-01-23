package aca.com.remote.tunes.util;

/**
 * Created by jim.yu on 2017/11/24.
 */

public class TuneInElement {
    String type;
    String text;
    String url;
    int bitrate;
    int reliability;
    String guide_id;
    String subtext;
    String genre_id;
    String format;
    String show_id;
    String item;

    String image;

    String current_track;

    String playing;
    String playing_image;
    String now_playing_id;
    String preset_id;
    boolean subscription_required;

    public void  setType(String m_type) {
        type = m_type;
    }

    public String getType() {
        return type;
    }

    public void setText(String m_text) {
        text = m_text;
    }

    public String getText() {
        return text;
    }

    public void setUrl(String m_url) {
        url = m_url;
    }

    public String getUrl() {
        return url;
    }

    public void setBitrate(int m_bitrate) {
        bitrate = m_bitrate;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setReliability(int m_reliability) {
        reliability = m_reliability;
    }

    public int getReliability() {
        return reliability;
    }

    public void setGuide_id(String id) {
        guide_id = id;
    }

    public String getGuide_id() {
        return guide_id;
    }

    public void setSubtext(String text) {
        subtext = text;
    }

    public String getSubtext() {
        return subtext;
    }

    public void setGenre_id(String genre) {
        genre_id = genre;
    }

    public String getGenre_id() {
        return genre_id;
    }

    public void setFormat(String m_format) {
        format = m_format;
    }

    public String getFormat() {
        return format;
    }

    public void setShow_id(String id) {
        show_id = id;
    }

    public String getShow_id() {
        return show_id;
    }

    public void setItem(String v) {
        item = v;
    }

    public String getItem() {
        return item;
    }

    public void setImage(String url) {
        image = url;
    }

    public String getImage() {
        return image;
    }

    public void setCurrent_track(String track) {
        current_track = track;
    }

    public String getCurrent_track() {
        return current_track;
    }

    public void setNow_playing_id(String id) {
        now_playing_id = id;
    }

    public String getNow_playing_id() {
        return now_playing_id;
    }

    public void setPreset_id(String id) {
        preset_id = id;
    }

    public String getPreset_id() {
        return preset_id;
    }

    public void setPlaying(String play) {
        playing = play;
    }

    public String  getPlaying() {
        return playing;
    }

    public void setPlaying_image(String url) {
        playing_image = url;
    }

    public String getPlaying_image() {
        return playing_image;
    }

    public void setSubscription_required(boolean b) {
        subscription_required = b;
    }

    public boolean getSubscription_required() {
        return subscription_required;
    }
}
