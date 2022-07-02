package com.cl.register.server;

/**
 * 服务实例对象
 * 定义了服务实例的所有信息：服务名称、IP地址、hostname、端口号、服务实例ID、契约信息(Lease)
 *
 * @author qinghua.shao
 * @date 2022/6/19
 * @since 1.0.0
 */
public class ServiceInstance {

    /**
     * 判断服务实例不再存活的时间
     */
    private static final Long NOT_ALIVE_PERIOD = 90 * 1000L;

    /**
     * 服务名称
     */
    private String serviceName;
    /**
     * ip地址
     */
    private String ip;
    /**
     * 主机名
     */
    private String hostname;
    /**
     * 端口号
     */
    private int port;
    /**
     * 服务实例id
     */
    private String serviceInstanceId;
    /**
     * 契约
     */
    private Lease lease;

    public ServiceInstance() {
        this.lease = new Lease();
    }

    /**
     * 服务实例续约
     */
    public void renew() {
        this.lease.renew();
    }

    /**
     * 判断服务实例是否存活
     */
    public Boolean isAlive() {
        return this.lease.isAlive();
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

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

    public String getServiceInstanceId() {
        return serviceInstanceId;
    }

    public void setServiceInstanceId(String serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
    }

    public Lease getLease() {
        return lease;
    }

    public void setLease(Lease lease) {
        this.lease = lease;
    }

    /**
     * 契约对象：维护一个服务实例跟当前注册中心的联系，包含心跳时间、创建时间等
     */
    private class Lease {

        /**
         * 最近一次心跳时间(存在多线程读写，一定要用 volatile 保证内存可见性)
         */
        private volatile Long latestHeartbeatTime = System.currentTimeMillis();

        /**
         * 续约操作：发送一次心跳，就等于将客户端与服务端之间的契约进行续约
         */
        public void renew() {
            this.latestHeartbeatTime = System.currentTimeMillis();
            System.out.println("服务实例【" + serviceInstanceId + "】，进行续约：" + latestHeartbeatTime);
        }

        /**
         * 判断当前服务实例的契约是否存活
         *
         * @return
         */
        public Boolean isAlive() {

            long currentTime = System.currentTimeMillis();

            if (currentTime - latestHeartbeatTime > NOT_ALIVE_PERIOD) {
                System.out.println("服务实例【" + serviceInstanceId + "】，已经死亡");
                return false;
            }

            System.out.println("服务实例【" + serviceInstanceId + "】，保持存活");
            return true;
        }
    }
}
