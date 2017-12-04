package aca.com.remote.tunes;

import aca.com.remote.tunes.daap.Session;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by gavin.liu on 2017/7/20.
 */
public class SessionWrapper {
    public final static long MAX_VALID_TIME = 300000;
    private ReentrantReadWriteLock rwlock;
    public String host;
    protected Session session;
    public long lastTime;

    public SessionWrapper(String host){
        this.host = host;
        rwlock = new ReentrantReadWriteLock();
        lastTime = System.currentTimeMillis();
    }

    public Session getSessionUnlock() {
        return session;
    }

    public Session getSession() {
        Session nowSession;
        rwlock.readLock().lock();
        lastTime = System.currentTimeMillis();
        nowSession = session;
        rwlock.readLock().unlock();
        return nowSession;
    }

    public boolean isTimeout(){
        return (( System.currentTimeMillis() -lastTime) > MAX_VALID_TIME );
    }

    public Session getSession(String host) {
        if(this.host.equals(host)){
            rwlock.readLock().lock();
            rwlock.readLock().unlock();
            lastTime = System.currentTimeMillis();
            return session;
        }
        return null;
    }
    public void  setSession(Session session){
        this.session = session;

    }

    public void writeLock(){
        rwlock.writeLock().lock();
    }

    public void writeUnlock(){
        rwlock.writeLock().unlock();
    }

    public String toString(){
        StringBuilder buf = new StringBuilder();
        buf.append("["+ this.getClass().getSimpleName() + "@");
        if(this.isTimeout())
            buf.append(host +" invalid,timeout");
        else
            buf.append(host +" valid");

        buf.append(']');
        return buf.toString();
    }

}
