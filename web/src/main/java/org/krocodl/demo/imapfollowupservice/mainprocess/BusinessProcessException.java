package org.krocodl.demo.imapfollowupservice.mainprocess;

class BusinessProcessException extends RuntimeException {

    BusinessProcessException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
