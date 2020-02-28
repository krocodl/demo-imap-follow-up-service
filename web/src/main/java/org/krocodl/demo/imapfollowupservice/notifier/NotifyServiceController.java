package org.krocodl.demo.imapfollowupservice.notifier;


import org.krocodl.demo.imapfollowupservice.common.datamodel.NotifyEntity;
import org.krocodl.demo.imapfollowupservice.common.utils.AbstractServiceController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.List;

@RestController
public class NotifyServiceController extends AbstractServiceController {

    private static final String API_EXECUTE = "/api/notifications";

    private final NotifyRepository repository;

    public NotifyServiceController(final NotifyRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void postConstruct() {
        printEndPointGetInfo(API_EXECUTE, "getting list of notifications");
    }

    @RequestMapping(value = API_EXECUTE, method = RequestMethod.GET)
    public List<NotifyEntity> getQueueInfo() {
        return repository.findAll();
    }

}
