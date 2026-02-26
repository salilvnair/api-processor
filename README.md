# api-processor
`api-processor` is a lightweight Java library to standardize external API integrations using a **Facade -> Handler -> Delegate** pattern.

It supports:
- REST orchestration via `RestWebServiceFacade`
- SOAP orchestration via `SoapWebServiceFacade`
- Built-in retry support (optional)
- Consistent request/response preparation and post-processing hooks

If you have many downstream APIs and want a clean, reusable integration structure, this library gives you that contract.

## Maven

```xml
<dependency>
  <groupId>com.github.salilvnair</groupId>
  <artifactId>api-processor</artifactId>
  <version>1.0.4</version>
</dependency>
```

## Core Concepts

## 1) REST flow

`RestWebServiceFacade.initiate(handler, restWsMap, objects...)` drives the call lifecycle:

1. `handler.prepareRequest(...)`
2. `handler.delegate().invoke(...)`
3.  optional retry wrapper (if delegate enables retry)
4. `handler.processResponse(...)`

### Contracts

- `RestWebServiceRequest` and `RestWebServiceResponse` are marker interfaces.
- You define your own concrete request/response classes.
- `restWsMap` is a shared mutable map for passing data across steps.
- `objects...` is optional contextual input (your DTO/context class).

## 2) Handler responsibilities

`RestWebServiceHandler`:
- chooses delegate (`delegate()`)
- builds request (`prepareRequest(...)`)
- transforms response (`processResponse(...)`)
- optional metadata:
  - `webServiceName()` for logs
  - `printLogs()` default `true`
  - `emptyPayLoad()` default `false`

## 3) Delegate responsibilities

`RestWebServiceDelegate`:
- performs actual HTTP invocation in `invoke(...)`
- optional retry controls:
  - `retry()`
  - `maxRetries()`
  - `delay()` + `delayTimeUnit()`
  - `whiteListedExceptions()` (string match on exception message)

## 4) Retry behavior

When enabled, facade wraps delegate invocation with `RetryExecutor`:
- retries up to `maxRetries`
- waits for configured delay between retries
- if whitelist is set, retries only matching exception messages

## Why This Pattern Works

- Keeps API transport logic inside delegate
- Keeps input/output mapping inside handler
- Keeps business flow outside infra classes
- Makes each API integration testable and replaceable

---

## REST Quick Start (Minimal)

### Step 1: Define context

```java
public class LoanApiContext {
    private String path;
    private String baseUrl;
    private String apiKey;
    private Map<String, Object> queryParams;
    private LoanWsResponse mappedResponse;

    // getters/setters
}
```

### Step 2: Define request/response wrappers

```java
import com.github.salilvnair.api.processor.rest.model.RestWebServiceRequest;
import com.github.salilvnair.api.processor.rest.model.RestWebServiceResponse;

public class LoanWsRequest implements RestWebServiceRequest {
    private SomeClassInfo someClassInfo;
    // getters/setters
}

public class LoanWsResponse implements RestWebServiceResponse {
    private SomeClassInfo someClassInfo;
    // getters/setters
}
```

### Step 3: Implement delegate (actual HTTP call)

```java
import com.github.salilvnair.api.processor.rest.handler.RestWebServiceDelegate;
import com.github.salilvnair.api.processor.rest.model.RestWebServiceRequest;
import com.github.salilvnair.api.processor.rest.model.RestWebServiceResponse;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class LoanApiWsDelegate implements RestWebServiceDelegate {
    
  @Override
  public RestWebServiceResponse invoke(RestWebServiceRequest request, Map<String, Object> restWsMap, Object... objects) {
    LoanWsRequest wsRequest = (LoanWsRequest) request;
    LoanApiContext context = (LoanApiContext) objects[0];
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
    headers.set("X-API-KEY", context.getApiKey());

    HttpEntity<?> entity = new HttpEntity<>(wsRequest, headers);

    String url = context.getBaseUrl() + context.getPath() + buildQuery(context.getQueryParams());

    ResponseEntity<LoanWsResponse> responseEntity = new RestTemplate().exchange(url, HttpMethod.GET, entity, Map.class);
    return responseEntity.getBody();
  }

  @Override
  public boolean retry() {
    return true;
  }

  @Override
  public int maxRetries() {
    return 2;
  }

  @Override
  public int delay() {
    return 300;
  }

  @Override
  public TimeUnit delayTimeUnit() {
    return TimeUnit.MILLISECONDS;
  }

  @Override
  public java.util.List<String> whiteListedExceptions() {
    return java.util.List.of("timed out", "Connection refused", "503");
  }

  private String buildQuery(Map<String, Object> queryParams) {
    if (queryParams == null || queryParams.isEmpty()) {
      return "";
    }
    StringBuilder out = new StringBuilder();
    for (Map.Entry<String, Object> e : queryParams.entrySet()) {
      if (e.getKey() == null || e.getKey().isBlank() || e.getValue() == null) {
        continue;
      }
      out.append(out.isEmpty() ? "?" : "&")
              .append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
              .append("=")
              .append(URLEncoder.encode(String.valueOf(e.getValue()), StandardCharsets.UTF_8));
    }
    return out.toString();
  }
}
```

