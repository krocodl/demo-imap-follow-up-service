package org.krocodl.demo.imapfollowupservice.analiser;


import org.krocodl.demo.imapfollowupservice.common.datamodel.OutcomingMailEntity;
import org.krocodl.demo.imapfollowupservice.common.utils.AbstractServiceController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.List;

@RestController
public class MailsAnaliserController extends AbstractServiceController {

    private static final String API_EXECUTE = "/api/outcoming";


    private final OutcomingMailRepository repository;

    public MailsAnaliserController(final OutcomingMailRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void postConstruct() {
        printEndPointGetInfo(API_EXECUTE, "getting list of non matched outcoming mails");
    }

    @RequestMapping(value = API_EXECUTE, method = RequestMethod.GET)
    public List<OutcomingMailEntity> getQueueInfo() {
        return repository.findAll();
    }

}
