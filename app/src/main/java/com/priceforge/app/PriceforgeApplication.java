package com.priceforge.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.priceforge")
public class PriceforgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(PriceforgeApplication.class, args);
    }
}
