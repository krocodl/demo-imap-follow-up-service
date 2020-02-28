package org.krocodl.demo.imapfollowupservice.common.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public final class MailUtils {

    public static final String IMAPS = "imaps";
    public static final String SMTP = "smtp";
    private static final Logger LOGGER = LoggerFactory.getLogger(MailUtils.class);


    private MailUtils() {
    }


    public static void closeNoEx(Folder folder, String message) {
        if (folder != null) {
            try {
                if (folder.isOpen()) {
                    folder.close(false);
                }
            } catch (Exception ex) {
                LOGGER.error("Can't close folder {}", message, ex);
            }
        }
    }

    public static void closeNoEx(Store store, String message) {
        if (store != null) {
            try {
                if (store.isConnected()) {
                    store.close();
                }
            } catch (Exception ex) {
                LOGGER.error("Can't close store {}", message, ex);
            }
        }
    }

    public static Map<String, String> getHeaders(Message msg) throws Exception {
        Map<String, String> ret = new HashMap<>();
        Enumeration lines = ((MimeMessage) msg).getAllHeaderLines();
        while (lines.hasMoreElements()) {
            String[] parts = StringUtils.split(lines.nextElement().toString(), ":");
            ret.put(parts[0], parts.length < 2 || parts[1] == null ? "" : parts[1].trim());
        }
        return ret;
    }

}
