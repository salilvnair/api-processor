package com.github.salilvnair.api.processor.rest.facade;

import tools.jackson.databind.ObjectMapper;
import com.github.salilvnair.api.processor.helper.retry.RetryExecutor;
import com.github.salilvnair.api.processor.helper.retry.RetryExecutorException;
import com.github.salilvnair.api.processor.helper.retry.RetryObserver;
import com.github.salilvnair.api.processor.rest.exception.RestWebServiceException;
import com.github.salilvnair.api.processor.rest.handler.RestWebServiceDelegate;
import com.github.salilvnair.api.processor.rest.handler.RestWebServiceHandler;
import com.github.salilvnair.api.processor.rest.model.RestWebServiceRequest;
import com.github.salilvnair.api.processor.rest.model.RestWebServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class RestWebServiceFacade {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    private ObjectMapper objectMapper;

    public static final String REQUEST = "REQUEST";

    public static final String RESPONSE = "RESPONSE";

    public RestWebServiceFacade() {
        this.objectMapper = new ObjectMapper();
    }

    public RestWebServiceFacade(Logger logger) {
        if(logger != null) {
            this.logger = logger;
        }
    }

    public RestWebServiceFacade(Logger logger, ObjectMapper objectMapper) {
        if (logger != null) {
            this.logger = logger;
        }
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    public RestWebServiceFacade(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    public void initiate(RestWebServiceHandler handler, Map<String, Object> restWsMap, Object... objects) {
        if(handler == null) {
            throw new RestWebServiceException("Cannot initiate webservice call without a proper handler class");
        }
        RestWebServiceRequest request = null;
        if(!handler.emptyPayLoad()) {
            request = handler.prepareRequest(restWsMap, objects);
            if(handler.printLogs()) {
                printLogs(request, handler, REQUEST);
            }
        }
        RestWebServiceDelegate delegate = handler.delegate();
        if(delegate == null) {
            throw new RestWebServiceException("Cannot initiate webservice call without a proper client delegate bean for the handler", handler.webServiceName());
        }
        RestWebServiceResponse response = null;
        if(delegate.retry()) {
            RestWebServiceRequest finalRequest = request;
            try {
                response =  new RetryExecutor()
                        .maxRetries(delegate.maxRetries())
                        .delay(delegate.delay(), delegate.delayTimeUnit())
                        .configure(delegate.whiteListedExceptions())
                        .observer(new RetryObserver() {
                            @Override
                            public void onRetryScheduled(int nextAttempt, int maxRetries, long delayMs, Exception lastError) {
                                delegate.onRetryScheduled(nextAttempt, maxRetries, delayMs, lastError);
                            }

                            @Override
                            public void onRetryAttemptFailed(int attempt, int maxRetries, Exception error) {
                                delegate.onRetryAttemptFailed(attempt, maxRetries, error);
                            }

                            @Override
                            public void onMaxRetriesExceeded(int maxRetries, Exception lastError) {
                                delegate.onMaxRetriesExceeded(maxRetries, lastError);
                            }
                        })
                        .execute(() -> delegate.invoke(finalRequest, restWsMap, objects));
            }
            catch (RetryExecutorException e) {
                throw new RestWebServiceException(e, handler.webServiceName());
            }
        }
        else {
            response = delegate.invoke(request, restWsMap, objects);
        }

        if(handler.printLogs()) {
            printLogs(response, handler, RESPONSE);
        }
        handler.processResponse(request, response, restWsMap, objects);
    }

    public void printLogs(Object requestResponse, RestWebServiceHandler handler, String type) {
        String webServiceName = handler.webServiceName();
        String jsonString = "{}";
        try {
            jsonString = objectMapper.writeValueAsString(requestResponse);
        }
        catch (Exception ex) {
            logger.error("RestWebServiceFacade>>printLogs>>caught exception:"+ex);
        }
        logger.info("===================================================={} {} BEGINS=================================================", webServiceName, type);
        logger.info(jsonString);
        logger.info("===================================================={} {} ENDS=================================================", webServiceName, type);
    }

}
