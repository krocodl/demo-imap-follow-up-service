package org.krocodl.demo.imapfollowupservice.analiser;

import org.apache.commons.lang3.StringUtils;
import org.krocodl.demo.imapfollowupservice.common.datamodel.IncomingMailDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SimplestMailsMatchingStrategy implements MailsMatchingStrategy {

    private final OutcomingMailRepository repository;

    public SimplestMailsMatchingStrategy(final OutcomingMailRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<String> makeMatch(final IncomingMailDto mail) {
        String srcSubject = StringUtils.removeStart(mail.getSubject(), "RE:").trim();
        srcSubject = StringUtils.removeStart(srcSubject, "re:");
        return repository.matchWithReplyToOrSubject(mail.getInReplyTo(), "%" + srcSubject + "%");
    }
}
