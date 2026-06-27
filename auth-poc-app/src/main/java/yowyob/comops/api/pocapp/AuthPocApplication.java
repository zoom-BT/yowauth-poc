package yowyob.comops.api.pocapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "yowyob.comops.api")
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AuthPocApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthPocApplication.class, args);
    }
}
