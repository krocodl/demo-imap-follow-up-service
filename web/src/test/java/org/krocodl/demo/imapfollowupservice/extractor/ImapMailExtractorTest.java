package org.krocodl.demo.imapfollowupservice.extractor;

import org.junit.Test;
import org.krocodl.demo.imapfollowupservice.common.AbstractServiceTest;
import org.krocodl.demo.imapfollowupservice.common.datamodel.BatchOfMails;
import org.krocodl.demo.imapfollowupservice.common.services.ServiceStateService;
import org.krocodl.demo.imapfollowupservice.mocks.MockedMailServer;
import org.krocodl.demo.imapfollowupservice.notifier.SmtpMailSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.krocodl.demo.imapfollowupservice.extractor.ImapMailReceiver.IMAP_USERNAME;

public class ImapMailExtractorTest extends AbstractServiceTest {

    @Autowired
    private MockedMailServer mailServer;

    @Autowired
    private MailExtractor mailExtractor;

    @Autowired
    private SmtpMailSender smtpMailSender;

    @Value("${" + IMAP_USERNAME + "}")
    private String username;

    @Autowired
    private ServiceStateService serviceState;

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void extractMailsTest() throws Exception {

        Date date1 = new Date();
        Thread.sleep(1000);

        MimeMessage mailOut = smtpMailSender.createMimeMessage();
        mailOut.setFrom(username);
        mailOut.setSentDate(new Date());
        mailOut.setRecipients(Message.RecipientType.TO, username);
        mailOut.setSubject("subjectOut");
        mailOut.setText("out");
        mailServer.putToOutbox(mailOut);

        MimeMessage mailIn = smtpMailSender.createMimeMessage();
        mailIn.setFrom(username);
        mailIn.setSentDate(new Date());
        mailIn.setRecipients(Message.RecipientType.TO, username);
        mailIn.setSubject("subjectIn");
        mailIn.setText("in");
        mailServer.putToInbox(mailIn);

        Thread.sleep(1000);
        Date date2 = new Date();

        serviceState.setLastReceiveDate(date2);
        serviceState.setLastSendDate(date2);
        BatchOfMails batch = mailExtractor.extractMails(-1);
        assertThat(batch.getIncomingMails()).hasSize(0);
        assertThat(batch.getOutcomingMails()).hasSize(0);
        assertThat(batch.getLastReceiveDate()).isNull();
        assertThat(batch.getLastSendDate()).isNull();

        serviceState.setLastReceiveDate(date1);
        serviceState.setLastSendDate(date1);
        batch = mailExtractor.extractMails(-1);
        assertThat(batch.getIncomingMails()).hasSize(1);
        assertThat(batch.getOutcomingMails()).hasSize(1);
        assertThat(batch.getLastReceiveDate()).isAfter(date1);
        assertThat(batch.getLastSendDate()).isAfter(date1);
    }
}
