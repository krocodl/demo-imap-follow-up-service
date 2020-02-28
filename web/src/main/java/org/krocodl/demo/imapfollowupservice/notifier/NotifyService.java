package org.krocodl.demo.imapfollowupservice.notifier;

import org.krocodl.demo.imapfollowupservice.common.datamodel.NotifyEntity;

import java.util.List;

public interface NotifyService {

    void addNotificationsToQueue(List<NotifyEntity> list);

    int compensateBrokenSendingTransaction(List<Long> sentQueueIds);

    int deleteNotificationsForMatchedMails(List<String> mailUids);

    int sendNotificationsFromQueue();
}
