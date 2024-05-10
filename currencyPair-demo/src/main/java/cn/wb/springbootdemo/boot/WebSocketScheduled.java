package cn.wb.springbootdemo.boot;

import cn.wb.springbootdemo.service.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * WebSocket 心跳定时任务
 *
 * @author caokuang
 */
@EnableScheduling
@Component
public class WebSocketScheduled {

    @Autowired
    private WebSocketService webSocketService;

    @Scheduled(cron = "0/3 * * * * ?")
    public void heartbeat() {
        webSocketService.heartbeat();
    }
}
