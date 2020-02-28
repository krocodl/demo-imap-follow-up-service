package org.krocodl.demo.imapfollowupservice.common.utils;

import org.slf4j.Logger;

import java.util.Collection;
import java.util.Iterator;

public final class LogUtils {

    private LogUtils() {
    }

    public static void infoCollection(Logger logger, Collection collection, String desc) {
        logger.info("Start of {}", desc);
        Iterator iter = collection.iterator();
        for (int i = 0; i < collection.size(); i++) {
            logger.info("     {}: {}", i, iter.next());
        }
        logger.info("End of {}", desc);
    }
}
