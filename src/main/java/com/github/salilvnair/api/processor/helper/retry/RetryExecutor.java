package com.github.salilvnair.api.processor.helper.retry;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class RetryExecutor {
	
	protected final Logger logger = LoggerFactory.getLogger(getClass());
    private static volatile RetryObserver globalObserver;

    private int maxRetries = 0;

    private long delay = 0;
    
    private List<String> whiteListedExceptions;
    private RetryObserver observer;

    public static void setGlobalObserver(RetryObserver observer) {
        globalObserver = observer;
    }

    public static RetryObserver getGlobalObserver() {
        return globalObserver;
    }

    public RetryExecutor observer(RetryObserver observer) {
        this.observer = observer;
        return this;
    }

    public RetryExecutor maxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }
    
    public RetryExecutor delay(long delayInMillis) {
        this.delay = delayInMillis;
        return this;
    }
    
    public RetryExecutor maxRetries(String maxRetries) {
    	if(StringUtils.isNotEmpty(maxRetries)) {
    		maxRetries = maxRetries.trim();
    		if(NumberUtils.isDigits(maxRetries)) {
                this.maxRetries = Integer.parseInt(maxRetries);	
    		}
    	}    
        return this;
    }
    
    public RetryExecutor delay(String delay, TimeUnit timeUnit) {
    	if(StringUtils.isNotEmpty(delay)) {
    		delay = delay.trim();
            if(NumberUtils.isDigits(delay)) {
            	this.delay = timeUnit.toMillis(Integer.parseInt(delay));
            }
    	} 
        return this;
    }
    
    public RetryExecutor configure(String exception) {
    	if(whiteListedExceptions == null) {
    		whiteListedExceptions = new ArrayList<>();
    	}
    	whiteListedExceptions.add(exception);
    	return this;
    }
    
    public RetryExecutor configure(List<String> exceptions) {
    	if(whiteListedExceptions == null) {
    		whiteListedExceptions = new ArrayList<>();
    	}
    	if(CollectionUtils.isNotEmpty(exceptions)) {
            whiteListedExceptions.addAll(exceptions);
        }
    	return this;
    }
    
    public RetryExecutor delay(int delay, TimeUnit timeUnit) {
        this.delay = timeUnit.toMillis(delay);
        return this;
    }

    // Takes a function and executes it, if fails, passes the function to the retry command
    //  executes until it exceeds max retries
    public <T, R> T execute(Supplier<T> function) throws RetryExecutorException {
        try {
            return function.get();
        } 
        catch (Exception e) {
        	if(whiteListedExceptions!=null && !whiteListedExceptions.isEmpty()) {
        		boolean foundWhiteListedException = whiteListedExceptions.stream()
        		.anyMatch(exception -> e.getLocalizedMessage()!=null && e.getLocalizedMessage().contains(exception));
        		if(!foundWhiteListedException) {
        			String retryExecutorMsg = "FAILED will not be retried as the exception is not whitelisted";
                    logger.error("{} ex:{}", retryExecutorMsg, e.getLocalizedMessage());
        			throw new RetryExecutorException(e, retryExecutorMsg, e.getLocalizedMessage());
        		}
        	}
        	logger.error(e.getLocalizedMessage());
            logger.error("FAILED will be retried {} times after a delay of {} seconds.", maxRetries, TimeUnit.MILLISECONDS.toSeconds(delay));
            return retry(function, e);
        }
    }

    private <T> T retry(Supplier<T> function, Exception initialException) throws RetryExecutorException {
    	Exception exception = initialException;
        int attempt = 1;
        while (attempt <= maxRetries) {
            notifyRetryScheduled(attempt, exception);
            try {
            	if( delay > 0 ) {
            		Thread.sleep(delay);
            	}
                return function.get();
            } 
            catch (Exception ex) {
                notifyRetryAttemptFailed(attempt, ex);
                logger.error(ex.getLocalizedMessage());
                logger.error("FAILED on retry {} of {}", attempt, maxRetries);
                exception = ex;
                if (attempt >= maxRetries) {
                	logger.error("Max retries exceeded.");
                    notifyMaxRetriesExceeded(exception);
                    break;
                }
                attempt++;
            }
        }
        if(exception==null) {
        	throw new RetryExecutorException("FAILED on all of " + maxRetries + " retries");
        }
        else {
        	throw new RetryExecutorException(exception, "FAILED on all of " + maxRetries + " retries", exception.getLocalizedMessage());
        }        
   }

    private RetryObserver activeObserver() {
        return observer == null ? globalObserver : observer;
    }

    private void notifyRetryScheduled(int nextAttempt, Exception lastError) {
        RetryObserver observer = activeObserver();
        if (observer == null) {
            return;
        }
        try {
            observer.onRetryScheduled(nextAttempt, maxRetries, delay, lastError);
        } catch (Exception ignored) {
        }
    }

    private void notifyRetryAttemptFailed(int attempt, Exception error) {
        RetryObserver observer = activeObserver();
        if (observer == null) {
            return;
        }
        try {
            observer.onRetryAttemptFailed(attempt, maxRetries, error);
        } catch (Exception ignored) {
        }
    }

    private void notifyMaxRetriesExceeded(Exception lastError) {
        RetryObserver observer = activeObserver();
        if (observer == null) {
            return;
        }
        try {
            observer.onMaxRetriesExceeded(maxRetries, lastError);
        } catch (Exception ignored) {
        }
    }
} 
