package io.github.qcsystudio.autocreatetable.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Description: main entry
 *
 * @author qcsy
 * @version 2025/3/11
 */
@SpringBootApplication(scanBasePackages = {"io.github.qcsystudio"})
public class ApplicationApp {
    public static void main(String[] args) {
        SpringApplication.run(ApplicationApp.class, args);
    }
}
