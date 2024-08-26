package com.github.salilvnair.api.processor.soap.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
public class SoapWebServiceException extends Exception {
    @Setter
    private String webserviceName;
    private String faultString;
    public SoapWebServiceException(String s) {
        super(s);
    }

    public SoapWebServiceException(Throwable ex) {
        super(ex);
    }

    public SoapWebServiceException(Throwable cause, String webserviceName) {
        super(cause);
        this.webserviceName = webserviceName;
    }

    public SoapWebServiceException(Throwable cause, String webserviceName, String faultString) {
        super(cause);
        this.webserviceName = webserviceName;
        this.faultString = faultString;
    }

}
