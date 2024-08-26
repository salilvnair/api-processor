package com.github.salilvnair.api.processor.rest.exception;

public class RestWebServiceException extends RuntimeException {
    private String webserviceName;

    public RestWebServiceException() {
        super();
    }

    public RestWebServiceException(String message, Throwable cause, String webserviceName) {
        super(message, cause);
        this.webserviceName = webserviceName;
    }

    public RestWebServiceException(String message) {
        super(message);
    }

    public RestWebServiceException(String message, String webserviceName) {
        super(message);
        this.webserviceName = webserviceName;
    }

    public RestWebServiceException(Throwable cause) {
        super(cause);
    }

    public RestWebServiceException(Throwable cause, String webserviceName) {
        super(cause);
        this.webserviceName = webserviceName;
    }
}
