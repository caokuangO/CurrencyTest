package cn.wb.springbootdemo.service;

import cn.wb.springbootdemo.pojo.RatePojo;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket 服务类
 *
 * @author caokuang
 */
@Component
@ServerEndpoint("/websocket/{terminalId}")
public class WebSocketService {

    private final Logger logger = LoggerFactory.getLogger(WebSocketService.class);

    /**
     * 保存连接信息
     */
    private static final Map<String, Session> CLIENTS = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> TERMINAL_IDS = new HashMap<>();

    /**
     * 需要注入的Service声明为静态，让其属于类
     */
    private static TerminalService terminalService;

    /**
     * 注入的时候，给类的Service注入
     */
    @Autowired
    public void setMchDeviceInfoService(TerminalService terminalService) {
        WebSocketService.terminalService = terminalService;
    }

    @OnOpen
    public void onOpen(@PathParam("terminalId") String terminalId, Session session) throws Exception {
        logger.info(session.getRequestURI().getPath() + "，打开连接开始：" + session.getId());

        //当前连接已存在，关闭
        if (CLIENTS.containsKey(terminalId)) {
            onClose(CLIENTS.get(terminalId));
        }

        TERMINAL_IDS.put(terminalId, new AtomicInteger(0));
        CLIENTS.put(terminalId, session);

        logger.info(session.getRequestURI().getPath() + "，打开连接完成：" + session.getId());

        terminalService.terminal();
    }

    @OnClose
    public void onClose(@PathParam("terminalId") String terminalId, Session session) throws Exception {
        logger.info(session.getRequestURI().getPath() + "，关闭连接开始：" + session.getId());

        CLIENTS.remove(terminalId);
        TERMINAL_IDS.remove(terminalId);

        logger.info(session.getRequestURI().getPath() + "，关闭连接完成：" + session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        logger.info("前台发送消息：" + message);

        if ("心跳".equals(message)) {
            //重置当前终端心跳次数
            TERMINAL_IDS.get(message).set(0);
            return;
        }

        sendMessage("收到消息：" + message, session);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        logger.error(error.toString());
    }

    public void onClose(Session session) {
        //判断当前连接是否在线
//        if (!session.isOpen()) {
//            return;
//        }

        try {
            session.close();
        } catch (IOException e) {
            logger.error("金斗云关闭连接异常：" + e);
        }
    }
    private List<RatePojo> fetchAndUpdateExchangeRate() {
        String ratePairs[]={"EUR/USD","USD/JPY","USD/CNY","GPD/USD"};
        List<RatePojo> rates=new ArrayList<>();
        for(String ratePair:ratePairs){
            System.out.println("Fetching "+ratePair+"...");
            RatePojo rate=new RatePojo();
            rate.setCurrencyPair(ratePair);
            Random random = new Random();
            // 生成0.0000到1.0000之间的随机数（不包括1.0000）
            double randomValue = random.nextDouble();
            // 调整范围到0.0000到9999.9999
            randomValue *= 9.9999;
            // 为了确保四舍五入后是四位小数，我们可以使用DecimalFormat进行格式化
            DecimalFormat df = new DecimalFormat("0.0000");
            String formattedValue = df.format(randomValue);
            rate.setFxRate(formattedValue);
            rates.add(rate);
        }
        return rates;
    }
    public void heartbeat() {
        //检查所有终端心跳次数
        for (String key : TERMINAL_IDS.keySet()) {
            //心跳3次及以上的主动断开
            if ((TERMINAL_IDS.get(key).intValue() >= 10)) {
                logger.info("心跳超时，关闭连接：" + key);
                onClose(CLIENTS.get(key));
            }
        }

        for (String key : CLIENTS.keySet()) {
            //记录当前终端心跳次数
            TERMINAL_IDS.get(key).incrementAndGet();
            List<RatePojo> rateLst=fetchAndUpdateExchangeRate();
            String jsonString = JSON.toJSONString(rateLst);
            sendMessage(jsonString, CLIENTS.get(key));
        }
    }

    public void sendMessage(String message, Session session) {
        try {
            session.getAsyncRemote().sendText(message);

            logger.info("推送成功：" + message);
        } catch (Exception e) {
            logger.error("推送异常：" + e);
        }
    }

    public boolean sendMessage(String terminalId, String message) {
        try {
            Session session = CLIENTS.get(terminalId);
            session.getAsyncRemote().sendText(message);

            logger.info("推送成功：" + message);
            return true;
        } catch (Exception e) {
            logger.error("推送异常：" + e);
            return false;
        }
    }
}
