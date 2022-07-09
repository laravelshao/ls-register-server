package com.cl.register.server;

import com.cl.register.server.dto.HeartbeatRequest;
import com.cl.register.server.dto.HeartbeatResponse;
import com.cl.register.server.dto.RegisterRequest;
import com.cl.register.server.dto.RegisterResponse;

/**
 * 负责接收客户端的服务注册及心跳上报
 * <p>
 * 在 spring cloud eureka 中使用的是 jersey，接收 http 请求
 *
 * @author qinghua.shao
 * @date 2022/6/19
 * @since 1.0.0
 */
public class RegisterServerController {

    private static ServiceRegistry registry = ServiceRegistry.getInstance();

    /**
     * 服务注册
     *
     * @param registerRequest 注册请求
     * @return 注册响应
     */
    public RegisterResponse register(RegisterRequest registerRequest) {

        RegisterResponse regResponse = new RegisterResponse();

        try {
            // 组装服务实例信息
            ServiceInstance serviceInstance = new ServiceInstance();
            serviceInstance.setServiceName(registerRequest.getServiceName());
            serviceInstance.setIp(registerRequest.getIp());
            serviceInstance.setHostname(registerRequest.getHostname());
            serviceInstance.setPort(registerRequest.getPort());
            serviceInstance.setServiceInstanceId(registerRequest.getServiceInstanceId());

            // 服务注册
            registry.register(serviceInstance);

            // 更新自我保护阈值
            synchronized (SelfProtectionPolicy.class) {
                SelfProtectionPolicy selfProtectionPolicy = SelfProtectionPolicy.getInstance();
                selfProtectionPolicy.setExpectedHeartbeatRate(selfProtectionPolicy.getExpectedHeartbeatRate() + 2);
                selfProtectionPolicy.setExpectedHeartbeatThreshold((long) (selfProtectionPolicy.getExpectedHeartbeatRate() * 0.85));
            }

            regResponse.setStatus(RegisterResponse.SUCCESS);
        } catch (Exception e) {
            e.printStackTrace();
            regResponse.setStatus(RegisterResponse.FAILURE);
        }

        return regResponse;
    }

    /**
     * 服务下线
     */
    public void cancel(String serviceName, String serviceInstanceId) {

        // 从服务注册中摘除实例
        registry.remove(serviceName, serviceInstanceId);

        // 更新自我保护阈值
        synchronized (SelfProtectionPolicy.class) {
            SelfProtectionPolicy selfProtectionPolicy = SelfProtectionPolicy.getInstance();
            selfProtectionPolicy.setExpectedHeartbeatRate(selfProtectionPolicy.getExpectedHeartbeatRate() + 2);
            selfProtectionPolicy.setExpectedHeartbeatThreshold((long) (selfProtectionPolicy.getExpectedHeartbeatRate() * 0.85));
        }
    }

    /**
     * 心跳操作
     *
     * @param heartbeatRequest 心跳请求
     * @return 心跳响应
     */
    public HeartbeatResponse heartbeat(HeartbeatRequest heartbeatRequest) {

        HeartbeatResponse heartbeatResponse = new HeartbeatResponse();

        try {
            // 获取服务实例
            ServiceInstance serviceInstance = registry.getServiceInstance(
                    heartbeatRequest.getServiceName(), heartbeatRequest.getServiceInstanceId());

            // 续约操作
            serviceInstance.renew();

            // 记录一下每分钟心跳次数
            HeartbeatCounter heartbeatMessuredRate = HeartbeatCounter.getInstance();
            heartbeatMessuredRate.increment();

            heartbeatResponse.setStatus(HeartbeatResponse.SUCCESS);
        } catch (Exception e) {
            e.printStackTrace();
            heartbeatResponse.setStatus(HeartbeatResponse.FAILURE);
        }

        return heartbeatResponse;
    }

    /**
     * 拉取全量注册表
     *
     * @return
     */
    public Applications fetchFullRegistry() {
        try {
            registry.readLock();
            return new Applications(registry.getRegistry());
        } finally {
            registry.readUnlock();
        }
    }

    /**
     * 拉取增量注册表
     *
     * @return
     */
    public DeltaRegistry fetchDeltaRegistry() {
        try {
            registry.readLock();
            return registry.getDeltaRegistry();
        } finally {
            registry.readUnlock();
        }
    }

}
