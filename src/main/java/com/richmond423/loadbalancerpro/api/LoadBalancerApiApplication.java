package com.richmond423.loadbalancerpro.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LoadBalancerApiApplication {
    private static final String FALLBACK_VERSION = "2.5.0";

    public static void main(String[] args) {
        if (isVersionRequested(args)) {
            System.out.println("LoadBalancerPro version " + version());
            return;
        }
        SpringApplication.run(LoadBalancerApiApplication.class, args);
    }

    static boolean isVersionRequested(String[] args) {
        if (args == null) {
            return false;
        }
        for (String arg : args) {
            if ("--version".equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }

    static String version() {
        String implementationVersion = LoadBalancerApiApplication.class.getPackage().getImplementationVersion();
        return implementationVersion == null || implementationVersion.isBlank()
                ? FALLBACK_VERSION
                : implementationVersion;
    }
}
