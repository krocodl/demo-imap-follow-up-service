package org.krocodl.demo.imapfollowupservice.extractor;

import org.apache.commons.lang3.tuple.Pair;
import org.krocodl.demo.imapfollowupservice.common.utils.MailUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.mail.Folder;
import javax.mail.FolderNotFoundException;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.ReceivedDateTerm;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.krocodl.demo.imapfollowupservice.common.utils.MailUtils.IMAPS;

@Service
public class ImapMailReceiver {

    public static final String IMAP_USERNAME = "imap.username";
    public static final String IMAP_PASSWORD = "imap.password";
    public static final String IMAP_SERVER = "imap.server";
    public static final String IMAP_PORT = "imap.port";
    public static final String IMAP_INBOX_NAME = "imap.inbox";
    public static final String IMAP_OUTBOX_NAME = "imap.outbox";
    public static final String SMTP_SERVER = "smtp.server";
    public static final String SMTP_PORT = "smtp.port";

    private static final Logger LOGGER = LoggerFactory.getLogger(ImapMailReceiver.class);

    @Value("${" + IMAP_USERNAME + "}")
    private String username;
    @Value("${" + IMAP_PASSWORD + "}")
    private String password;
    @Value("${" + IMAP_SERVER + "}")
    private String imapServer;
    @Value("${" + IMAP_PORT + "}")
    private int imapPort;
    @Value("${" + IMAP_INBOX_NAME + "}")
    private String inboxName;
    @Value("${" + IMAP_OUTBOX_NAME + "}")
    private String outboxName;

    private Session emailSession;

    private static Date getMessageReceivedDate(Message msg) {
        try {
            return msg.getReceivedDate();
        } catch (Exception ex) {
            throw new ImapTransportException("Can't get date from message", ex);
        }
    }

    @PostConstruct
    public void postConstruct() {
        Properties properties = new Properties();
        properties.put("mail.imap.host", imapServer);
        properties.put("mail.imap.port", imapPort);
        properties.put("mail.store.protocol", IMAPS);

        emailSession = Session.getInstance(properties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
        Store store = getConnectedStore();


        try {
            LOGGER.info("{} count = {} ", inboxName, getMessagesCount(inboxName));
            LOGGER.info("{} count = {} ", outboxName, getMessagesCount(outboxName));
        } catch (Exception ex) {
            if (ex.getCause() instanceof FolderNotFoundException) {
                LOGGER.error("Only {} folders were found", getFolders());
            }
            throw new ImapTransportException("Can't validate access", ex);
        } finally {
            MailUtils.closeNoEx(store, IMAPS);
        }
    }

    public int getMessagesCount(String boxName) throws Exception {
        return doWithStore(boxName, Folder::getMessageCount, "receiving count in " + boxName);
    }

    private Store getConnectedStore() {
        try {
            Store store = emailSession.getStore(IMAPS);
            store.connect(imapServer, imapPort, username, password);
            return store;
        } catch (Exception ex) {
            throw new ImapTransportException("Can't validate access", ex);
        }
    }

    Pair<List<ExtractedMessage>, List<ExtractedMessage>> getReceivedSentMessages(Date receivedAfter, Date sentAfter) {
        Pair<List<ExtractedMessage>, List<ExtractedMessage>> ret = Pair.of(new ArrayList<>(), new ArrayList<>());

        ret.getLeft().addAll(doWithStore(
                inboxName,
                folder -> Stream.of(folder.search(new ReceivedDateTerm(ComparisonTerm.GE, receivedAfter))). // IMAP supports only day accuracy search
                        filter(m -> getMessageReceivedDate(m).compareTo(receivedAfter) >= 0). // time is stored to the nearest second
                        map(ExtractedMessage::new).collect(Collectors.toList()),
                "receiving new mails"));
        ret.getRight().addAll(doWithStore(
                outboxName,
                folder -> Stream.of(folder.search(new ReceivedDateTerm(ComparisonTerm.GE, sentAfter))). // IMAP supports only day accuracy search
                        filter(m -> getMessageReceivedDate(m).compareTo(sentAfter) >= 0). // time is stored to the nearest second
                        map(ExtractedMessage::new).collect(Collectors.toList()),
                "receiving sent mails"));

        return ret;
    }

    private List<String> getFolders() {
        Store store = getConnectedStore();
        try {
            return Stream.of(store.getDefaultFolder().list("*")).map(Folder::getName).collect(Collectors.toList());
        } catch (Exception ex) {
            throw new ImapTransportException("Can't get list of folders", ex);
        } finally {
            MailUtils.closeNoEx(store, IMAPS);
        }
    }

    public <R> R doWithStore(String folderName, FolderAction<R> action, String desc) {
        Folder folder = null;
        Store store = getConnectedStore();
        try {
            folder = store.getFolder(folderName);
            folder.open(Folder.READ_WRITE);
            return action.execute(folder);
        } catch (Exception ex) {
            throw new ImapTransportException("Can't perform '" + desc + "' with folder " + folderName, ex);
        } finally {
            MailUtils.closeNoEx(folder, folderName);
            MailUtils.closeNoEx(store, IMAPS);
        }
    }

    public String getInboxName() {
        return inboxName;
    }

    public String getOutboxName() {
        return outboxName;
    }


}
