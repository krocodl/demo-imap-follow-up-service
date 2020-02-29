package org.krocodl.demo.imapfollowupservice.extractor;

import javax.mail.Store;

@FunctionalInterface
public interface StoreAction<R> {

    R execute(Store store) throws Exception;
}
