echo "🚀 LocalStack 초기화 스크립트 시작"

# 🗝️ IAM 역할 생성 (Secrets Manager 용)
echo "🔐 IAM 역할 (Secrets Manager) 생성 중..."
awslocal iam create-role \
    --role-name localstack-secrets-role \
    --assume-role-policy-document '{
        "Version": "2012-10-17",
        "Statement": [
            {
                "Effect": "Allow",
                "Principal": {
                    "Service": "secretsmanager.amazonaws.com"
                },
                "Action": "sts:AssumeRole"
            }
        ]
    }'

awslocal iam put-role-policy \
    --role-name localstack-secrets-role \
    --policy-name SecretsManagerAccess \
    --policy-document '{
        "Version": "2012-10-17",
        "Statement": [
            {
                "Effect": "Allow",
                "Action": [
                    "secretsmanager:GetSecretValue",
                    "secretsmanager:DescribeSecret"
                ],
                "Resource": "*"
            }
        ]
    }'

echo "✅ IAM 역할 생성 완료"

# 🗝️ S3 사용자 생성 (S3 용)
echo "🪣 S3 사용자 생성 중..."
awslocal iam create-user --user-name localstack-s3-user

S3_KEYS=$(awslocal iam create-access-key --user-name localstack-s3-user)
ACCESS_KEY_ID="test"
SECRET_ACCESS_KEY="test"

awslocal iam put-user-policy \
    --user-name localstack-s3-user \
    --policy-name S3AccessPolicy \
    --policy-document '{
        "Version": "2012-10-17",
        "Statement": [
            {
                "Effect": "Allow",
                "Action": [
                    "s3:ListBucket",
                    "s3:GetObject",
                    "s3:PutObject",
                    "s3:DeleteObject"
                ],
                "Resource": "*"
            }
        ]
    }'

echo "✅ S3 사용자 생성 완료"
echo "🗝️ Access Key ID: $ACCESS_KEY_ID"
echo "🗝️ Secret Access Key: $SECRET_ACCESS_KEY"

# 🪣 S3 버킷 생성
echo "🪣 S3 버킷 생성 중..."
awslocal s3 mb s3://pinup-test
echo "✅ S3 버킷 생성 완료"

# 🔐 Secrets Manager 시크릿 생성
echo "🔐 Secrets Manager 시크릿 생성 중..."
awslocal secretsmanager create-secret \
    --name test/api/kakaomap \
    --secret-string '{
        "kakao.api.key.rest": "test-rest-key",
        "kakao.api.key.js": "test-js-key"
    }'
echo "✅ 시크릿 생성 완료"

echo "🎉 초기화 스크립트 완료"

# 📢 환경 변수 출력 (필수)
echo "🗝️ Access Key ID: $ACCESS_KEY_ID"
echo "🗝️ Secret Access Key: $SECRET_ACCESS_KEY"
