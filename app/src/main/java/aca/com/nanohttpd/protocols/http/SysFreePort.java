package aca.com.nanohttpd.protocols.http;

import java.io.IOException;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.Random;

/**
 * Created by ali_mac on 2018/4/18.
 */

public class SysFreePort {
    private static Random random = new Random();

    private Socket socket;

    /**
     * <p>
     * <b>获取系统空闲端口</b>
     * <p>
     * <pre>
     * 获取系统空闲端口，并占用该端口资源
     * </pre>
     *
     * @throws    IOException
     */
    public static SysFreePort custom() throws IOException {
        return new SysFreePort();
    }

    private SysFreePort() throws IOException {
        socket = new Socket();
        InetSocketAddress inetAddress = new InetSocketAddress(0);
        socket.bind(inetAddress);
    }

    /**
     * <p>
     * <b>释放端口资源</b>
     * <p>
     * <pre>
     * 释放该端口资源
     * </pre>
     *
     * @throws    IOException
     */
    public void freePort() throws IOException {
        if (null == socket || socket.isClosed()) {
            return;
        }

        socket.close();
    }

    /**
     * <p>
     * <b>返回端口</b>
     * <p>
     * <pre>
     * 返回端口，不释放该端口资源
     * </pre>
     */
    public int getPort() {
        if (null == socket || socket.isClosed()) {
            return -1;
        }

        return socket.getLocalPort();
    }

    /**
     * <p>
     * <b>返回端口</b>
     * <p>
     * <pre>
     * 返回端口并释放该端口资源
     * </pre>
     *
     * @throws    IOException
     */
    public int getPortAndFree() throws IOException {
        if (null == socket || socket.isClosed()) {
            return -1;
        }

        int port = socket.getLocalPort();
        socket.close();
        return port;
    }

    /**
     * <p>
     * <b>生成随机port</b>
     * <p>
     * <pre>
     * 在[start, end)间随机生成一个数字作为port
     * </pre>
     *
     * @param   start
     * @param   end
     * @return  int
     */
    public static int random(int start, int end) {
        return random.nextInt(Math.abs(end - start)) + start;
    }
}
