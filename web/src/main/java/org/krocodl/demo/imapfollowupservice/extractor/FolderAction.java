package org.krocodl.demo.imapfollowupservice.extractor;

import com.sun.mail.imap.IMAPFolder;

@FunctionalInterface
public interface FolderAction<R> {

    R execute(IMAPFolder folder) throws Exception;
}
