package com.github.salilvnair.api.processor.soap.callback;

import com.github.salilvnair.api.processor.soap.delegate.SoapWebServiceDelegate;
import com.github.salilvnair.api.processor.soap.log.SoapWebServiceLogger;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.core.WebServiceMessageExtractor;
import org.springframework.ws.support.MarshallingUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class SoapWebServiceResponseCallback<T> implements WebServiceMessageExtractor<T> {

    private final SoapWebServiceDelegate delegate;

    private final Map<String, Object> wsParamsMap;

    public SoapWebServiceResponseCallback(SoapWebServiceDelegate delegate, Map<String, Object> wsParamsMap) {
        this.delegate = delegate;
        this.wsParamsMap = wsParamsMap;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T extractData(WebServiceMessage webServiceMessage) throws IOException {
        Map<String, Object> responseHeaders = SoapWebServiceDelegate.extractHeaders(webServiceMessage);
        wsParamsMap.put(SoapWebServiceDelegate.SOAP_RESPONSE_HEADERS, responseHeaders);
        delegate.doMoreWithResponse(webServiceMessage, wsParamsMap);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        webServiceMessage.writeTo(byteArrayOutputStream);
        String responseString =  byteArrayOutputStream.toString();
        wsParamsMap.put(SoapWebServiceDelegate.SOAP_RESPONSE, responseString);
        wsParamsMap.put(SoapWebServiceDelegate.RESPONSE_STATUS_CODE, 200);
        if(delegate.printLogs()) {
            SoapWebServiceLogger.printLogs(responseString, delegate, SoapWebServiceDelegate.LOG_TYPE_RESPONSE);
        }
        Object object = null;
        try {
            object = MarshallingUtils.unmarshal(delegate.webServiceTemplate().getUnmarshaller(), webServiceMessage);
        }
        catch (Exception ex){
            wsParamsMap.put(SoapWebServiceDelegate.SOAP_RESPONSE_HEADERS, responseHeaders);
            wsParamsMap.put(SoapWebServiceDelegate.SOAP_ERROR_STRING, responseString);
            throw new RuntimeException(ex);
        }
        return (T)object ;
    }
}
