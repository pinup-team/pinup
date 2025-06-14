package kr.co.pinup.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@ConfigurationProperties("cloud.aws.credentials")
@Getter
@Setter
public class AwsCredentialsProperties {

    private String accessKey;
    private String secretKey;

    private final Environment environment;

    @Autowired
    public AwsCredentialsProperties(final Environment environment) {
        this.environment = environment;
    }

    public String getActiveProfile() {
        final String[] activeProfiles = environment.getActiveProfiles();

        return activeProfiles.length > 0
                ? activeProfiles[0]
                : environment.getDefaultProfiles()[0];
    }
}
