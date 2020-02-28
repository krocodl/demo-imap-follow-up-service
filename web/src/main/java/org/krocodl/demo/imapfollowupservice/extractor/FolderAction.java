package org.krocodl.demo.imapfollowupservice.extractor;

import javax.mail.Folder;

@FunctionalInterface
public interface FolderAction<R> {

    R execute(Folder folder) throws Exception;
}
