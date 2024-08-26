package com.github.salilvnair.api.processor.helper.retry;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RetryExecutorException extends Exception {
	private static final long serialVersionUID = 1L;
	
	private String exceptionMessage;
	private String retryExecutorMessage;
	
	public RetryExecutorException(Throwable ex, String retryExecutorMessage, String exceptionMessage) {
		super(ex);
		this.setRetryExecutorMessage(retryExecutorMessage);
		this.setExceptionMessage(exceptionMessage);
	}
	
	public RetryExecutorException(String exception) {
		super(exception);
	}


}
