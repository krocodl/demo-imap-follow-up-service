package org.krocodl.demo.imapfollowupservice.extractor;

import javax.mail.Store;

@FunctionalInterface
interface StoreAction<R> {
    R execute(Store store) throws Exception;
}
