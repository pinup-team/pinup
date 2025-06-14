package kr.co.pinup.api.aws;

public interface AwsSecretsProvider {

    String getSecretValue(String secretKey);
}
