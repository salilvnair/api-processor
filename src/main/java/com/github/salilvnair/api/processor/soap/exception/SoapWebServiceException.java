package com.github.salilvnair.api.processor.soap.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.ws.FaultAwareWebServiceMessage;

@Getter
public class SoapWebServiceException extends Exception {
    @Setter
    private String webserviceName;
    private FaultAwareWebServiceMessage faultAwareWebServiceMessage;
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

    public SoapWebServiceException(Throwable cause, String webserviceName, FaultAwareWebServiceMessage faultAwareWebServiceMessage) {
        super(cause);
        this.webserviceName = webserviceName;
        this.faultAwareWebServiceMessage = faultAwareWebServiceMessage;
    }

    public SoapWebServiceException(Throwable cause, String webserviceName, String faultString) {
        super(cause);
        this.webserviceName = webserviceName;
        this.faultString = faultString;
    }

}
