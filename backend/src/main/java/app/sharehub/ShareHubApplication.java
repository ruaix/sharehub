package app.sharehub;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@MapperScan("app.sharehub.mapper")
@ConfigurationPropertiesScan
public class ShareHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShareHubApplication.class, args);
    }
}
