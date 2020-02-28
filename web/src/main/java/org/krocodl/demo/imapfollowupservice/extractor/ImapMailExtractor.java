package org.krocodl.demo.imapfollowupservice.extractor;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.krocodl.demo.imapfollowupservice.common.datamodel.BatchOfMails;
import org.krocodl.demo.imapfollowupservice.common.services.DateService;
import org.krocodl.demo.imapfollowupservice.common.services.ServiceStateService;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;


@Service
public class ImapMailExtractor implements MailExtractor {

    private final ImapMailReceiver imapTransport;

    private final ServiceStateService serviceState;

    private final DateService dateService;

    public ImapMailExtractor(final ImapMailReceiver imapTransport, final ServiceStateService serviceState, final DateService dateService) {
        this.imapTransport = imapTransport;
        this.serviceState = serviceState;
        this.dateService = dateService;
    }


    public BatchOfMails extractMails(int startingFromDaysOffset) {
        Calendar earliestTime = dateService.nowCalendar();
        earliestTime.setTime(DateUtils.truncate(earliestTime.getTime(), Calendar.DATE));
        earliestTime.add(Calendar.DAY_OF_YEAR, startingFromDaysOffset);

        Date lastReceiveDate = serviceState.getLastReceiveDate(earliestTime.getTime());
        Date lastSendDate = serviceState.getLastSendDate(earliestTime.getTime());

        Pair<List<ExtractedMessage>, List<ExtractedMessage>> messages = imapTransport.getReceivedSentMessages(lastReceiveDate, lastSendDate);

        BatchOfMails batch = new BatchOfMails();
        messages.getLeft().forEach(msg -> batch.addIncomingMail(
                msg.getUid(), msg.getFrom(), msg.getSubject(), msg.getReceivedDate(), msg.getInReplyTo()
        ));
        messages.getRight().forEach(msg -> batch.addOutcomingMail(
                msg.getUid(), msg.getTo(), msg.getSubject(), msg.getSentDate(), msg.getQueueId(), msg.getReceivedDate()
        ));

        return batch;
    }


}
