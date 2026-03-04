package com.github.salilvnair.api.processor.helper.retry;

public interface RetryObserver {

    default void onRetryScheduled(int nextAttempt, int maxRetries, long delayMs, Exception lastError) {
    }

    default void onRetryAttemptFailed(int attempt, int maxRetries, Exception error) {
    }

    default void onMaxRetriesExceeded(int maxRetries, Exception lastError) {
    }
}

