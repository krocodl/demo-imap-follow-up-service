package org.krocodl.demo.imapfollowupservice.notifier;

class NotifyException extends RuntimeException {

    NotifyException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
