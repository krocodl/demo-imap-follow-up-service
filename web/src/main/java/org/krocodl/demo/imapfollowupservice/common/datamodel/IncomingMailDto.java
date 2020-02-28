package org.krocodl.demo.imapfollowupservice.common.datamodel;

import java.util.Date;

public class IncomingMailDto {

    private String from;
    private String subject;
    private String inReplyTo;
    private Date receivingDate;

    public String getFrom() {
        return from;
    }

    public void setFrom(final String from) {
        this.from = from;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(final String subject) {
        this.subject = subject;
    }

    public String getInReplyTo() {
        return inReplyTo;
    }

    public void setInReplyTo(final String inReplyTo) {
        this.inReplyTo = inReplyTo;
    }

    public Date getReceivingDate() {
        return receivingDate;
    }

    public void setReceivingDate(final Date receivingDate) {
        this.receivingDate = receivingDate;
    }

    @Override
    public String toString() {
        return "IncomingMailDto{" +
                "from='" + from + '\'' +
                ", subject='" + subject + '\'' +
                ", receivingDate='" + receivingDate + '\'' +
                ", inReplyTo='" + inReplyTo + '\'' +
                '}';
    }
}
