package com.richmond423.loadbalancerpro.api.proxy;

import java.io.IOException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@ConditionalOnProperty(prefix = "loadbalancerpro.proxy", name = "enabled", havingValue = "true")
public class ReverseProxyController {
    private final ReverseProxyService reverseProxyService;

    public ReverseProxyController(ReverseProxyService reverseProxyService) {
        this.reverseProxyService = reverseProxyService;
    }

    @RequestMapping({"/proxy", "/proxy/", "/proxy/**"})
    public void proxy(HttpServletRequest request, HttpServletResponse response) throws IOException {
        reverseProxyService.forward(request, response);
    }
}
