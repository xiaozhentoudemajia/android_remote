package aca.com.remote.tunes.util;

/**
 * Created by jim.yu on 2017/11/24.
 */

public class ShoutCastRadioGenre {
    String name;
    int id;
    int parent_id;
    boolean hasChildren;
//        ArrayList<radioGenre> children = new ArrayList<radioGenre>();

    public ShoutCastRadioGenre(String name, int id, int parent_id, boolean child){
        this.name = name;
        this.id = id;
        this.parent_id = parent_id;
        this.hasChildren = child;
    }

    public ShoutCastRadioGenre(){

    }

    public void setName(String m_name){
        name = m_name;
    }
    public String getName() {
        return name;
    }

    public void setId(int m_id){
        id = m_id;
    }
    public int getId() {
        return id;
    }

    public void setParent_id(int m_parentId) {
        parent_id = m_parentId;
    }
    public int getParent_id() {
        return parent_id;
    }

    public void setHasChildren(boolean m_hasChildren) {
        hasChildren = m_hasChildren;
    }
    public boolean getHasChildren() {
        return hasChildren;
    }

//        public void setChildren(radioGenre m_children) {
//            children.add(m_children);
//        }
}
