package com.cl.register.server;

import java.util.Map;

/**
 * 微服务存活监控组件
 *
 * @author qinghua.shao
 * @date 2022/6/19
 * @since 1.0.0
 */
public class ServiceAliveMonitor {

    /**
     * 检查服务实例是否存活时间间隔
     */
    private static final Long CHECK_ALIVE_INTERVAL = 60 * 1000L;

    /**
     * 负责监控微服务存活状态的后台线程
     */
    private MonitorThread monitorThread;

    public ServiceAliveMonitor() {
        monitorThread = new MonitorThread();

        // 标记为后台线程，后台线程只有在存在前台线程时才有效，如果没有前台线程，则 JVM 会结束进程
        // 作为 服务实例存活监控线程，如果 JVM 已经退出，没有存活的必要，如果设置为工作线程，则会导致 JVM 无法退出
        monitorThread.setDaemon(true);
    }

    /**
     * 启动后台线程
     */
    public void start() {

        // 启动线程
        monitorThread.start();
    }

    /**
     * 微服务存活状态监控线程任务
     */
    private class MonitorThread extends Thread {

        private ServiceRegistry registry = ServiceRegistry.getInstance();

        @Override
        public void run() {
            Map<String, Map<String, ServiceInstance>> registryMap;

            while (true) {
                try {
                    // 判断是否开启自我保护机制
                    SelfProtectionPolicy selfProtectionPolicy = SelfProtectionPolicy.getInstance();
                    if (selfProtectionPolicy.isEnable()) {
                        Thread.sleep(1000L);
                        continue;
                    }

                    // 获取注册表MAP
                    registryMap = registry.getRegistry();

                    for (String serviceName : registryMap.keySet()) {

                        // 获取服务下服务实例列表
                        Map<String, ServiceInstance> serviceInstanceMap = registryMap.get(serviceName);

                        for (ServiceInstance serviceInstance : serviceInstanceMap.values()) {
                            // 判断是否存活，超过90秒无心跳认为已死亡，则从注册表移除
                            if (!serviceInstance.isAlive()) {
                                registry.remove(serviceName, serviceInstance.getServiceInstanceId());
                            }
                        }
                    }

                    Thread.sleep(CHECK_ALIVE_INTERVAL);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
