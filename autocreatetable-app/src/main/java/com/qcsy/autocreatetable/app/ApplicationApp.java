package com.qcsy.autocreatetable.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Description: mian entry
 *
 * @author qcsy
 * @version 2025/3/11
 */
@SpringBootApplication(scanBasePackages = {"com.qcsy.autocreatetable"})
public class ApplicationApp {
    public static void main(String[] args) {
        SpringApplication.run(ApplicationApp.class, args);
    }
}
