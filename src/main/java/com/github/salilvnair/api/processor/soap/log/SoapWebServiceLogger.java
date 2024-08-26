package com.github.salilvnair.api.processor.soap.log;

import com.github.salilvnair.api.processor.soap.delegate.SoapWebServiceDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoapWebServiceLogger {
    protected final static Logger logger = LoggerFactory.getLogger(SoapWebServiceLogger.class);

    public static void printLogs(String xmlString, SoapWebServiceDelegate delegate, String type) {
        String webServiceName = delegate.webServiceName();
        logger.info("====================================================" + webServiceName + " " + type + " BEGINS=================================================");
        logger.info(xmlString);
        logger.info("====================================================" + webServiceName + " " + type + " ENDS=================================================");
    }


}
