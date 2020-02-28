package org.krocodl.demo.imapfollowupservice.common;

import org.junit.Test;
import org.krocodl.demo.imapfollowupservice.common.services.ServiceStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

public class ServiceStateServiceTest extends AbstractServiceTest {

    public static final String SOME_VALUE = "someValue";
    public static final String ANOTHER_VALUE = "anotherValue";
    private static final String SOME_PARAM = "someParam";
    @Autowired
    private ServiceStateService service;

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void stateTest() {
        assertThat(service.getState(SOME_PARAM, null)).isNull();
        assertThat(service.getState(SOME_PARAM, SOME_PARAM)).isEqualTo(SOME_PARAM);

        service.setState(SOME_PARAM, SOME_VALUE);
        assertThat(service.getState(SOME_PARAM, null)).isEqualTo(SOME_VALUE);
        assertThat(service.getState(SOME_PARAM, SOME_PARAM)).isEqualTo(SOME_VALUE);

        assertThat(service.getState(SOME_VALUE, null)).isNull();
        assertThat(service.getState(SOME_VALUE, SOME_VALUE)).isEqualTo(SOME_VALUE);

        service.setState(SOME_PARAM, ANOTHER_VALUE);
        assertThat(service.getState(SOME_PARAM, null)).isEqualTo(ANOTHER_VALUE);
        assertThat(service.getState(SOME_PARAM, SOME_PARAM)).isEqualTo(ANOTHER_VALUE);

        assertThat(service.getState(SOME_VALUE, null)).isNull();
        assertThat(service.getState(SOME_VALUE, SOME_VALUE)).isEqualTo(SOME_VALUE);
    }

}
