package org.krocodl.demo.imapfollowupservice.analiser;

import org.apache.commons.lang3.StringUtils;
import org.krocodl.demo.imapfollowupservice.common.datamodel.BatchOfMails;
import org.krocodl.demo.imapfollowupservice.common.datamodel.NotifyEntity;
import org.krocodl.demo.imapfollowupservice.common.datamodel.OutcomingMailEntity;
import org.krocodl.demo.imapfollowupservice.common.services.DateService;
import org.krocodl.demo.imapfollowupservice.common.services.ServiceStateService;
import org.krocodl.demo.imapfollowupservice.common.utils.DateFormater;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class MailsAnaliserImpl implements MailsAnaliser {

    private final OutcomingMailRepository outcomingMailRepository;
    private final ServiceStateService stateService;
    private final List<MailsMatchingStrategy> matchStrategies;
    private final DateService dateService;
    @Value("${analiser.remindStrategy}")
    private String remindStrategy;
    private List<Integer> remindSteps = new ArrayList<>();

    public MailsAnaliserImpl(final OutcomingMailRepository outcomingMailRepository, final ServiceStateService stateService,
                             final List<MailsMatchingStrategy> matchStrategies, final DateService dateService) {
        this.outcomingMailRepository = outcomingMailRepository;
        this.stateService = stateService;
        this.matchStrategies = matchStrategies;
        this.dateService = dateService;
    }

    /**
     * Date of the next notification or null, if notification can't be sent more
     *
     * @param sentDate
     * @return
     */
    static Date calculateNextNotificationDate(Date now, Date sentDate, List<Integer> remindSteps) {
        Calendar sentCal = Calendar.getInstance();
        sentCal.setTime(sentDate);
        for (int dayOffset : remindSteps) {
            sentCal.add(Calendar.DAY_OF_YEAR, dayOffset);
            if (sentCal.getTime().compareTo(now) > 0) {
                return sentCal.getTime();
            }
        }
        return null;
    }

    @PostConstruct
    public void postConstruct() {
        Objects.requireNonNull(remindStrategy, "anaiser.remindStrategy can't be null");
        Stream.of(StringUtils.split(remindStrategy, ",")).forEach(item -> remindSteps.add(Integer.parseInt(item)));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int saveOutcomingMails(BatchOfMails batchOfMails) {
        if (batchOfMails.getOutcomingMails().isEmpty()) {
            return 0;
        }

        /**
         * due to issues with IMAP query we have in any case to remove duplicates
         */
        outcomingMailRepository.findDuplicates(batchOfMails.getOutcomingMails().keySet()).forEach(uid -> batchOfMails.getOutcomingMails().remove(uid));

        batchOfMails.getOutcomingMails().values().stream().filter(m -> m.getQueueId() == null).forEach(entity -> {
            Date notificationDate = calculateNextNotificationDate(dateService.nowDate(), entity.getSentDate(), remindSteps);
            if (notificationDate != null) {
                entity.setNotifyDate(notificationDate);
                outcomingMailRepository.save(entity);
            }
        });

        stateService.setLastSendUid(batchOfMails.getLastSendUid());

        return batchOfMails.getOutcomingMails().size();
    }

    @Override
    public List<String> prepareMailsMatching(BatchOfMails batchOfMails) {
        List<String> foundMails = new ArrayList<>();
        if (batchOfMails.getIncomingMails().isEmpty()) {
            return foundMails;
        }
        batchOfMails.getIncomingMails().forEach(mail -> matchStrategies.forEach(strategy -> {
            foundMails.addAll(strategy.makeMatch(mail));
        }));
        return foundMails;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void applyMailsMatching(BatchOfMails batchOfMails, List<String> foundMails) {
        stateService.setLastReceiveUid(batchOfMails.getLastReceiveUid());
        outcomingMailRepository.removeByIds(foundMails);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void commitOutcomingMailsAsNotified(List<NotifyEntity> list) {
        list.forEach(notify -> {
            OutcomingMailEntity mail = outcomingMailRepository.getOne(notify.getSourceUid());
            Date nextNotificationDate = calculateNextNotificationDate(dateService.nowDate(), mail.getSentDate(), remindSteps);
            if (nextNotificationDate != null) {
                mail.setNotifyDate(nextNotificationDate);
                outcomingMailRepository.save(mail);
            } else {
                outcomingMailRepository.delete(mail);
            }
        });
    }

    @Override
    public List<NotifyEntity> createNotificationsFromOutcompingMails() {
        List<NotifyEntity> ret = new ArrayList<>();
        outcomingMailRepository.findMailsForNotification(dateService.nowDate()).forEach(mail -> {
            NotifyEntity notify = new NotifyEntity();
            notify.setWasSent(false);
            notify.setTo(mail.getTo());
            notify.setSourceUid(mail.getUid());
            notify.setSubject("Missed letter notification");
            notify.setText("You did not onswer on message '" + mail.getSubject() + "' from " + DateFormater.formatTime(mail.getSentDate()));
            ret.add(notify);
        });
        return ret;
    }

    @Override
    public int getMaximumRemindPerionsForOutcomingMails() {
        return remindSteps.stream().reduce(0, Integer::sum);
    }
}
