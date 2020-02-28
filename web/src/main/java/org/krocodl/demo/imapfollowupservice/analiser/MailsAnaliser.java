package org.krocodl.demo.imapfollowupservice.analiser;

import org.krocodl.demo.imapfollowupservice.common.datamodel.BatchOfMails;
import org.krocodl.demo.imapfollowupservice.common.datamodel.NotifyEntity;

import java.util.List;

public interface MailsAnaliser {

    int saveOutcomingMails(BatchOfMails batchOfMails);

    List<String> prepareMailsMatching(BatchOfMails batchOfMails);

    void applyMailsMatching(BatchOfMails batchOfMails, List<String> foundMails);

    void commitOutcomingMailsAsNotified(List<NotifyEntity> list);

    List<NotifyEntity> createNotificationsFromOutcompingMails();

    int getMaximumRemindPerionsForOutcomingMails();
}
