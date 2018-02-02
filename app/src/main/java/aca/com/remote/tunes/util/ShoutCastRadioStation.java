package aca.com.remote.tunes.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jim.yu on 2017/11/24.
 */

public class ShoutCastRadioStation {
    String name;
    String mediaType;
    int id;
    int bitrate;
    List<String> genre = new ArrayList<String>();
    String ct;
    int lc;
    int ml;
    String logo;

    public void setName(String m_name){
        name = m_name;
    }

    public String getName() {
        return name;
    }

    public void setMediaType(String m_type){
        mediaType = m_type;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setId(int m_id){
        id = m_id;
    }

    public int getId() {
        return id;
    }

    public void setBitrate(int m_bitrate){
        bitrate = m_bitrate;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setGenre(String m_genre){
        genre.add(m_genre);
    }

    public List<String> getGenre() {
        return genre;
    }

    public void setCt(String m_ct){
        ct = m_ct;
    }

    public String getCt() {
        return ct;
    }

    public void setLc(int m_lc){
        lc =m_lc;
    }

    public int getLc() {
        return lc;
    }

    public void setMl(int m_ml) {
        ml = m_ml;
    }

    public int getMl() {
        return ml;
    }

    public void setLogo(String m_logo){
        logo = m_logo;
    }

    public String getLogo() {
        return logo;
    }
}
