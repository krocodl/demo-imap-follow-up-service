package org.krocodl.demo.imapfollowupservice.analiser;

import org.krocodl.demo.imapfollowupservice.common.datamodel.OutcomingMailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface OutcomingMailRepository extends JpaRepository<OutcomingMailEntity, String> {

    @Query("select m.uid from OutcomingMailEntity m where m.uid in ?1")
    List<String> findDuplicates(Collection ids);

    @Query("select m.uid from OutcomingMailEntity m where m.uid = ?1 or m.subject like ?2")
    List<String> matchWithReplyToOrSubject(String uid, String subjectFilter);

    @Modifying
    @Query("delete from OutcomingMailEntity m where m.uid in ?1")
    void removeByIds(List<String> sentQueueIds);

    @Query("select m from OutcomingMailEntity m where m.notifyDate <= ?1")
    List<OutcomingMailEntity> findMailsForNotification(Date now);

}
