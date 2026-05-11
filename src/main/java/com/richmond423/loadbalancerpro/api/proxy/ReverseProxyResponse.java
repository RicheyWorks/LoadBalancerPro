package com.richmond423.loadbalancerpro.api.proxy;

import org.springframework.http.HttpHeaders;

record ReverseProxyResponse(int statusCode, HttpHeaders headers, byte[] body) {
    ReverseProxyResponse {
        headers = HttpHeaders.readOnlyHttpHeaders(headers);
        body = body == null ? new byte[0] : body.clone();
    }
}
