package cn.wb.springbootdemo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * TerminalService
 *
 * @author caokuang
 */
@Service
public class TerminalService {

    private final Logger logger = LoggerFactory.getLogger(TerminalService.class);

    public void terminal() {
        logger.info("这是一条测试日志");
    }
}
