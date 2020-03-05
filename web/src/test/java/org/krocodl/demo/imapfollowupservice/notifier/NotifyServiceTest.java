package org.krocodl.demo.imapfollowupservice.notifier;

import org.junit.Test;
import org.krocodl.demo.imapfollowupservice.common.AbstractServiceTest;
import org.krocodl.demo.imapfollowupservice.common.datamodel.NotifyEntity;
import org.krocodl.demo.imapfollowupservice.common.services.TransactionalService;
import org.krocodl.demo.imapfollowupservice.mocks.MockedMailServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

public class NotifyServiceTest extends AbstractServiceTest {

    private static final int NOTIFICATIONS_COUNT = 37;

    @Autowired
    private NotifyService notifyService;

    @Autowired
    private NotifyRepository notifyRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MockedMailServer mailServer;

    @Autowired
    private TransactionalService transactionalService;

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createNotificationsTest() {
        List<NotifyEntity> list = new ArrayList<>();
        list.add(createNotifyEntity(1));
        list.add(createNotifyEntity(2));
        list.add(createNotifyEntity(3));

        notifyService.addNotificationsToQueue(list);
        assertThat(notifyRepository.findAll()).hasSize(3);
        notifyRepository.findAll().forEach(notify -> {
            assertThat(notify.getPartitionId()).isNotNull();
            assertThat(notify.isWasSent()).isFalse();
        });

        long id1 = (Long) entityManager.createQuery("select n.id from NotifyEntity n where n.sourceUid=?1").setParameter(1, "s1").getSingleResult();
        entityManager.createQuery("update NotifyEntity n set n.wasSent = TRUE where n.sourceUid=?1").setParameter(1, "s2").executeUpdate();
        notifyService.compensateBrokenSendingTransaction(Collections.singletonList(id1));
        assertThat(notifyRepository.findAll()).hasSize(2);
        notifyRepository.findAll().forEach(notify ->
                assertThat(notify.isWasSent()).isFalse()
        );

        notifyService.deleteNotificationsForMatchedMails(Collections.singletonList("s2"));
        assertThat(notifyRepository.findAll()).hasSize(1);

        assertThat(notifyRepository.findAll().get(0).getSourceUid()).isEqualTo("s3");
    }

    @Test
    public void sendNotificationsFromQueueTest() {
        List<NotifyEntity> list = new ArrayList<>();
        LongStream.range(0, NOTIFICATIONS_COUNT).forEach(n -> list.add(createNotifyEntity(n)));

        transactionalService.executeInNewTransaction(() -> notifyService.addNotificationsToQueue(list), "add notifications");
        assertThat(notifyRepository.findAll()).hasSize(NOTIFICATIONS_COUNT);

        int cnt = mailServer.getServer().getReceivedMessages().length;
        assertThat(notifyService.sendNotificationsFromQueue()).isEqualTo(NOTIFICATIONS_COUNT);
        assertThat(notifyRepository.findAll()).isEmpty();
        assertThat(mailServer.getServer().getReceivedMessages().length).isEqualTo(cnt + NOTIFICATIONS_COUNT);

    }

    private NotifyEntity createNotifyEntity(long n) {
        return new NotifyEntity().withSourceUid("s" + n).withText("text" + n).withSubject("subj" + n).withTo("to" + n);
    }
}
