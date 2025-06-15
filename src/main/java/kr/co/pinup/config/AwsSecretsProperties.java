package kr.co.pinup.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("cloud.aws.secretsmanager")
@Getter
@Setter
public class AwsSecretsProperties {

    private String endpoint;
    private String region;
    private String secretName;

}
