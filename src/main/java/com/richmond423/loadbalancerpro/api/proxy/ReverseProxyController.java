package com.richmond423.loadbalancerpro.api.proxy;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@ConditionalOnProperty(prefix = "loadbalancerpro.proxy", name = "enabled", havingValue = "true")
public class ReverseProxyController {
    private final ReverseProxyService reverseProxyService;

    public ReverseProxyController(ReverseProxyService reverseProxyService) {
        this.reverseProxyService = reverseProxyService;
    }

    @RequestMapping({"/proxy", "/proxy/", "/proxy/**"})
    public ResponseEntity<byte[]> proxy(HttpServletRequest request) {
        ReverseProxyResponse response = reverseProxyService.forward(request);
        return ResponseEntity.status(response.statusCode())
                .headers(response.headers())
                .body(response.body());
    }
}
