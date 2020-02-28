package org.krocodl.demo.imapfollowupservice.common.services;

import org.krocodl.demo.imapfollowupservice.common.utils.DateFormater;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

public interface ServiceStateService extends JpaRepository<ServiceStateEntity, Integer> {

    String LAST_SEND_DATE = "lastSendDate";
    String LAST_RECEIVE_DATE = "lastReveiceDate";

    @Transactional(propagation = Propagation.MANDATORY)
    default void setState(String paramName, String value) {
        ServiceStateEntity probe = new ServiceStateEntity().withName(paramName);
        List<ServiceStateEntity> params = findAll(Example.of(probe));
        if (params.isEmpty()) {
            saveAndFlush(probe.withValue(value));
        } else {
            params.get(0).setValue(value);
            saveAndFlush(params.get(0));
        }
    }

    default String getState(String paramName, String defValue) {
        ServiceStateEntity probe = new ServiceStateEntity().withName(paramName);
        List<ServiceStateEntity> params = findAll(Example.of(probe));
        return params.isEmpty() ? defValue : params.get(0).getValue();
    }

    default Date getLastSendDate(Date defValue) {
        Date ret = DateFormater.parse(getState(LAST_SEND_DATE, null));
        return ret == null ? defValue : ret;
    }

    default void setLastSendDate(Date value) {
        if (value != null) {
            setState(LAST_SEND_DATE, DateFormater.formatTime(value));
        }
    }

    default Date getLastReceiveDate(Date defValue) {
        Date ret = DateFormater.parse(getState(LAST_RECEIVE_DATE, null));
        return ret == null ? defValue : ret;
    }

    default void setLastReceiveDate(Date value) {
        if (value != null) {
            setState(LAST_RECEIVE_DATE, DateFormater.formatTime(value));
        }
    }
}
