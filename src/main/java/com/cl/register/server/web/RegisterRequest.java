package com.cl.register.server.web;

/**
 * 注册请求对象
 *
 * @author qinghua.shao
 * @date 2022/6/19
 * @since 1.0.0
 */
public class RegisterRequest extends AbstractRequest {

    /**
     * 服务所在机器IP地址
     */
    private String ip;
    /**
     * 服务所在机器主机名
     */
    private String hostname;
    /**
     * 服务监听端口号
     */
    private int port;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
