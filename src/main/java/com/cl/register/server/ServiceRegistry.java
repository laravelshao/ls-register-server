package com.cl.register.server;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * 注册表
 *
 * @author qinghua.shao
 * @date 2022/6/19
 * @since 1.0.0
 */
public class ServiceRegistry {

    /**
     * 最近变更服务实例检查间隔
     */
    public static final Long RECENTLY_CHANGED_ITEM_CHECK_INTERVAL = 3000L;
    /**
     * 最近变更服务实例队列维护过期时间
     */
    public static final Long RECENTLY_CHANGED_ITEM_EXPIRED = 3 * 60 * 1000L;

    /**
     * 设置为单例
     */
    private static ServiceRegistry instance = new ServiceRegistry();

    private ServiceRegistry() {
        RecentlyChangedQueueMonitor recentlyChangedQueueMonitor = new RecentlyChangedQueueMonitor();
        recentlyChangedQueueMonitor.setDaemon(true);
        recentlyChangedQueueMonitor.start();
    }

    /**
     * 获取实例对象
     *
     * @return
     */
    public static ServiceRegistry getInstance() {
        return instance;
    }

    /**
     * 注册表：核心内存数据结构
     * <p>
     * 外层MAP：key 为服务名称，value 为这个服务的所有服务实例
     * 内层MAP：key 为服务实例ID，value 为服务实例信息
     */
    private Map<String, Map<String, ServiceInstance>> registry = new HashMap<>();

    /**
     * 最近变更的服务实例的队列
     */
    private LinkedList<RecentlyChangedServiceInstance> recentlyChangedQueue = new LinkedList<>();

    /**
     * 获取服务实例信息
     *
     * @param serviceName       服务名称
     * @param serviceInstanceId 服务实例ID
     * @return
     */
    public synchronized ServiceInstance getServiceInstance(String serviceName, String serviceInstanceId) {

        Map<String, ServiceInstance> serviceInstanceMap = registry.get(serviceName);
        if (serviceInstanceMap != null) {
            return serviceInstanceMap.get(serviceInstanceId);
        }

        return null;
    }

    /**
     * 获取完整注册表信息
     *
     * @return
     */
    public synchronized Map<String, Map<String, ServiceInstance>> getRegistry() {
        return registry;
    }

    /**
     * 获取最近有变化的注册表
     *
     * @return
     */
    public synchronized DeltaRegistry getDeltaRegistry() {

        Long totalCount = 0L;
        for (Map<String, ServiceInstance> serviceInstanceMap : registry.values()) {
            totalCount += serviceInstanceMap.size();
        }

        DeltaRegistry deltaRegistry = new DeltaRegistry(recentlyChangedQueue, totalCount);

        return deltaRegistry;
    }

    /**
     * 服务注册
     *
     * @param serviceInstance 服务实例
     */
    public synchronized void register(ServiceInstance serviceInstance) {

        // 将服务实例放入最近变更的队列中
        RecentlyChangedServiceInstance recentlyChangedItem = new RecentlyChangedServiceInstance(
                serviceInstance,
                System.currentTimeMillis(),
                ServiceInstanceOperation.REGISTER);
        recentlyChangedQueue.offer(recentlyChangedItem);

        // 获取指定服务名称的服务实例MAP
        Map<String, ServiceInstance> serviceInstanceMap = registry.get(serviceInstance.getServiceName());

        // 不存在则初始化
        if (serviceInstanceMap == null) {
            serviceInstanceMap = new HashMap<>();
            registry.put(serviceInstance.getServiceName(), serviceInstanceMap);
        }

        // 添加服务实例至注册表
        serviceInstanceMap.put(serviceInstance.getServiceInstanceId(), serviceInstance);

        System.out.println("服务实例【" + serviceInstance + "】注册成功");
        System.out.println("注册表：" + registry);
    }

    /**
     * 服务移除(从注册表移除)
     *
     * @param serviceName       服务名称
     * @param serviceInstanceId 服务实例ID
     */
    public synchronized void remove(String serviceName, String serviceInstanceId) {

        System.out.println("服务实例【" + serviceInstanceId + "】，从注册表中移除");

        // 获取服务实例
        Map<String, ServiceInstance> serviceInstanceMap = registry.get(serviceName);
        ServiceInstance serviceInstance = serviceInstanceMap.get(serviceInstanceId);

        // 将服务实例添加到最近变更服务实例队列中
        RecentlyChangedServiceInstance recentlyChangedItem = new RecentlyChangedServiceInstance(
                serviceInstance,
                System.currentTimeMillis(),
                ServiceInstanceOperation.REMOVE);
        recentlyChangedQueue.offer(recentlyChangedItem);

        // 从服务注册表删除服务实例
        serviceInstanceMap.remove(serviceInstanceId);

        System.out.println("注册表：" + registry);
    }

    /**
     * 最近变化的服务实例
     */
    class RecentlyChangedServiceInstance {

        /**
         * 服务实例
         */
        ServiceInstance serviceInstance;

        /**
         * 发生变更的时间戳
         */
        Long changedTimestamp;

        /**
         * 服务实例操作类型
         */
        String serviceInstanceOperation;

        public RecentlyChangedServiceInstance(
                ServiceInstance serviceInstance, Long changedTimestamp, String serviceInstanceOperation) {
            this.serviceInstance = serviceInstance;
            this.changedTimestamp = changedTimestamp;
            this.serviceInstanceOperation = serviceInstanceOperation;
        }

        @Override
        public String toString() {
            return "RecentlyChangedServiceInstance [serviceInstance=" + serviceInstance + ", changedTimestamp="
                    + changedTimestamp + ", serviceInstanceOperation=" + serviceInstanceOperation + "]";
        }
    }

    /**
     * 服务实例操作类型
     */
    class ServiceInstanceOperation {

        /**
         * 注册
         */
        public static final String REGISTER = "register";
        /**
         * 删除
         */
        public static final String REMOVE = "remove";
    }

    /**
     * 最近变更队列的监控线程
     */
    class RecentlyChangedQueueMonitor extends Thread {

        @Override
        public void run() {
            while (true) {
                try {
                    synchronized (instance) {
                        RecentlyChangedServiceInstance recentlyChangedItem = null;
                        long currentTimestamp = System.currentTimeMillis();

                        while ((recentlyChangedItem = recentlyChangedQueue.peek()) != null) {
                            // 判断如果一个服务实例变更信息已经在队列里存在超过3分钟就从队列中移除
                            if (currentTimestamp - recentlyChangedItem.changedTimestamp > RECENTLY_CHANGED_ITEM_EXPIRED) {
                                recentlyChangedQueue.pop();
                            }
                        }
                    }
                    Thread.sleep(RECENTLY_CHANGED_ITEM_CHECK_INTERVAL);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

