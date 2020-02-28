package org.krocodl.demo.imapfollowupservice.analiser;

import org.krocodl.demo.imapfollowupservice.common.datamodel.IncomingMailDto;

import java.util.List;

public interface MailsMatchingStrategy {

    // returns ids of mails, for which parameter is response
    List<String> makeMatch(IncomingMailDto mail);
}
