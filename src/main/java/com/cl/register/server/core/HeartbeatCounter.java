package com.cl.register.server.core;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 心跳测试计数器
 *
 * @author qinghua.shao
 * @date 2022/6/26
 * @since 1.0.0
 */
public class HeartbeatCounter {

    /**
     * 单例实例
     */
    private static HeartbeatCounter instance = new HeartbeatCounter();

    /**
     * 构造私有
     */
    private HeartbeatCounter() {

        // 启动后台重置阈值线程任务
        ResetRateDaemon resetRateDaemon = new ResetRateDaemon();
        resetRateDaemon.setDaemon(true); // 设置为后台线程
        resetRateDaemon.start();
    }

    /**
     * 最近一分钟的心跳次数
     */
    private AtomicLong latestMinuteHeartbeatRate = new AtomicLong(0L);
    //private LongAdder latestMinuteHeartbeatRate = new LongAdder();
    /**
     * 最近一分钟的时间戳
     */
    private long latestMinuteTimestamp = System.currentTimeMillis();

    /**
     * 获取单例对象
     *
     * @return
     */
    public static HeartbeatCounter getInstance() {
        return instance;
    }

    /**
     * 增加一次最新一分钟的心跳次数
     */
    public void increment() {
        latestMinuteHeartbeatRate.incrementAndGet();
        //latestMinuteHeartbeatRate.increment();
    }

    /**
     * 获取最近一分钟的心跳次数
     *
     * @return
     */
    public long get() {
        return latestMinuteHeartbeatRate.get();
        //return latestMinuteHeartbeatRate.longValue();
    }

    /**
     * 重置阈值后台线程任务
     */
    public class ResetRateDaemon extends Thread {

        @Override
        public void run() {
            while (true) {
                try {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - latestMinuteTimestamp > 60 * 1000) {
                        while (true) {
                            long expectValue = latestMinuteHeartbeatRate.get();
                            if (latestMinuteHeartbeatRate.compareAndSet(expectValue, 0L)) {
                                break;
                            }
                        }

                        latestMinuteTimestamp = System.currentTimeMillis();
                    }
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
