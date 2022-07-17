package com.cl.register.server;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

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

    /**
     * 注册表：核心内存数据结构
     * <p>
     * 外层MAP：key 为服务名称，value 为这个服务的所有服务实例
     * 内层MAP：key 为服务实例ID，value 为服务实例信息
     */
    private Map<String, Map<String, ServiceInstance>> registry = new ConcurrentHashMap<>();

    /**
     * 最近变更的服务实例的队列
     */
    private Queue<RecentlyChangedServiceInstance> recentlyChangedQueue = new ConcurrentLinkedQueue<>();

    /**
     * 读写锁
     */
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private ReadLock readLock = lock.readLock();
    private WriteLock writeLock = lock.writeLock();

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
     * 加读锁
     */
    public void readLock() {
        this.readLock.lock();
    }

    /**
     * 释放读锁
     */
    public void readUnlock() {
        this.readLock.unlock();
    }

    /**
     * 加写锁
     */
    public void writeLock() {
        this.writeLock.lock();
    }

    /**
     * 释放写锁
     */
    public void writeUnlock() {
        this.writeLock.unlock();
    }

    /**
     * 获取服务实例信息
     *
     * @param serviceName       服务名称
     * @param serviceInstanceId 服务实例ID
     * @return
     */
    public synchronized ServiceInstance getServiceInstance(String serviceName, String serviceInstanceId) {

        try {
            // 加读锁
            this.readLock();

            Map<String, ServiceInstance> serviceInstanceMap = registry.get(serviceName);
            if (serviceInstanceMap != null) {
                return serviceInstanceMap.get(serviceInstanceId);
            }
        } finally {
            // 释放读锁
            this.readUnlock();
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
    public void register(ServiceInstance serviceInstance) {

        try {
            // 加写锁
            this.writeLock();

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
                serviceInstanceMap = new ConcurrentHashMap<>();
                registry.put(serviceInstance.getServiceName(), serviceInstanceMap);
            }

            // 添加服务实例至注册表
            serviceInstanceMap.put(serviceInstance.getServiceInstanceId(), serviceInstance);

            System.out.println("服务实例【" + serviceInstance + "】注册成功");
            System.out.println("注册表：" + registry);
        } finally {
            this.writeUnlock();
        }
    }

    /**
     * 服务移除(从注册表移除)
     *
     * @param serviceName       服务名称
     * @param serviceInstanceId 服务实例ID
     */
    public synchronized void remove(String serviceName, String serviceInstanceId) {

        try {
            // 加写锁
            this.writeLock();

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
        } finally {
            // 释放写锁
            this.writeUnlock();
        }
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
                    try {
                        writeLock();

                        RecentlyChangedServiceInstance recentlyChangedItem = null;
                        long currentTimestamp = System.currentTimeMillis();

                        while ((recentlyChangedItem = recentlyChangedQueue.peek()) != null) {
                            // 判断如果一个服务实例变更信息已经在队列里存在超过3分钟就从队列中移除
                            if (currentTimestamp - recentlyChangedItem.changedTimestamp > RECENTLY_CHANGED_ITEM_EXPIRED) {
                                recentlyChangedQueue.poll();
                            }
                        }
                    } finally {
                        writeUnlock();
                    }
                    Thread.sleep(RECENTLY_CHANGED_ITEM_CHECK_INTERVAL);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

