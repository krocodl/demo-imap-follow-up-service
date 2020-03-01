package org.krocodl.demo.imapfollowupservice.mainprocess;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.krocodl.demo.imapfollowupservice.analiser.MailsAnaliser;
import org.krocodl.demo.imapfollowupservice.common.datamodel.BatchOfMails;
import org.krocodl.demo.imapfollowupservice.common.datamodel.NotifyEntity;
import org.krocodl.demo.imapfollowupservice.common.datamodel.OutcomingMailEntity;
import org.krocodl.demo.imapfollowupservice.common.services.TransactionalService;
import org.krocodl.demo.imapfollowupservice.common.utils.DateFormater;
import org.krocodl.demo.imapfollowupservice.common.utils.ExceptionsUtils;
import org.krocodl.demo.imapfollowupservice.common.utils.LogUtils;
import org.krocodl.demo.imapfollowupservice.extractor.MailExtractor;
import org.krocodl.demo.imapfollowupservice.notifier.NotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class BusinessProcessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessProcessService.class);
    private final MailExtractor extractor;
    private final MailsAnaliser analiser;
    private final NotifyService notifyService;
    private final TransactionalService transactionalService;
    @Value("${businessProcess.retryMinutes}")
    private int retryMinutes;
    private ScheduledThreadPoolExecutor singleTheadExecutorService;

    public BusinessProcessService(final MailExtractor extractor, final MailsAnaliser analiser, final NotifyService notifyService,
                                  final TransactionalService transactionalService) {
        this.extractor = extractor;
        this.analiser = analiser;
        this.notifyService = notifyService;
        this.transactionalService = transactionalService;
    }

    @PostConstruct
    public void postConstruct() {
        singleTheadExecutorService = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
    }

    @PreDestroy
    public void preDestroy() {
        singleTheadExecutorService.shutdownNow();
    }

    String getManuallySchedulledTime() {
        Object task = singleTheadExecutorService.getQueue().peek();
        if (task == null) {
            return null;
        }
        try {
            return DateFormater.formatTime(new Date((long) FieldUtils.readField(task, "time", true)));
        } catch (Exception ex) {
            LOGGER.error("Can't get time from {}", task);
            return null;
        }
    }

    @Scheduled(cron = "${businessProcess.cron}")
    public void activateByCron() {
        executeBusinessProcess();
    }

    public synchronized boolean executeBusinessProcess() {
        LOGGER.info("Attempt to execute");
        if (singleTheadExecutorService.getQueue().isEmpty()) {
            singleTheadExecutorService.schedule(executeBusinessProcessWithRetryPolicy(), 0, TimeUnit.SECONDS);
            return true;
        }

        return false;
    }

    private Runnable executeBusinessProcessWithRetryPolicy() {
        return () -> {
            try {
                executeRawBusinessProcess();
            } catch (Exception ex) {
                if (ExceptionsUtils.isTimeoutException(ex) || ExceptionsUtils.isMailConnectionException(ex)) {
                    LOGGER.error("Due to some transport issues, process will be re-executed after {} minutes", retryMinutes);
                    singleTheadExecutorService.schedule(executeBusinessProcessWithRetryPolicy(), retryMinutes, TimeUnit.MINUTES);
                } else {
                    throw new BusinessProcessException("Can't execute process", ex);
                }
            }
        };
    }

    protected void executeRawBusinessProcess() {
        BatchOfMails batch = extractor.extractMails(-analiser.getMaximumRemindPerionsForOutcomingMails());

        int savedCount = transactionalService.executeInNewTransaction(() -> analiser.saveOutcomingMails(batch), "Saving batch of received mails");
        LOGGER.info("{} outcoming messages were saved", savedCount);

        int lostCount = transactionalService.executeInNewTransaction(() -> {
            List<Long> sentQueueIds = batch.getOutcomingMails().values().stream().
                    map(OutcomingMailEntity::getQueueId).
                    filter(Objects::nonNull).
                    collect(Collectors.toList());
            return notifyService.compensateBrokenSendingTransaction(sentQueueIds);
        }, "Removing lost messages from the notify queue");
        LOGGER.info(" Found {} lost messages in the notify queue", lostCount);

        List<String> matchedMailIds = analiser.prepareMailsMatching(batch);
        LogUtils.infoCollection(LOGGER, matchedMailIds, "Matched messages");
        transactionalService.executeInNewTransaction(() -> {
            analiser.applyMailsMatching(batch, matchedMailIds);
            notifyService.deleteNotificationsForMatchedMails(matchedMailIds);
        }, "Applying matching");


        List<NotifyEntity> notifications = analiser.createNotificationsFromOutcompingMails();
        LogUtils.infoCollection(LOGGER, notifications, "Created notifications");
        transactionalService.executeInNewTransaction(() -> {
            notifyService.addNotificationsToQueue(notifications);
            analiser.commitOutcomingMailsAsNotified(notifications);
        }, "Applying created notifications");

        int count = notifyService.sendNotificationsFromQueue();
        LOGGER.info("{} notifications were sent", count);
    }

}
