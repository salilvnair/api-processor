package com.github.salilvnair.api.processor.soap.facade;

import com.github.salilvnair.api.processor.helper.retry.RetryExecutor;
import com.github.salilvnair.api.processor.soap.delegate.SoapWebServiceDelegate;
import com.github.salilvnair.api.processor.soap.exception.SoapWebServiceException;
import com.github.salilvnair.api.processor.soap.handler.SoapWebServiceHandler;
import com.github.salilvnair.api.processor.soap.model.SoapWebServiceRequest;
import com.github.salilvnair.api.processor.soap.model.SoapWebServiceResponse;
import java.util.Map;


public class SoapWebServiceFacade {
    public SoapWebServiceResponse initiateWebService(SoapWebServiceHandler handler, Map<String, Object> methodParamMap, Object... objects) throws SoapWebServiceException {
        if (handler == null){
            throw new SoapWebServiceException("Cannot initiate webservice call without a proper handler class");
        }
        SoapWebServiceRequest requestWrapper = handler.prepareRequestBody(methodParamMap, objects);
        SoapWebServiceDelegate delegate = handler.delegate();
        SoapWebServiceResponse responseWrapper;
        if(delegate == null) {
            throw new SoapWebServiceException("Cannot initiate webservice call without a proper client delegate bean for the handler");
        }
        if (delegate.retry()) {
            try {
                responseWrapper = (new RetryExecutor()).maxRetries(delegate.maxRetries()).delay(delegate.delay(), delegate.delayTimeUnit()).configure(delegate.whiteListedExceptions()).execute(() -> {
                    try {
                        return delegate.invoke(requestWrapper, methodParamMap, objects);
                    }
                    catch (SoapWebServiceException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            catch (Exception ex) {
                throw new SoapWebServiceException(ex, delegate.webServiceName());
            }
        }
        else {
            responseWrapper = delegate.invoke(requestWrapper, methodParamMap, objects);
        }
        handler.processResponse(requestWrapper, responseWrapper, methodParamMap, objects);
        return  responseWrapper;
    }
}
