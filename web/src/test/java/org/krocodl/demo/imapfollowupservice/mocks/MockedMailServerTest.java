package org.krocodl.demo.imapfollowupservice.mocks;

import org.junit.Test;
import org.krocodl.demo.imapfollowupservice.common.AbstractServiceTest;
import org.krocodl.demo.imapfollowupservice.extractor.ImapMailReceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.mail.Message;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.krocodl.demo.imapfollowupservice.extractor.ImapMailReceiver.IMAP_USERNAME;

public class MockedMailServerTest extends AbstractServiceTest {

    @Value("${" + IMAP_USERNAME + "}")
    private String username;

    @Autowired
    private MockedMailServer mailServer;

    @Autowired
    private ImapMailReceiver imapMailReceiver;

    @Test
    public void testSendingMail() throws Exception {
        mailServer.sendMessage(mimeMessage -> {
            mimeMessage.setFrom(username);
            mimeMessage.setSentDate(new Date());
            mimeMessage.setRecipients(Message.RecipientType.TO, username);
            mimeMessage.setSubject("subject");
            mimeMessage.setText("test");
        }, "test messages");

        assertThat(mailServer.getServer().getReceivedMessages()).hasSize(2);
        assertThat(imapMailReceiver.getMessagesCount(imapMailReceiver.getInboxName())).isEqualTo(1);
        assertThat(imapMailReceiver.getMessagesCount(imapMailReceiver.getOutboxName())).isEqualTo(1);

        mailServer.clean();
        assertThat(mailServer.getServer().getReceivedMessages()).hasSize(0);
        assertThat(imapMailReceiver.getMessagesCount(imapMailReceiver.getInboxName())).isEqualTo(0);
        assertThat(imapMailReceiver.getMessagesCount(imapMailReceiver.getOutboxName())).isEqualTo(0);
    }

}
