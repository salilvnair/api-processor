package com.github.salilvnair.api.processor.soap.callback;

import com.github.salilvnair.api.processor.soap.delegate.SoapWebServiceDelegate;
import com.github.salilvnair.api.processor.soap.log.SoapWebServiceLogger;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.soap.saaj.SaajSoapMessage;

import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class SoapWebServiceRequestCallback implements WebServiceMessageCallback {
    private final SoapWebServiceDelegate delegate;
    private final Map<String, Object> wsParamsMap;

    public SoapWebServiceRequestCallback(SoapWebServiceDelegate delegate, Map<String, Object> wsParamsMap) {
        this.delegate = delegate;
        this.wsParamsMap = wsParamsMap;
    }

    @Override
    public void doWithMessage(WebServiceMessage webServiceMessage) throws IOException {
        SOAPMessage saajSoapMessage = ((SaajSoapMessage) webServiceMessage).getSaajMessage();
        SOAPEnvelope envelope;
        SOAPHeader soapHeader;
        try {
            envelope = saajSoapMessage.getSOAPPart().getEnvelope();
            soapHeader = saajSoapMessage.getSOAPHeader();
            if(soapHeader == null) {
                envelope.addHeader();
            }
            delegate.addHeaders(soapHeader, wsParamsMap);
        }
        catch (SOAPException e) {
            throw new RuntimeException(e);
        }
        delegate.doMoreWithRequest(webServiceMessage, wsParamsMap);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        webServiceMessage.writeTo(byteArrayOutputStream);
        String requestString =  byteArrayOutputStream.toString();
        wsParamsMap.put(SoapWebServiceDelegate.SOAP_REQUEST, requestString);
        Map<String, Object> requestHeaders = SoapWebServiceDelegate.extractHeaders(webServiceMessage);
        wsParamsMap.put(SoapWebServiceDelegate.SOAP_REQUEST_HEADERS, requestHeaders);
        if(delegate.printLogs()) {
            SoapWebServiceLogger.printLogs(requestString, delegate, SoapWebServiceDelegate.LOG_TYPE_REQUEST);
        }
    }

    public  Map<String, Object> wsParamsMap() {
        return wsParamsMap;
    }
}
