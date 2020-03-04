package org.krocodl.demo.imapfollowupservice.common.services;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ServiceStateService extends JpaRepository<ServiceStateEntity, Integer> {

    String LAST_SEND_UID = "lastSendUid";
    String LAST_RECEIVE_UID = "lastReveiceUid";

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

    default long getLastSendUid(long defValue) {
        String str = getState(LAST_SEND_UID, null);
        return StringUtils.isEmpty(str) ? defValue : Long.parseLong(str);
    }

    default void setLastSendUid(long value) {
        setState(LAST_SEND_UID, String.valueOf(value));
    }

    default long getLastReceiveUid(long defValue) {
        String str = getState(LAST_RECEIVE_UID, null);
        return StringUtils.isEmpty(str) ? defValue : Long.parseLong(str);
    }

    default void setLastReceiveUid(long value) {
        setState(LAST_RECEIVE_UID, String.valueOf(value));
    }
}
