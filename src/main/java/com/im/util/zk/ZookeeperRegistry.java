package com.im.util.zk;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.net.InetAddress;

@Component
public class ZookeeperRegistry {

    @Value("${im.zookeeper.connect-string}")
    private String connectString;

    @Value("${im.zookeeper.enabled:true}")
    private boolean enabled;

    @Value("${im.server.port}")
    private int port;

    private CuratorFramework client;

    @PostConstruct
    public void init() {
        if (!enabled) {
            System.out.println("Zookeeper registry is disabled.");
            return;
        }
        client = CuratorFrameworkFactory.builder()
                .connectString(connectString)
                .sessionTimeoutMs(5000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        client.start();
        
        registerServer();
    }

    private void registerServer() {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            String path = "/im/servers/" + ip + ":" + port;
            if (client.checkExists().forPath(path) == null) {
                client.create().creatingParentsIfNeeded()
                        .withMode(CreateMode.EPHEMERAL)
                        .forPath(path);
                System.out.println("Server registered to ZK: " + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
