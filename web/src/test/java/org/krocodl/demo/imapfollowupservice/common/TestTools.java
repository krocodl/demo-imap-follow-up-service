package org.krocodl.demo.imapfollowupservice.common;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.net.ServerSocket;
import java.util.Properties;

public final class TestTools {

    private static Properties testProperies;

    static {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        yaml.afterPropertiesSet();
        testProperies = yaml.getObject();
    }

    private TestTools() {
    }

    public static Integer findRandomOpenPortOnAllLocalInterfaces() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception ex) {
            throw new RuntimeException("Can't get free port", ex);
        }
    }

    public static Properties getTestProperties() {
        return testProperies;
    }


}
