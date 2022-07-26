package com.laravelshao.register.server.web;

import com.laravelshao.register.server.cluster.PeersReplicateBatch;
import com.laravelshao.register.server.cluster.PeersReplicator;
import com.laravelshao.register.server.core.DeltaRegistry;
import com.laravelshao.register.server.core.HeartbeatCounter;
import com.laravelshao.register.server.core.SelfProtectionPolicy;
import com.laravelshao.register.server.core.ServiceInstance;
import com.laravelshao.register.server.core.ServiceRegistry;
import com.laravelshao.register.server.core.ServiceRegistryCache;
import com.laravelshao.register.server.core.ServiceRegistryCache.CacheKey;

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
     * 服务注册表缓存
     */
    private ServiceRegistryCache registryCache = ServiceRegistryCache.getInstance();

    /**
     * 集群同步组件
     */
    private PeersReplicator peersReplicator = PeersReplicator.getInstance();

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

            // 过期掉注册表缓存
            registryCache.invalidate();

            // 进行集群同步
            peersReplicator.replicateRegister(registerRequest);

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
    public void cancel(CancelRequest cancelRequest) {

        // 从服务注册中摘除实例
        registry.remove(cancelRequest.getServiceName(), cancelRequest.getServiceInstanceId());

        // 更新自我保护阈值
        synchronized (SelfProtectionPolicy.class) {
            SelfProtectionPolicy selfProtectionPolicy = SelfProtectionPolicy.getInstance();
            selfProtectionPolicy.setExpectedHeartbeatRate(selfProtectionPolicy.getExpectedHeartbeatRate() + 2);
            selfProtectionPolicy.setExpectedHeartbeatThreshold((long) (selfProtectionPolicy.getExpectedHeartbeatRate() * 0.85));
        }

        // 过期掉注册表缓存
        registryCache.invalidate();

        // 进行集群同步
        peersReplicator.replicateCancel(cancelRequest);
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

            // 进行集群同步
            peersReplicator.replicateHeartbeat(heartbeatRequest);

            heartbeatResponse.setStatus(HeartbeatResponse.SUCCESS);
        } catch (Exception e) {
            e.printStackTrace();
            heartbeatResponse.setStatus(HeartbeatResponse.FAILURE);
        }

        return heartbeatResponse;
    }

    /**
     * 同步batch数据
     *
     * @param batch
     */
    public void replicateBatch(PeersReplicateBatch batch) {

        for (AbstractRequest request : batch.getRequests()) {

            if (request.getType().equals(AbstractRequest.REGISTER_REQUEST)) {
                register((RegisterRequest) request);
            } else if (request.getType().equals(AbstractRequest.CANCEL_REQUEST)) {
                cancel((CancelRequest) request);
            } else if (request.getType().equals(AbstractRequest.HEARTBEAT_REQUEST)) {
                heartbeat((HeartbeatRequest) request);
            }
        }
    }

    /**
     * 拉取全量注册表
     *
     * @return
     */
    public Applications fetchFullRegistry() {
        return (Applications) registryCache.get(CacheKey.FULL_SERVICE_REGISTRY);
    }

    /**
     * 拉取增量注册表
     *
     * @return
     */
    public DeltaRegistry fetchDeltaRegistry() {
        return (DeltaRegistry) registryCache.get(CacheKey.DELTA_SERVICE_REGISTRY);
    }

}
