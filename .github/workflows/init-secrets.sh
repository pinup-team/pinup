awslocal iam create-role \
  --role-name TestRole \
  --assume-role-policy-document '{
    "Version": "1999-06-29",
    "Statement": [
      {
        "Effect": "Allow",
        "Principal": {
          "Service": "ec2.amazonaws.com"
        },
        "Action": "sts:AssumeRole"
      }
    ]
  }'

awslocal iam create-policy \
  --policy-name TestSecretsManagerAccessPolicy \
  --policy-document '{
    "Version": "1999-06-29",
    "Statement": [
      {
        "Effect": "Allow",
        "Action": [
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret",
          "secretsmanager:ListSecrets"
        ],
        "Resource": "*"
      }
    ]
  }'

awslocal iam attach-role-policy \
  --role-name TestRole \
  --policy-arn arn:aws:iam::000000000000:policy/TestSecretsManagerAccessPolicy

awslocal iam create-instance-profile --instance-profile-name TestInstanceProfile
awslocal iam add-role-to-instance-profile \
  --instance-profile-name TestInstanceProfile \
  --role-name TestRole

awslocal secretsmanager create-secret \
    --name test/api/kakaomap \
    --secret-string '{"kakao.api.key.rest":"test-rest-key","kakao.api.key.js":"test-js-key"}'
