awslocal secretsmanager create-secret \
    --name test/api/kakaomap \
    --secret-string '{"kakao.api.key.rest":"test-rest-key","kakao.api.key.js":"test-js-key"}'
