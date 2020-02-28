package org.krocodl.demo.imapfollowupservice.notifier;

import javax.mail.internet.MimeMessage;

@FunctionalInterface
public interface ComposeMailAction {

    void execute(MimeMessage mail) throws Exception;
}
