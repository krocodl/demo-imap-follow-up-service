package org.krocodl.demo.imapfollowupservice.common.services;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "SERVICE_STATE", uniqueConstraints = {@UniqueConstraint(columnNames = {"name"})})
public class ServiceStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String name;

    @Column
    private String value;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public ServiceStateEntity withId(final Long id) {
        setId(id);
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public ServiceStateEntity withName(final String name) {
        setName(name);
        return this;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public ServiceStateEntity withValue(final String value) {
        setValue(value);
        return this;
    }
}
