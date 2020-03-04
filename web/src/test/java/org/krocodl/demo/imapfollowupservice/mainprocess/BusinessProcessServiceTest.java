package org.krocodl.demo.imapfollowupservice.mainprocess;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.krocodl.demo.imapfollowupservice.extractor.ImapMailReceiver.IMAP_USERNAME;

public class BusinessProcessServiceTest extends AbstractServiceTest {

    public static final String SOME_CORRESPONDENT_MAIL = "some@gmail.com";
    @Autowired
    private SmtpMailSender smtpMailSender;

    @Autowired
    private DateService dateService;

    @Autowired
    private MockedMailServer mailServer;

    @Value("${" + IMAP_USERNAME + "}")
    private String username;

    @Autowired
    private BusinessProcessService businessProcessService;

    @Test
    public void commonUseCaseTest() throws Exception {
        mailServer.putToOutbox(produceMimeMessage(0, SOME_CORRESPONDENT_MAIL, username));
        mailServer.putToOutbox(produceMimeMessage(1, SOME_CORRESPONDENT_MAIL, username));
        mailServer.putToOutbox(produceMimeMessage(2, SOME_CORRESPONDENT_MAIL, username));

        MimeMessage mail = produceMimeMessage(3, username, SOME_CORRESPONDENT_MAIL);
        mail.setSubject("RE: subject0");
        mailServer.putToInbox(mail);

        businessProcessService.executeRawBusinessProcess();
        assertThat(mailServer.getServer().getReceivedMessages()).hasSize(4);

        mail = produceMimeMessage(4, username, SOME_CORRESPONDENT_MAIL);
        mail.setSubject("RE: subject1");
        mailServer.putToInbox(mail);

        dateService.setDaysOffset(4);
        businessProcessService.executeRawBusinessProcess();
        assertThat(mailServer.getServer().getReceivedMessages()).hasSize(5 + 1);

        businessProcessService.executeRawBusinessProcess();
        assertThat(mailServer.getServer().getReceivedMessages()).hasSize(5 + 1);

        dateService.setDaysOffset(12);
        businessProcessService.executeRawBusinessProcess();
        assertThat(mailServer.getServer().getReceivedMessages()).hasSize(5 + 2);
    }

    private MimeMessage produceMimeMessage(int n, String to, String from) throws Exception {
        MimeMessage mail = smtpMailSender.createMimeMessage();
        mail.setFrom(from);
        mail.setSentDate(new Date());
        mail.setRecipients(Message.RecipientType.TO, to);
        mail.setSubject("subject" + n);
        mail.setText("text" + n);
        return mail;
    }

}
