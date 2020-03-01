package org.krocodl.demo.imapfollowupservice;

import org.krocodl.demo.imapfollowupservice.mainprocess.BusinessProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableConfigurationProperties
@Configuration
@EnableJpaRepositories
@EnableTransactionManagement
@EnableScheduling
public class MainApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainApplication.class);

    public static void main(String[] args) {

        ConfigurableApplicationContext context = null;
        try {
            context = SpringApplication.run(MainApplication.class, args);
        } catch (Exception ex) {
            LOGGER.error("Startap initialization exception", ex);
            // due to current startup logic we can do it fast and safe
            System.exit(2);
        }

        context.getBean(BusinessProcessService.class).executeBusinessProcess();
    }

}
