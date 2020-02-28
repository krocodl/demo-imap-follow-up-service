package org.krocodl.demo.imapfollowupservice.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

public abstract class AbstractServiceController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractServiceController.class);


    @Value("${server.port}")
    private int serverPort;

    @Value("${server.contextPath}")
    private String contextPath;

    protected void printEndPointPostInfo(String path, String desc) {
        LOGGER.info("Use http://localhost:{}{}{} for {} by POST", serverPort, contextPath, path, desc);
    }

    protected void printEndPointGetInfo(String path, String desc) {
        LOGGER.info("Use http://localhost:{}{}{} for {} by GET", serverPort, contextPath, path, desc);
    }
}
