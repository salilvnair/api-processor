package com.github.salilvnair.api.processor.soap.delegate;

import com.github.salilvnair.api.processor.soap.callback.SoapWebServiceRequestCallback;
import com.github.salilvnair.api.processor.soap.callback.SoapWebServiceResponseCallback;
import com.github.salilvnair.api.processor.soap.exception.SoapWebServiceException;
import com.github.salilvnair.api.processor.soap.model.SoapWebServiceRequest;
import com.github.salilvnair.api.processor.soap.model.SoapWebServiceResponse;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.FaultAwareWebServiceMessage;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;
import org.springframework.ws.soap.saaj.SaajSoapMessage;
import org.springframework.ws.support.MarshallingUtils;

import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public interface SoapWebServiceDelegate {

    String LOG_TYPE_REQUEST = "REQUEST";
    String LOG_TYPE_RESPONSE = "RESPONSE";
    String SOAP_REQUEST = "SOAP_REQUEST";
    String SOAP_RESPONSE = "SOAP_RESPONSE";
    String SOAP_REQUEST_HEADERS = "SOAP_REQUEST_HEADERS";
    String SOAP_RESPONSE_HEADERS = "SOAP_RESPONSE_HEADERS";

    String SOAP_ERROR_STRING = "SOAP_ERROR_STRING";
    String RESPONSE_STATUS_CODE = "RESPONSE_STATUS_CODE";

    default void addHeaders(SOAPHeader soapHeader, Map<String, Object> wsParamsMap) throws SOAPException {};

    WebServiceTemplate webServiceTemplate();

    default void doMoreWithRequest(WebServiceMessage webServiceMessage, Map<String, Object> wsParamsMap) {};
    default void doMoreWithResponse(WebServiceMessage webServiceMessage, Map<String, Object> wsParamsMap) {};

    SoapWebServiceResponse invoke(SoapWebServiceRequest requestWrapper, Map<String, Object> wsParamsMap, Object... objects) throws SoapWebServiceException;

    default boolean retry() {
        return false;
    }

    default List<String> whiteListedExceptions() {
        return Collections.emptyList();
    }

    default int delay() {
        return 0;
    }

    default int maxRetries() {
        return 0;
    }

    default TimeUnit delayTimeUnit() {
        return TimeUnit.MINUTES;
    }

    String webServiceName();

    default boolean printLogs() {
        return true;
    }

    default <T> T marshalSendAndReceive(SoapWebServiceRequest requestWrapper, Map<String, Object> wsParamsMap) throws SoapWebServiceException {
        return marshalSendAndReceive(requestWrapper, wsParamsMap, new SoapWebServiceRequestCallback(this, wsParamsMap), new SoapWebServiceResponseCallback<>(this, wsParamsMap));
    }

    default <T> T marshalSendAndReceive(SoapWebServiceRequest requestWrapper, Map<String, Object> wsParamsMap,  SoapWebServiceRequestCallback requestCallback, SoapWebServiceResponseCallback<T> responseExtractor) throws SoapWebServiceException {
        T t;
        try {
            t = webServiceTemplate().sendAndReceive(webServiceMessage -> {
                if (requestWrapper != null) {
                    Marshaller marshaller = webServiceTemplate().getMarshaller();
                    if (marshaller == null) {
                        throw new IllegalStateException("No marshaller registered. Check configuration of WebServiceTemplate.");
                    }

                    MarshallingUtils.marshal(marshaller, requestWrapper.request(), webServiceMessage);
                    requestCallback.doWithMessage(webServiceMessage);
                }

            }, responseExtractor);
        }
        catch (Exception ex) {
            if(ex instanceof SoapFaultClientException) {
                FaultAwareWebServiceMessage webServiceMessage = ((SoapFaultClientException) ex).getWebServiceMessage();
                Map<String, Object> responseHeaders = extractHeaders(webServiceMessage);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                try {
                    webServiceMessage.writeTo(byteArrayOutputStream);
                }
                catch (IOException ignore) {}
                String faultString =  byteArrayOutputStream.toString();
                if(wsParamsMap!=null) {
                    wsParamsMap.put(SOAP_RESPONSE_HEADERS, responseHeaders);
                    wsParamsMap.put(SOAP_ERROR_STRING, faultString);
                    wsParamsMap.put(SoapWebServiceDelegate.RESPONSE_STATUS_CODE, 500);
                }
                throw new SoapWebServiceException(ex, webServiceName(), faultString);
            }
            if(wsParamsMap!=null && wsParamsMap.containsKey(SOAP_ERROR_STRING)) {
                throw new SoapWebServiceException(ex, webServiceName(), String.valueOf(wsParamsMap.get(SOAP_ERROR_STRING)));
            }
            else {
                throw new SoapWebServiceException(ex, webServiceName());
            }

        }
        return t;
    }

    static Map<String, Object> extractHeaders(WebServiceMessage webServiceMessage) {
        Map<String, Object> headers = new HashMap<>();
        MimeHeaders mimeHeaders = ((SaajSoapMessage) webServiceMessage).getSaajMessage().getMimeHeaders();
        if(mimeHeaders != null) {
            Iterator<?> allMimeHeaders = mimeHeaders.getAllHeaders();
            while (allMimeHeaders.hasNext()) {
                MimeHeader mimeHeader = (MimeHeader) allMimeHeaders.next();
                headers.put(mimeHeader.getName(), mimeHeader.getValue());
            }
        }
        return headers;
    }

    static WebServiceTemplate webServiceTemplate(String contextPath, String endpointUrl) {
        Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
        jaxb2Marshaller.setContextPath(contextPath);
        WebServiceTemplate webServiceTemplate = new WebServiceTemplate();
        webServiceTemplate.setMarshaller(jaxb2Marshaller);
        webServiceTemplate.setUnmarshaller(jaxb2Marshaller);
        webServiceTemplate.setDefaultUri(endpointUrl);
        return webServiceTemplate;
    }
}
