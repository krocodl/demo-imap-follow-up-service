package org.krocodl.demo.imapfollowupservice.notifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

import static org.krocodl.demo.imapfollowupservice.extractor.ImapMailReceiver.IMAP_PASSWORD;
import static org.krocodl.demo.imapfollowupservice.extractor.ImapMailReceiver.IMAP_USERNAME;
import static org.krocodl.demo.imapfollowupservice.extractor.ImapMailReceiver.SMTP_PORT;
import static org.krocodl.demo.imapfollowupservice.extractor.ImapMailReceiver.SMTP_SERVER;

@Service
public class SmtpMailSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmtpMailSender.class);


    @Value("${" + SMTP_SERVER + "}")
    private String smtpServer;
    @Value("${" + SMTP_PORT + "}")
    private int smtpPort;
    @Value("${" + IMAP_USERNAME + "}")
    private String username;
    @Value("${" + IMAP_PASSWORD + "}")
    private String password;


    private Session emailSession;

    @PostConstruct
    public void postConstruct() {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", smtpServer);
        properties.put("mail.smtp.port", smtpPort);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");

        emailSession = Session.getInstance(properties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
    }

    public MimeMessage createMimeMessage() {
        return new MimeMessage(emailSession);
    }

    public MimeMessage sendMail(ComposeMailAction action, String desc) {
        MimeMessage mail = createMimeMessage();
        try {
            action.execute(mail);
            Transport.send(mail);
            return mail;
        } catch (Exception ex) {
            throw new RuntimeException("Can't sent mail:" + desc, ex);
        }
    }
}
