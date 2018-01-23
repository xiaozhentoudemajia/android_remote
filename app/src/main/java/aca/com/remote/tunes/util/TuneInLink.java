package aca.com.remote.tunes.util;

/**
 * Created by jim.yu on 2017/11/24.
 */

public class TuneInLink {
    String type;
    String text;
    String url;
    String key;
    String guide_id;

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

    public void setKey(String m_key) {
        key = m_key;
    }

    public String getKey() {
        return key;
    }

    public void setGuide_id(String m_guide_id) {
        guide_id = m_guide_id;
    }

    public String getGuide_id() {
        return guide_id;
    }
}
