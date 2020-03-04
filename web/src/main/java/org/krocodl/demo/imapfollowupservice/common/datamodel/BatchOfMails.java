package org.krocodl.demo.imapfollowupservice.common.datamodel;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BatchOfMails {

    private List<IncomingMailDto> incomingMails = new ArrayList<>();
    private Map<String, OutcomingMailEntity> outcomingMails = new HashMap<>();
    private long lastSendUid = 0;
    private long lastReceiveUid = 0;

    public void addIncomingMail(long muid, String uid, String from, String subject, Date receivedDate, String inReplyTo) {
        IncomingMailDto mail = new IncomingMailDto();
        mail.setFrom(from);
        mail.setSubject(subject);
        mail.setInReplyTo(inReplyTo);
        mail.setReceivingDate(receivedDate);
        incomingMails.add(mail);

        lastReceiveUid = Math.max(lastReceiveUid, muid);
    }

    public void addOutcomingMail(long muid, String uid, String to, String subject, Date sentDate, Long queueId, Date receivingDate) {
        OutcomingMailEntity mail = new OutcomingMailEntity();
        mail.setQueueId(queueId);
        mail.setSentDate(sentDate);
        mail.setTo(to);
        mail.setSubject(subject);
        mail.setUid(uid);
        outcomingMails.put(uid, mail);

        lastSendUid = Math.max(lastSendUid, muid);
    }

    public List<IncomingMailDto> getIncomingMails() {
        return incomingMails;
    }

    public Map<String, OutcomingMailEntity> getOutcomingMails() {
        return outcomingMails;
    }

    public long getLastSendUid() {
        return lastSendUid;
    }

    public long getLastReceiveUid() {
        return lastReceiveUid;
    }
}
