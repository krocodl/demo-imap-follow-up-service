package org.krocodl.demo.imapfollowupservice.common.services;


import org.krocodl.demo.imapfollowupservice.common.utils.AbstractServiceController;
import org.krocodl.demo.imapfollowupservice.common.utils.DateFormater;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.List;

@RestController
public class ServiceStateController extends AbstractServiceController {

    private static final String API_EXECUTE = "/api/state";


    private final ServiceStateService repository;

    private final DateService dateService;

    public ServiceStateController(final ServiceStateService repository, final DateService dateService) {
        this.repository = repository;
        this.dateService = dateService;
    }

    @PostConstruct
    public void postConstruct() {
        printEndPointGetInfo(API_EXECUTE, "getting state of the service");
    }

    @RequestMapping(value = API_EXECUTE, method = RequestMethod.GET)
    public List<ServiceStateEntity> getQueueInfo() {
        List<ServiceStateEntity> ret = repository.findAll();
        ret.add(new ServiceStateEntity().withId((long) ret.size() + 1).withName("nowDate").withValue(DateFormater.formatTime(dateService.nowDate())));
        return ret;
    }

}
