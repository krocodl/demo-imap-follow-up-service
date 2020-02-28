package org.krocodl.demo.imapfollowupservice.common.utils;

import com.sun.mail.util.MailConnectException;

import java.net.SocketTimeoutException;

public final class ExceptionsUtils {

    private static final int MAX_DEPTH = 10;

    private ExceptionsUtils() {
    }

    private static <T> T findException(Throwable throwable, Class<? extends T> clazz, int level) {
        if (clazz.isAssignableFrom(throwable.getClass())) {
            //noinspection unchecked
            return (T) throwable;
        }
        if (level > MAX_DEPTH || throwable.getCause() == null) {
            return null;
        }

        return findException(throwable.getCause(), clazz, level + 1);
    }

    public static boolean isTimeoutException(Throwable throwable) {
        return findException(throwable, SocketTimeoutException.class, 0) != null;
    }

    public static boolean isMailConnectionException(Throwable throwable) {
        return findException(throwable, MailConnectException.class, 0) != null;
    }
}
