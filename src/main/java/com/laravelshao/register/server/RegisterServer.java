package com.laravelshao.register.server;

import com.laravelshao.register.server.core.ServiceAliveMonitor;
import com.laravelshao.register.server.web.HeartbeatRequest;
import com.laravelshao.register.server.web.RegisterRequest;
import com.laravelshao.register.server.web.RegisterServerController;

import java.util.UUID;

/**
 * 注册中心服务端
 *
 * @author qinghua.shao
 * @date 2022/6/19
 * @since 1.0.0
 */
public class RegisterServer {

    public static void main(String[] args) throws Exception{

        RegisterServerController controller = new RegisterServerController();

        String serviceInstanceId = UUID.randomUUID().toString().replace("-", "");

        // 模拟发起一个服务注册的请求
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setHostname("inventory-service-01");
        registerRequest.setIp("192.168.31.208");
        registerRequest.setPort(9000);
        registerRequest.setServiceInstanceId(serviceInstanceId);
        registerRequest.setServiceName("inventory-service");

        // 服务注册
        controller.register(registerRequest);

        // 模拟心跳操作，进行续约操作
        HeartbeatRequest heartbeatRequest = new HeartbeatRequest();
        heartbeatRequest.setServiceName("inventory-service");
        heartbeatRequest.setServiceInstanceId(serviceInstanceId);

        // 执行心跳操作
        controller.heartbeat(heartbeatRequest);

        // 开启后台线程检测微服务存活状态
        ServiceAliveMonitor serviceAliveMonitor = new ServiceAliveMonitor();
        serviceAliveMonitor.start();

        while(true) {
            Thread.sleep(30 * 1000);
        }
    }

}
