package com.github.salilvnair.api.processor.rest.handler;

import com.github.salilvnair.api.processor.rest.model.RestWebServiceRequest;
import com.github.salilvnair.api.processor.rest.model.RestWebServiceResponse;
import java.util.Map;

public interface RestWebServiceHandler {
    RestWebServiceDelegate delegate();
    default RestWebServiceRequest prepareRequest(Map<String, Object> restWsMap, Object... objects) {return null;}
    default void processResponse(RestWebServiceRequest request, RestWebServiceResponse response, Map<String, Object> restWsMap, Object... objects) {}
    default String webServiceName() { return null; }
    default boolean printLogs() { return true; }
    default boolean emptyPayLoad() { return false; }
}
