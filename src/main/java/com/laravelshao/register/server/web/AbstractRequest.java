package com.laravelshao.register.server.web;

/**
 * 基础请求
 *
 * @author qinghua.shao
 * @date 2022/7/23
 * @since 1.0.0
 */
public class AbstractRequest {

    public static final Integer REGISTER_REQUEST = 1;
    public static final Integer CANCEL_REQUEST = 2;
    public static final Integer HEARTBEAT_REQUEST = 3;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 服务实例ID
     */
    private String serviceInstanceId;

    /**
     * 请求类型
     */
    private Integer type;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceInstanceId() {
        return serviceInstanceId;
    }

    public void setServiceInstanceId(String serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "AbstractRequest{" +
                "serviceName='" + serviceName + '\'' +
                ", serviceInstanceId='" + serviceInstanceId + '\'' +
                '}';
    }
}
