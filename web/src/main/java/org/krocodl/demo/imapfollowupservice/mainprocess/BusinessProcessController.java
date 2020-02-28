package org.krocodl.demo.imapfollowupservice.mainprocess;


import org.krocodl.demo.imapfollowupservice.common.utils.AbstractServiceController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

@RestController
public class BusinessProcessController extends AbstractServiceController {

    private static final String API_EXECUTE = "/api/execute";


    private final BusinessProcessService service;

    public BusinessProcessController(final BusinessProcessService service) {
        this.service = service;
    }

    @PostConstruct
    public void postConstruct() {
        printEndPointPostInfo(API_EXECUTE, "starting process");
        printEndPointGetInfo(API_EXECUTE, "getting queue information");
    }

    @RequestMapping(value = API_EXECUTE, method = RequestMethod.POST)
    public boolean executeProcess() {
        return service.executeBusinessProcess();
    }

    @RequestMapping(value = API_EXECUTE, method = RequestMethod.GET)
    public String getQueueInfo() {
        return service.getManuallySchedulledTime();
    }

}
