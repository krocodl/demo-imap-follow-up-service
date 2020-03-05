package org.krocodl.demo.imapfollowupservice.notifier;

import org.krocodl.demo.imapfollowupservice.common.datamodel.NotifyEntity;
import org.krocodl.demo.imapfollowupservice.common.services.DateService;
import org.krocodl.demo.imapfollowupservice.common.services.TransactionalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.mail.Message;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.krocodl.demo.imapfollowupservice.extractor.ExtractedMessage.FOLLOW_UP_QUEUE_ID;
import static org.krocodl.demo.imapfollowupservice.extractor.ImapMailReceiver.IMAP_USERNAME;

@Service
public class NotifyServiceImpl implements NotifyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifyServiceImpl.class);
    private final TransactionalService transactionalService;
    private final NotifyRepository notifyRepository;
    private final SmtpMailSender smtpMailSender;
    private final DateService dateService;
    private final EntityManager entityManager;
    @Value("${notifier.threadsCount}")
    private int threadsCount;
    @Value("${" + IMAP_USERNAME + "}")
    private String username;
    private ExecutorService executorService;

    public NotifyServiceImpl(final TransactionalService transactionalService, final NotifyRepository notifyRepository,
                             final SmtpMailSender smtpMailSender, final DateService dateService, final EntityManager entityManager) {
        this.transactionalService = transactionalService;
        this.notifyRepository = notifyRepository;
        this.smtpMailSender = smtpMailSender;
        this.dateService = dateService;
        this.entityManager = entityManager;
    }

    @PostConstruct
    public void postConstruct() {
        initNotificationQueue();
        executorService = Executors.newFixedThreadPool(threadsCount);
    }

    @PreDestroy
    public void preDestroy() {
        executorService.shutdownNow();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void addNotificationsToQueue(List<NotifyEntity> list) {
        list.forEach(notifyEntity -> {
            notifyEntity.setId(null);
            notifyEntity.setWasSent(false);
        });
        notifyRepository.save(list);
        notifyRepository.updateUnpartitioned(threadsCount);
        entityManager.clear();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int compensateBrokenSendingTransaction(List<Long> sentQueueIds) {
        int ret = notifyRepository.removeMessagesWithIds(sentQueueIds);
        notifyRepository.marAllAsNotSent();
        return ret;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteNotificationsForMatchedMails(List<String> mailUids) {
        return notifyRepository.removeMatchedMessages(mailUids);
    }

    private void initNotificationQueue() {
        long size = notifyRepository.count();
        LOGGER.info("{} messages in the notification queue", size);
        if (size > 0) {
            transactionalService.executeInNewTransaction(() -> {
                notifyRepository.updateAllParttions(threadsCount);
            }, "Repartitioning the queue");
        }
    }

    @Override
    public int sendNotificationsFromQueue() {
        List<Callable<Integer>> partitionTasks = IntStream.range(0, threadsCount).boxed()
                .map(idx -> (Callable<Integer>) () -> sendNotificationForOnePartition(idx))
                .collect(Collectors.toList());

        int cnt = 0;
        try {
            for (Future<Integer> task : executorService.invokeAll(partitionTasks)) {
                cnt += task.get();
            }
        } catch (InterruptedException | ExecutionException ex) {
            LOGGER.error("Can't call execution service", ex);
            Thread.currentThread().interrupt();
        }

        return cnt;
    }

    private int sendNotificationForOnePartition(int partitionId) {
        List<NotifyEntity> partition = notifyRepository.queryForSending(partitionId);
        if (partition.isEmpty()) {
            return 0;
        }

        transactionalService.executeInNewTransaction(() -> {
            notifyRepository.markAsSent(partition.stream().map(NotifyEntity::getId).collect(Collectors.toList()));
        }, " mark notification as sent from partition " + partitionId);

        List<Long> sentIds = sendNotificationsWithSmtpTransport(partition);

        transactionalService.executeInNewTransaction(() -> {
            notifyRepository.removeMessagesWithIds(sentIds);
        }, "Removing sent notifications");

        return sentIds.size();
    }

    private List<Long> sendNotificationsWithSmtpTransport(List<NotifyEntity> notifications) {
        Date now = dateService.nowDate();
        List<Long> ret = new ArrayList<>();

        notifications.forEach(notification -> {
            smtpMailSender.sendMail(mail -> {
                mail.setFrom(username);
                mail.setSentDate(now);
                mail.setRecipients(Message.RecipientType.TO, notification.getTo());
                mail.setSubject(notification.getSubject());
                mail.setText(notification.getText());
                mail.addHeader(FOLLOW_UP_QUEUE_ID, notification.getId().toString());
            }, "Mail to " + username);
            ret.add(notification.getId());
        });
        return ret;
    }

}
