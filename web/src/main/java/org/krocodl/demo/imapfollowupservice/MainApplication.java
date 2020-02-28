package org.krocodl.demo.imapfollowupservice;

import org.krocodl.demo.imapfollowupservice.mainprocess.BusinessProcessService;
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

    public static void main(String[] args) {

        ConfigurableApplicationContext context = SpringApplication.run(MainApplication.class, args);

        context.getBean(BusinessProcessService.class).executeBusinessProcess();
    }

}
