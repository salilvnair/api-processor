package com.github.salilvnair.api.processor.soap.handler;


import com.github.salilvnair.api.processor.soap.delegate.SoapWebServiceDelegate;
import com.github.salilvnair.api.processor.soap.exception.SoapWebServiceException;
import com.github.salilvnair.api.processor.soap.model.SoapWebServiceRequest;
import com.github.salilvnair.api.processor.soap.model.SoapWebServiceResponse;

import java.util.Map;

public interface SoapWebServiceHandler {
    SoapWebServiceDelegate delegate();
    SoapWebServiceRequest prepareRequestBody(Map<String, Object> methodParamMap, Object... objects) throws SoapWebServiceException;
    default void processResponse(SoapWebServiceRequest requestWrapper, SoapWebServiceResponse responseWrapper, Map<String, Object> methodParamMap, Object... objects)  throws SoapWebServiceException {};
}
