package com.fengc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplicatin {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplicatin.class, args);
    }
}
