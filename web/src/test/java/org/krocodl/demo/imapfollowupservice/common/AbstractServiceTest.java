package org.krocodl.demo.imapfollowupservice.common;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.krocodl.demo.imapfollowupservice.analiser.OutcomingMailRepository;
import org.krocodl.demo.imapfollowupservice.common.services.DateService;
import org.krocodl.demo.imapfollowupservice.mocks.MockedMailServer;
import org.krocodl.demo.imapfollowupservice.notifier.NotifyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MockedMailServer.class)
public class AbstractServiceTest {

    @Autowired
    private MockedMailServer mailServer;

    @Autowired
    private DateService dateService;

    @Autowired
    private OutcomingMailRepository outcomingMailRepository;

    @Autowired
    private NotifyRepository notifyRepository;

    @Before
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void before() throws Exception {
        mailServer.clean();
        dateService.setDaysOffset(0);
        outcomingMailRepository.deleteAll();
        notifyRepository.deleteAll();
    }

}
