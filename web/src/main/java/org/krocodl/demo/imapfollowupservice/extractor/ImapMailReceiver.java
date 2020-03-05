package org.krocodl.demo.imapfollowupservice.extractor;

import com.sun.mail.imap.IMAPFolder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.krocodl.demo.imapfollowupservice.common.services.DateService;
import org.krocodl.demo.imapfollowupservice.common.utils.MailUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.mail.Folder;
import javax.mail.FolderNotFoundException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.ReceivedDateTerm;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
    private static final long MAX_FOLDER_MSG_UID = Long.MAX_VALUE - 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(ImapMailReceiver.class);
    private static final String ID_ATTRIBUTE_OUTBOX_FOLDER = "\\Sent";
    private static final String STD_AUTO_OUTBOX_FOLDER = "auto";
    private final DateService dateService;
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

    public ImapMailReceiver(final DateService dateService) {
        this.dateService = dateService;
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


        fixAutoOutboxName();
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

    public int getMessagesCount(String boxName) {
        return doWithFolder(boxName, Folder::getMessageCount, "receiving count in " + boxName);
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


    Pair<Long, Long> getInitialMessageUid(int startingFromDaysOffset) {
        Calendar earliestTime = dateService.nowCalendar();
        earliestTime.setTime(DateUtils.truncate(earliestTime.getTime(), Calendar.DATE));
        earliestTime.add(Calendar.DAY_OF_YEAR, startingFromDaysOffset);

        return Pair.of(
                doWithFolder(inboxName, folder -> {
                    Message[] msgs = folder.search(new ReceivedDateTerm(ComparisonTerm.GE, earliestTime.getTime()));
                    return msgs.length == 0 ? 0 : folder.getUID(msgs[0]) - 1;
                }, "Getting first income messages since " + earliestTime.getTime()),
                doWithFolder(outboxName, folder -> {
                    Message[] msgs = folder.search(new ReceivedDateTerm(ComparisonTerm.GE, earliestTime.getTime()));
                    return msgs.length == 0 ? 0 : folder.getUID(msgs[0]) - 1;
                }, "Getting first outcome messages since " + earliestTime.getTime()));
    }

    Pair<List<ExtractedMessage>, List<ExtractedMessage>> getReceivedSentMessages(long receivedAfter, long sentAfter) {
        Pair<List<ExtractedMessage>, List<ExtractedMessage>> ret = Pair.of(new ArrayList<>(), new ArrayList<>());

        ret.getLeft().addAll(doWithFolder(inboxName,
                folder -> Stream.of(folder.getMessagesByUID(receivedAfter + 1, MAX_FOLDER_MSG_UID)).map(msg -> new ExtractedMessage(folder, msg)).collect(Collectors.toList()),
                "receiving new mails"));
        ret.getRight().addAll(doWithFolder(outboxName,
                folder -> Stream.of(folder.getMessagesByUID(sentAfter + 1, MAX_FOLDER_MSG_UID)).map(msg -> new ExtractedMessage(folder, msg)).collect(Collectors.toList()),
                "receiving sent mails"));

        return ret;
    }

    private List<String> getFolders() {
        return doWithStore(store ->
                        Stream.of(store.getDefaultFolder().list("*")).map(folder -> {
                            String attributes;
                            try {
                                attributes = StringUtils.join(((IMAPFolder) folder).getAttributes());
                            } catch (MessagingException ex) {
                                throw new ImapTransportException("can't get attributes of " + folder.getName(), ex);
                            }
                            return folder.getName() + "[" + attributes + "]";
                        }).collect(Collectors.toList())

                , "getting list of folders");
    }

    private void fixAutoOutboxName() {
        if (!STD_AUTO_OUTBOX_FOLDER.equals(outboxName)) {
            return;
        }
        doWithStore(store -> {
            for (Folder folder : store.getDefaultFolder().list("*")) {
                String[] attrs = ((IMAPFolder) folder).getAttributes();
                if (Arrays.asList(attrs).contains(ID_ATTRIBUTE_OUTBOX_FOLDER)) {
                    outboxName = folder.getFullName();
                    break;
                }
            }
            return null;
        }, "guessing outo box name");
    }

    private <R> R doWithStore(StoreAction<R> action, String desc) {
        Store store = getConnectedStore();
        try {
            return action.execute(store);
        } catch (Exception ex) {
            throw new ImapTransportException("Can't perform '" + desc, ex);
        } finally {
            MailUtils.closeNoEx(store, IMAPS);
        }
    }

    public <R> R doWithFolder(String folderName, FolderAction<R> action, String desc) {
        Folder folder = null;
        Store store = getConnectedStore();
        try {
            folder = store.getFolder(folderName);
            folder.open(Folder.READ_WRITE);
            return action.execute((IMAPFolder) folder);
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
