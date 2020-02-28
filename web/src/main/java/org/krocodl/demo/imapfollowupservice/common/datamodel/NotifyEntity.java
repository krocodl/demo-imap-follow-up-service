package org.krocodl.demo.imapfollowupservice.common.datamodel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "NOTIFY_MAIL")
public class NotifyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String sourceUid;

    @NotNull
    private String to;

    @NotNull
    private String subject;

    @NotNull
    private String text;

    @Column
    private Long partitionId;

    @NotNull
    private boolean wasSent;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
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

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public Long getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(final Long partitionId) {
        this.partitionId = partitionId;
    }


    public String getSourceUid() {
        return sourceUid;
    }

    public void setSourceUid(final String sourceUid) {
        this.sourceUid = sourceUid;
    }

    public boolean isWasSent() {
        return wasSent;
    }

    public void setWasSent(final boolean wasSent) {
        this.wasSent = wasSent;
    }

    @Override
    public String toString() {
        return "NotifyEntity{" +
                "id=" + id +
                ", sourceUid='" + sourceUid + '\'' +
                ", to='" + to + '\'' +
                ", subject='" + subject + '\'' +
                ", text='" + text + '\'' +
                ", partitionId=" + partitionId +
                ", wasSent=" + wasSent +
                '}';
    }


    public NotifyEntity withId(Long id) {
        this.id = id;
        return this;
    }

    public NotifyEntity withSourceUid(String sourceUid) {
        this.sourceUid = sourceUid;
        return this;
    }

    public NotifyEntity withTo(String to) {
        this.to = to;
        return this;
    }

    public NotifyEntity withSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public NotifyEntity withText(String text) {
        this.text = text;
        return this;
    }

    public NotifyEntity withPartitionId(Long partitionId) {
        this.partitionId = partitionId;
        return this;
    }

    public NotifyEntity withWasSent(boolean wasSent) {
        this.wasSent = wasSent;
        return this;
    }

}
