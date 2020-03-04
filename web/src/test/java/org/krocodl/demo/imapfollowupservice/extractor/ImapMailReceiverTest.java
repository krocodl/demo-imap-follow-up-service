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

    private long getMaxMsgUid(List<ExtractedMessage> msgs) {
        return msgs.stream().map(ExtractedMessage::getMsgUid).max(Long::compareTo).orElse(0L);

    }

    @Test
    public void getReceivedSentMessagesTest() throws Exception {

        Pair<List<ExtractedMessage>, List<ExtractedMessage>> content = imapMailReceiver.getReceivedSentMessages(0, 0);
        assertThat(content.getLeft()).hasSize(0);
        assertThat(content.getRight()).hasSize(0);

        mailServer.putToInbox(createMail());
        mailServer.putToOutbox(createMail());

        content = imapMailReceiver.getReceivedSentMessages(0, 0);
        assertThat(content.getLeft()).hasSize(1);
        assertThat(content.getRight()).hasSize(1);

        long inMax = getMaxMsgUid(content.getLeft());
        long outMax = getMaxMsgUid(content.getLeft());

        content = imapMailReceiver.getReceivedSentMessages(inMax, outMax);
        assertThat(content.getLeft()).hasSize(0);
        assertThat(content.getRight()).hasSize(0);

        mailServer.putToInbox(createMail());
        mailServer.putToOutbox(createMail());

        content = imapMailReceiver.getReceivedSentMessages(0, 0);
        assertThat(content.getLeft()).hasSize(2);
        assertThat(content.getRight()).hasSize(2);

        content = imapMailReceiver.getReceivedSentMessages(inMax, outMax);
        assertThat(content.getLeft()).hasSize(1);
        assertThat(content.getRight()).hasSize(1);

        content = imapMailReceiver.getReceivedSentMessages(inMax + 1, outMax + 1);
        assertThat(content.getLeft()).hasSize(0);
        assertThat(content.getRight()).hasSize(0);
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
