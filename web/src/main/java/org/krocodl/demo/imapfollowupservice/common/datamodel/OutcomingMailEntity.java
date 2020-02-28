package org.krocodl.demo.imapfollowupservice.common.datamodel;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.krocodl.demo.imapfollowupservice.common.utils.DateFormater;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Table(name = "OUTCOMING_MAIL")
public class OutcomingMailEntity {

    @Id
    private String uid;

    @NotNull
    private String to;

    @Column
    private String subject;

    @JsonFormat(pattern = DateFormater.DD_MM_YYYY_HH_MM_SS)
    @NotNull
    private Date sentDate;

    @JsonFormat(pattern = DateFormater.DD_MM_YYYY_HH_MM_SS)
    @NotNull
    private Date notifyDate;

    @JsonIgnore
    @Transient
    private Long queueId;

    public String getUid() {
        return uid;
    }

    public void setUid(final String uid) {
        this.uid = uid;
    }

    public String getTo() {
        return to;
    }

    public void setTo(final String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(final String subject) {
        this.subject = subject;
    }

    public Date getSentDate() {
        return sentDate;
    }

    public void setSentDate(final Date sentDate) {
        this.sentDate = sentDate;
    }

    public Date getNotifyDate() {
        return notifyDate;
    }

    public void setNotifyDate(final Date notifyDate) {
        this.notifyDate = notifyDate;
    }

    public Long getQueueId() {
        return queueId;
    }

    public void setQueueId(final Long queueId) {
        this.queueId = queueId;
    }

    @Override
    public String toString() {
        return "OutcomingMailEntity{" +
                "uid='" + uid + '\'' +
                ", to='" + to + '\'' +
                ", subject='" + subject + '\'' +
                ", sentDate=" + sentDate +
                ", notifyDate=" + notifyDate +
                ", queueId='" + queueId + '\'' +
                '}';
    }
}
