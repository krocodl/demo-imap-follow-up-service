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

        MimeMessage mailOut = smtpMailSender.createMimeMessage();
        mailOut.setFrom(username);
        mailOut.setSentDate(new Date());
        mailOut.setRecipients(Message.RecipientType.TO, username);
        mailOut.setSubject("subjectOut1");
        mailOut.setText("out");
        mailServer.putToOutbox(mailOut);

        mailOut = smtpMailSender.createMimeMessage();
        mailOut.setFrom(username);
        mailOut.setSentDate(new Date());
        mailOut.setRecipients(Message.RecipientType.TO, username);
        mailOut.setSubject("subjectOut2");
        mailOut.setText("out");
        mailServer.putToOutbox(mailOut);

        MimeMessage mailIn = smtpMailSender.createMimeMessage();
        mailIn.setFrom(username);
        mailIn.setSentDate(new Date());
        mailIn.setRecipients(Message.RecipientType.TO, username);
        mailIn.setSubject("subjectIn1");
        mailIn.setText("in");
        mailServer.putToInbox(mailIn);

        mailIn = smtpMailSender.createMimeMessage();
        mailIn.setFrom(username);
        mailIn.setSentDate(new Date());
        mailIn.setRecipients(Message.RecipientType.TO, username);
        mailIn.setSubject("subjectIn2");
        mailIn.setText("in");
        mailServer.putToInbox(mailIn);

        BatchOfMails batch = mailExtractor.extractMails(-1);
        assertThat(batch.getIncomingMails()).hasSize(2);
        assertThat(batch.getOutcomingMails()).hasSize(2);
    }
}