### Step 4: Implement handler (request prep + response mapping)

```java
import com.github.salilvnair.api.processor.rest.handler.RestWebServiceDelegate;
import com.github.salilvnair.api.processor.rest.handler.RestWebServiceHandler;
import com.github.salilvnair.api.processor.rest.model.RestWebServiceRequest;
import com.github.salilvnair.api.processor.rest.model.RestWebServiceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LoanApiWsHandler implements RestWebServiceHandler {

  private final LoanApiWsDelegate delegate;
  

  @Override
  public RestWebServiceDelegate delegate() {
    return delegate;
  }

  @Override
  public RestWebServiceRequest prepareRequest(Map<String, Object> restWsMap, Object... objects) {
    LoanApiContext context = (LoanApiContext) objects[0];
    LoanWsRequest request = new LoanWsRequest();
    request.setSomeClassInfo(prepareSomeClassInfo(context));
    return request;
  }

  private SomeClassInfo prepareSomeClassInfo(LoanApiContext context) {
    // build request wrapper from context
  }

  @Override
  public void processResponse(RestWebServiceRequest request, RestWebServiceResponse response, Map<String, Object> restWsMap, Object... objects) {
    LoanApiContext context = (LoanApiContext) objects[0];
    LoanWsResponse wsResponse = (LoanWsResponse) response;
    context.setMappedResponse(wsResponse);
    restWsMap.put("mappedResponse", wsResponse);
  }

  @Override
  public String webServiceName() {
    return "LoanApiHttp";
  }
}
```

### Step 5: Call facade from service

```java
import com.github.salilvnair.api.processor.rest.facade.RestWebServiceFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoanApiService {

  private final RestWebServiceFacade facade = new RestWebServiceFacade();
  private final LoanApiWsHandler loanApiWsHandler;


  public LoanWsResponse call( String path, Map<String, Object> queryParams) {
    LoanApiContext context = new LoanApiContext();
    context.setBaseUrl("https://some-loan-api.com");
    context.setApiKey("some-api-key");
    context.setPath(path);
    context.setQueryParams(queryParams);
    context.setResponseFieldMap(responseFieldMap);

    Map<String, Object> restWsMap = new LinkedHashMap<>();
    facade.initiate(loanApiHandler, restWsMap, context);
    return context.getMappedResponse();
  }
}
```

---

## SOAP Usage (Overview)

`SoapWebServiceFacade` follows the same structure:
- Handler prepares SOAP request wrapper
- Delegate invokes `WebServiceTemplate`
- Handler processes response
- Retry available via delegate options

Key interfaces:
- `SoapWebServiceHandler`
- `SoapWebServiceDelegate`
- `SoapWebServiceRequest`
- `SoapWebServiceResponse`

For SOAP delegates, helper methods exist in `SoapWebServiceDelegate` for:
- `marshalSendAndReceive(...)`
- header extraction
- easy `WebServiceTemplate` creation via context path + endpoint

---

## Best Practices for Consumers

1. Keep delegates pure transport-layer.
2. Keep handlers focused on request/response mapping.
3. Pass business context through typed context object via `objects...`.
4. Use `restWsMap` for integration outputs only (`mappedResponse`, status, raw payload).
5. Enable retry only for idempotent/safe calls.
6. Keep whitelist specific to transient errors (timeouts, 5xx, connection issues).

## Common Pitfalls

1. `handler == null` or `delegate == null`
- Facade throws explicit exception.

2. Retry not triggering
- `retry()` must return `true` in delegate.
- For whitelist mode, exception text must contain configured strings.

3. Empty payload call
- Return `true` from `emptyPayLoad()` if API doesn’t need request building.

4. Missing mapped response
- Ensure `processResponse(...)` writes output to `restWsMap`.

## Summary

`api-processor` gives you a small, explicit integration framework:
- predictable lifecycle
- clear separation of concerns
- reusable transport delegates
- easy orchestration from service layer

For most consumer apps, start with one generic handler + one delegate + one context class, then scale by endpoint group (loan, customer, payment, etc.).
