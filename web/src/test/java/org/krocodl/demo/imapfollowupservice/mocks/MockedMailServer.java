package org.krocodl.demo.imapfollowupservice.mocks;

import com.icegreen.greenmail.util.DummySSLSocketFactory;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.krocodl.demo.imapfollowupservice.common.TestTools;
import org.krocodl.demo.imapfollowupservice.common.utils.MailUtils;
import org.krocodl.demo.imapfollowupservice.extractor.ImapMailReceiver;
import org.krocodl.demo.imapfollowupservice.extractor.ImapTransportException;
import org.krocodl.demo.imapfollowupservice.notifier.ComposeMailAction;
import org.krocodl.demo.imapfollowupservice.notifier.SmtpMailSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;

import javax.annotation.PreDestroy;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
import java.security.Security;

import static org.krocodl.demo.imapfollowupservice.common.utils.MailUtils.IMAPS;
import static org.krocodl.demo.imapfollowupservice.common.utils.MailUtils.SMTP;

@TestConfiguration
public class MockedMailServer {

    private static GreenMail server;
    private static int freeImapPort = TestTools.findRandomOpenPortOnAllLocalInterfaces();
    private static int freeSmtpPort = freeImapPort + 1;
    private static String outFolderName;
    private static String inFolderName;


    static {
        Security.setProperty("ssl.SocketFactory.provider", DummySSLSocketFactory.class.getName());
        ServerSetup imapSetup = new ServerSetup(freeImapPort, null, IMAPS);
        imapSetup.setServerStartupTimeout(3000);
        ServerSetup smtpSetup = new ServerSetup(freeSmtpPort, null, SMTP);
        smtpSetup.setServerStartupTimeout(3000);
        server = new GreenMail(new ServerSetup[]{imapSetup, smtpSetup});

        server.setUser(
                TestTools.getTestProperties().getProperty(ImapMailReceiver.IMAP_USERNAME),
                TestTools.getTestProperties().getProperty(ImapMailReceiver.IMAP_USERNAME),
                TestTools.getTestProperties().getProperty(ImapMailReceiver.IMAP_PASSWORD));
        server.start();

        Store store = null;
        try {
            store = server.getImaps().createStore();
            store.connect(
                    TestTools.getTestProperties().getProperty(ImapMailReceiver.IMAP_SERVER),
                    freeImapPort,
                    TestTools.getTestProperties().getProperty(ImapMailReceiver.IMAP_USERNAME),
                    TestTools.getTestProperties().getProperty(ImapMailReceiver.IMAP_PASSWORD)
            );
            outFolderName = TestTools.getTestProperties().getProperty(ImapMailReceiver.IMAP_OUTBOX_NAME);
            inFolderName = TestTools.getTestProperties().getProperty(ImapMailReceiver.IMAP_INBOX_NAME);
            Folder folder = store.getDefaultFolder().getFolder(outFolderName);
            if (!folder.create(Folder.HOLDS_MESSAGES)) {
                throw new IllegalStateException("Cna't create " + outFolderName);
            }
        } catch (Exception ex) {
            throw new ImapTransportException("can't ini test mail server", ex);
        } finally {
            MailUtils.closeNoEx(store, "test store");
        }

        System.setProperty(ImapMailReceiver.IMAP_SERVER, TestTools.getTestProperties().getProperty(ImapMailReceiver.IMAP_SERVER));
        System.setProperty(ImapMailReceiver.IMAP_PORT, "" + freeImapPort);
        System.setProperty(ImapMailReceiver.SMTP_SERVER, TestTools.getTestProperties().getProperty(ImapMailReceiver.SMTP_SERVER));
        System.setProperty(ImapMailReceiver.SMTP_PORT, "" + freeSmtpPort);
    }

    @Autowired
    private ImapMailReceiver imapMailReceiver;

    @Autowired
    private SmtpMailSender smtpMailSender;


    public MimeMessage sendMessage(ComposeMailAction action, String desc) throws Exception {
        MimeMessage mimeMessage = smtpMailSender.sendMail(action, desc);
        putToOutbox(mimeMessage);
        return mimeMessage;
    }

    public void putToOutbox(MimeMessage mimeMessage) throws Exception {
        imapMailReceiver.doWithFolder(outFolderName, folder -> {
            folder.appendMessages(new Message[]{mimeMessage});
            return mimeMessage.getSubject();
        }, mimeMessage.getSubject());
    }

    public void putToInbox(MimeMessage mimeMessage) throws Exception {
        imapMailReceiver.doWithFolder(inFolderName, folder -> {
            folder.appendMessages(new Message[]{mimeMessage});
            return mimeMessage.getSubject();
        }, mimeMessage.getSubject());
    }

    public void clean() throws Exception {
        server.purgeEmailFromAllMailboxes();
    }


    @PreDestroy
    public void preDestroy() {
        server.stop();
    }

    public GreenMail getServer() {
        return server;
    }
}
