package org.krocodl.demo.imapfollowupservice.extractor;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.krocodl.demo.imapfollowupservice.common.AbstractServiceTest;
import org.krocodl.demo.imapfollowupservice.common.services.DateService;
import org.krocodl.demo.imapfollowupservice.mocks.MockedMailServer;
import org.krocodl.demo.imapfollowupservice.notifier.SmtpMailSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.krocodl.demo.imapfollowupservice.extractor.ImapMailReceiver.IMAP_USERNAME;

public class ImapMailReceiverTest extends AbstractServiceTest {

    @Value("${" + IMAP_USERNAME + "}")
    private String username;

    @Autowired
    private ImapMailReceiver imapMailReceiver;

    @Autowired
    private SmtpMailSender smtpMailSender;

    @Autowired
    private DateService dateService;

    @Autowired
    private MockedMailServer mailServer;

    @Test
    public void getReceivedSentMessagesTest() throws Exception {

        Date date1 = new Date();
        Thread.sleep(1000);
        mailServer.putToInbox(createMail());
        mailServer.putToOutbox(createMail());

        Date date2 = new Date();
        Thread.sleep(1000);
        mailServer.putToInbox(createMail());
        mailServer.putToOutbox(createMail());

        Date date3 = new Date();

        Pair<List<ExtractedMessage>, List<ExtractedMessage>> content = imapMailReceiver.getReceivedSentMessages(date3, date3);
        assertThat(content.getLeft()).hasSize(0);
        assertThat(content.getRight()).hasSize(0);

        content = imapMailReceiver.getReceivedSentMessages(date2, date2);
        assertThat(content.getLeft()).hasSize(1);
        assertThat(content.getRight()).hasSize(1);

        content = imapMailReceiver.getReceivedSentMessages(date1, date1);
        assertThat(content.getLeft()).hasSize(2);
        assertThat(content.getRight()).hasSize(2);
    }

    private MimeMessage createMail() throws Exception {
        MimeMessage mail = smtpMailSender.createMimeMessage();
        mail.setFrom(username);
        mail.setSentDate(dateService.nowDate());
        mail.setRecipients(Message.RecipientType.TO, username);
        mail.setSubject("subject");
        mail.setText("test");
        return mail;
    }

}
