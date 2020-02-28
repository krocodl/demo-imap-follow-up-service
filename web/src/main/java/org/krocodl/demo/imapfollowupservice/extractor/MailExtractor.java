package org.krocodl.demo.imapfollowupservice.extractor;

import org.krocodl.demo.imapfollowupservice.common.datamodel.BatchOfMails;

public interface MailExtractor {

    BatchOfMails extractMails(int startingFromDaysOffset);
}
