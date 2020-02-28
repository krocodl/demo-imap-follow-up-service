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
import javax.mail.Message;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

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

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void addNotificationsToQueue(List<NotifyEntity> list) {
        if (list.isEmpty()) {
            return;
        }

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
        int ret = notifyRepository.removeLostMessages(sentQueueIds);
        notifyRepository.markAsNotSent();
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
        List<List<NotifyEntity>> partitions = transactionalService.executeInNewTransaction(() -> {
            List<List<NotifyEntity>> ret = new ArrayList<>();
            LongStream.range(0, threadsCount).forEach(partitionId -> {
                List<NotifyEntity> partition = notifyRepository.queryForSending(partitionId);
                if (!partition.isEmpty()) {
                    ret.add(partition);
                    notifyRepository.markAsSent(partition.stream().map(NotifyEntity::getId).collect(Collectors.toList()));
                    LOGGER.info("Select {} notifications for {} partition", partition.size(), partitionId);
                }
            });
            return ret;
        }, "Select notifies for sending");

        List<Callable<List<Long>>> tasks = partitions.stream()
                .map(partition -> (Callable<List<Long>>) () -> sendNotifications(partition)).collect(Collectors.toList());

        List<Future<List<Long>>> results = new ArrayList<>();
        try {
            results = executorService.invokeAll(tasks);
        } catch (InterruptedException ex) {
            LOGGER.error("Can't call execution service", ex);
            Thread.currentThread().interrupt();
        }

        List<Long> ids = new ArrayList<>();
        results.forEach(task -> {
            if (task.isDone()) {
                try {
                    ids.addAll(task.get());
                } catch (Exception ex) {
                    throw new NotifyException("Can't get ids from task", ex.getCause());
                }
            }
        });

        transactionalService.executeInNewTransaction(() -> {
            //@FIXME on Oracle with more then 1000 mail will be error, use special dialect
            notifyRepository.removeLostMessages(ids);
        }, "Removing sent notifications");

        return ids.size();
    }

    private List<Long> sendNotifications(List<NotifyEntity> notifications) {
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
