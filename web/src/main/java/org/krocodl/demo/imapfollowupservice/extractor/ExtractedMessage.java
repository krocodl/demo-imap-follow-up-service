package org.krocodl.demo.imapfollowupservice.extractor;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import org.krocodl.demo.imapfollowupservice.common.utils.MailUtils;
import org.springframework.util.StringUtils;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import java.util.Date;
import java.util.Map;

public class ExtractedMessage {

    public static final String IN_REPLY_TO = "In-Reply-To";
    public static final String FOLLOW_UP_QUEUE_ID = "follow-up-queue-id";
    private String from;
    private String to;
    private String subject;
    private Date sentDate;
    private Date receivedDate;
    private String uid;
    private String inReplyTo;
    private Long queueId;
    private Long msgUid;

    public ExtractedMessage(final IMAPFolder folder, final Message message) {
        try {
            this.to = ((InternetAddress) message.getAllRecipients()[0]).getAddress();
            this.subject = message.getSubject();
            this.sentDate = message.getSentDate();
            this.receivedDate = message.getReceivedDate();
            this.uid = ((IMAPMessage) message).getMessageID();
            this.msgUid = folder.getUID(message);

            from = message.getFrom()[0].toString();
            if (from.contains("<") && from.endsWith(">")) {
                from = from.substring(from.lastIndexOf("<") + 1, from.length() - 1);
            }

            Map<String, String> headers = MailUtils.getHeaders(message);
            this.inReplyTo = headers.get(IN_REPLY_TO);
            String queueIdStr = headers.get(FOLLOW_UP_QUEUE_ID);
            queueId = StringUtils.isEmpty(queueIdStr) ? null : Long.decode(queueIdStr);
        } catch (Exception ex) {
            throw new ImapTransportException("Can't extract data from message", ex);
        }
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getSubject() {
        return subject;
    }

    public Date getSentDate() {
        return sentDate;
    }

    public Date getReceivedDate() {
        return receivedDate;
    }

    public String getUid() {
        return uid;
    }

    public String getInReplyTo() {
        return inReplyTo;
    }

    public Long getQueueId() {
        return queueId;
    }

    public Long getMsgUid() {
        return msgUid;
    }
}
