package com.laravelshao.register.server.cluster;

import java.util.ArrayList;
import java.util.List;

/**
 * 注册服务端集群
 *
 * @author qinghua.shao
 * @date 2022/7/24
 * @since 1.0.0
 */
public class RegisterServerCluster {

    private static List<String> peers = new ArrayList<>();

    static {
        // 读取配置文件，看看你配合了哪些机器部署的 register-server
    }

    public static List<String> getPeers() {
        return peers;
    }
}
