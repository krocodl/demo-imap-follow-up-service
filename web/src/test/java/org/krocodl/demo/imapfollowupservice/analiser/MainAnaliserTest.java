package org.krocodl.demo.imapfollowupservice.analiser;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import org.krocodl.demo.imapfollowupservice.common.AbstractServiceTest;
import org.krocodl.demo.imapfollowupservice.common.datamodel.BatchOfMails;
import org.krocodl.demo.imapfollowupservice.common.datamodel.NotifyEntity;
import org.krocodl.demo.imapfollowupservice.common.datamodel.OutcomingMailEntity;
import org.krocodl.demo.imapfollowupservice.common.services.DateService;
import org.krocodl.demo.imapfollowupservice.common.services.ServiceStateService;
import org.krocodl.demo.imapfollowupservice.notifier.NotifyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MainAnaliserTest extends AbstractServiceTest {

    @Autowired
    private MailsAnaliser analiser;

    @Autowired
    private DateService dateService;

    @Autowired
    private ServiceStateService serviceState;

    @Autowired
    private OutcomingMailRepository outcomingMailRepository;

    @Autowired
    private NotifyRepository notifyRepository;


    @Test
    public void getMaximumRemindPerionsForOutcomingMailsTest() {

        assertThat(analiser.getMaximumRemindPerionsForOutcomingMails()).isEqualTo(24);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveOutcomingMailsTest() {
        Calendar calendar = Calendar.getInstance();
        Date date1 = calendar.getTime();
        calendar.add(Calendar.MINUTE, 1);
        Date date2 = calendar.getTime();
        calendar.add(Calendar.MINUTE, 1);
        Date date3 = calendar.getTime();
        calendar.add(Calendar.MINUTE, 1);

        BatchOfMails batch = new BatchOfMails();
        batch.addOutcomingMail("1", "t1", "s1", date1, null, date1);
        batch.addOutcomingMail("2", "t2", "s2", date2, 3L, date2);

        assertThat(analiser.saveOutcomingMails(batch)).isEqualTo(2);
        assertThat(outcomingMailRepository.findAll()).hasSize(1);
        assertThat(outcomingMailRepository.findAll().get(0).getUid()).isEqualTo("1");
        assertThat(outcomingMailRepository.findAll().get(0).getNotifyDate()).isNotNull();
        assertThat(serviceState.getLastSendDate(null)).isEqualTo(DateUtils.truncate(date2, Calendar.SECOND));

        batch = new BatchOfMails();
        batch.addOutcomingMail("1", "t1", "s1", date1, null, date1);
        batch.addOutcomingMail("3", "t3", "s3", date3, null, date3);

        assertThat(analiser.saveOutcomingMails(batch)).isEqualTo(1);
        assertThat(outcomingMailRepository.findAll()).hasSize(2);
        assertThat(serviceState.getLastSendDate(null)).isEqualTo(DateUtils.truncate(date3, Calendar.SECOND));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void prepareAndApplyMailsMatchingTest() {
        Calendar calendar = Calendar.getInstance();
        Date date1 = calendar.getTime();

        BatchOfMails batch = new BatchOfMails();
        batch.addOutcomingMail("1", "t1", "s1", date1, null, date1);
        batch.addOutcomingMail("2", "t2", "s2", date1, null, date1);
        batch.addOutcomingMail("3", "t3", "s3", date1, null, date1);

        assertThat(analiser.saveOutcomingMails(batch)).isEqualTo(3);
        assertThat(outcomingMailRepository.findAll()).hasSize(3);

        calendar.add(Calendar.MINUTE, 1);
        Date date2 = calendar.getTime();

        batch = new BatchOfMails();
        batch.addIncomingMail("4", "f4", "s4", date2, "1");
        batch.addIncomingMail("5", "f5", "RE: s2", date2, null);

        calendar.add(Calendar.MINUTE, 1);
        Date date3 = calendar.getTime();
        batch.addIncomingMail("6", "f6", "s6", date3, "8");

        List<String> foundIds = analiser.prepareMailsMatching(batch);
        assertThat(foundIds).containsExactly("1", "2");

        analiser.applyMailsMatching(batch, foundIds);

        assertThat(outcomingMailRepository.findAll()).hasSize(1);
        assertThat(outcomingMailRepository.findAll().get(0).getUid()).isEqualTo("3");
        assertThat(serviceState.getLastReceiveDate(null)).isEqualTo(DateUtils.truncate(date3, Calendar.SECOND));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createAndCommitNotificationsFromOutcompingMailsTest() {
        Calendar calendar = Calendar.getInstance();
        Date date1 = calendar.getTime();

        BatchOfMails batch = new BatchOfMails();
        batch.addOutcomingMail("1", "t1", "s1", date1, null, date1);

        assertThat(analiser.saveOutcomingMails(batch)).isEqualTo(1);
        assertThat(outcomingMailRepository.findAll()).hasSize(1);
        assertThat(notifyRepository.findAll()).hasSize(0);

        List<NotifyEntity> notifications = analiser.createNotificationsFromOutcompingMails();
        assertThat(notifications).hasSize(0);

        dateService.setDaysOffset(4);
        notifications = analiser.createNotificationsFromOutcompingMails();
        assertThat(notifications).hasSize(1);

        OutcomingMailEntity mail = outcomingMailRepository.findAll().get(0);
        Date notifyDate = mail.getNotifyDate();
        assertThat(notifyDate).isAfter(date1);

        NotifyEntity notification = notifications.get(0);
        assertThat(notification.getSourceUid()).isEqualTo(mail.getUid());
        assertThat(notification.getTo()).isEqualTo(mail.getTo());

        analiser.commitOutcomingMailsAsNotified(notifications);
        assertThat(outcomingMailRepository.findAll().get(0).getNotifyDate()).isAfter(notifyDate);

        dateService.setDaysOffset(100);
        notifications = analiser.createNotificationsFromOutcompingMails();
        assertThat(notifications).hasSize(1);

        analiser.commitOutcomingMailsAsNotified(notifications);
        assertThat(outcomingMailRepository.findAll()).hasSize(0);
    }

    @Test
    public void calculateNextNotificationDateTest() {
        List<Integer> remindSteps = Arrays.asList(3, 7, 14);
        Calendar sendCal = dateService.nowCalendar();
        Calendar nowCal = dateService.nowCalendar();

        sendCal.add(Calendar.DAY_OF_YEAR, -100);
        Date nextSent = MailsAnaliserImpl.calculateNextNotificationDate(nowCal.getTime(), sendCal.getTime(), remindSteps);
        assertThat(nextSent).isNull();

        sendCal = dateService.nowCalendar();
        nextSent = MailsAnaliserImpl.calculateNextNotificationDate(nowCal.getTime(), sendCal.getTime(), remindSteps);
        sendCal.add(Calendar.DAY_OF_YEAR, 3);
        assertThat(nextSent).isEqualTo(sendCal.getTime());

        sendCal = dateService.nowCalendar();
        sendCal.add(Calendar.DAY_OF_YEAR, 1);
        nextSent = MailsAnaliserImpl.calculateNextNotificationDate(nowCal.getTime(), sendCal.getTime(), remindSteps);
        sendCal.add(Calendar.DAY_OF_YEAR, 3);
        assertThat(nextSent).isEqualTo(sendCal.getTime());

        sendCal = dateService.nowCalendar();
        sendCal.add(Calendar.DAY_OF_YEAR, 100);
        nextSent = MailsAnaliserImpl.calculateNextNotificationDate(nowCal.getTime(), sendCal.getTime(), remindSteps);
        sendCal.add(Calendar.DAY_OF_YEAR, 3);
        assertThat(nextSent).isEqualTo(sendCal.getTime());

        sendCal = dateService.nowCalendar();
        sendCal.add(Calendar.DAY_OF_YEAR, -1);
        nextSent = MailsAnaliserImpl.calculateNextNotificationDate(nowCal.getTime(), sendCal.getTime(), remindSteps);
        sendCal.add(Calendar.DAY_OF_YEAR, 3);
        assertThat(nextSent).isEqualTo(sendCal.getTime());

        sendCal = dateService.nowCalendar();
        sendCal.add(Calendar.DAY_OF_YEAR, -4);
        nextSent = MailsAnaliserImpl.calculateNextNotificationDate(nowCal.getTime(), sendCal.getTime(), remindSteps);
        sendCal.add(Calendar.DAY_OF_YEAR, 3 + 7);
        assertThat(nextSent).isEqualTo(sendCal.getTime());
    }

}
