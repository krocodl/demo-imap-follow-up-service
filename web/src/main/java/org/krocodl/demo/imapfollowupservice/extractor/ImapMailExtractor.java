package org.krocodl.demo.imapfollowupservice.extractor;

import org.apache.commons.lang3.tuple.Pair;
import org.krocodl.demo.imapfollowupservice.common.datamodel.BatchOfMails;
import org.krocodl.demo.imapfollowupservice.common.services.DateService;
import org.krocodl.demo.imapfollowupservice.common.services.ServiceStateService;
import org.springframework.stereotype.Service;

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
        long lastReceiveUid = serviceState.getLastReceiveUid(0);
        long lastSendUid = serviceState.getLastSendUid(0);

        if (lastReceiveUid == 0 || lastSendUid == 0) {
            Pair<Long, Long> initUids = imapTransport.getInitialMessageUid(startingFromDaysOffset);
            lastReceiveUid = initUids.getLeft();
            lastSendUid = initUids.getRight();
        }

        Pair<List<ExtractedMessage>, List<ExtractedMessage>> messages = imapTransport.getReceivedSentMessages(lastReceiveUid, lastSendUid);

        BatchOfMails batch = new BatchOfMails();
        messages.getLeft().forEach(msg -> batch.addIncomingMail(
                msg.getMsgUid(),
                msg.getUid(),
                msg.getFrom(),
                msg.getSubject(),
                msg.getReceivedDate(),
                msg.getInReplyTo()
        ));
        messages.getRight().forEach(msg -> batch.addOutcomingMail(
                msg.getMsgUid(),
                msg.getUid(),
                msg.getTo(),
                msg.getSubject(),
                msg.getSentDate(),
                msg.getQueueId(),
                msg.getReceivedDate()
        ));

        return batch;
    }


}
