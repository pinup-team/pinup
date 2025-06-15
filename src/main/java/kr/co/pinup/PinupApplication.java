package kr.co.pinup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PinupApplication {

    public static void main(String[] args) {
        SpringApplication.run(PinupApplication.class, args);
    }

}
