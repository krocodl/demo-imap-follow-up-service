package org.krocodl.demo.imapfollowupservice.common.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.Callable;

@Service
public class TransactionalService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionalService.class);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> T executeInNewTransaction(Callable<T> callable, String desc) {
        LOGGER.info("Executing {}", desc);
        try {
            return callable.call();
        } catch (Exception ex) {
            throw new IllegalStateException("Can't call " + desc, ex);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeInNewTransaction(Runnable callable, String desc) {
        LOGGER.info("Executing {}", desc);
        try {
            callable.run();
        } catch (Exception ex) {
            throw new IllegalStateException("Can't call " + desc, ex);
        }
    }
}
