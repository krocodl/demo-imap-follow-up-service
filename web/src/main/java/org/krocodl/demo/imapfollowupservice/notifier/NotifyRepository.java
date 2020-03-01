package org.krocodl.demo.imapfollowupservice.notifier;

import org.krocodl.demo.imapfollowupservice.common.datamodel.NotifyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotifyRepository extends JpaRepository<NotifyEntity, Long> {

    @Modifying
    @Query("update NotifyEntity n set n.partitionId = MOD(n.id,?1)")
    int updateAllParttions(long count);

    @Modifying
    @Query("update NotifyEntity n set n.partitionId = MOD(n.id,?1) where n.partitionId is null")
    int updateUnpartitioned(long count);

    //@FIXME on Oracle with more then 1000 mail will be error, use special dialect
    @Modifying
    @Query("delete from NotifyEntity n where n.id in ?1")
    int removeMessagesWithIds(List<Long> sentQueueIds);

    //@FIXME on Oracle with more then 1000 mail will be error, use special dialect
    @Modifying
    @Query("delete from NotifyEntity n where n.sourceUid in ?1")
    int removeMatchedMessages(List<String> sentQueueIds);

    @Query("select n from NotifyEntity n where n.wasSent = FALSE and n.partitionId = ?1")
    List<NotifyEntity> queryForSending(long partitionId);

    //@FIXME on Oracle with more then 1000 mail will be error, use special dialect
    @Modifying
    @Query("update NotifyEntity n set n.wasSent = TRUE where n.id in ?1")
    int markAsSent(List<Long> ids);

    @Modifying
    @Query("update NotifyEntity n set n.wasSent = FALSE")
    int marAllkAsNotSent();
}
