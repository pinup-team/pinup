package kr.co.pinup.custom.memberRequest;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import kr.co.pinup.members.model.dto.MemberRequest;
import kr.co.pinup.oauth.OAuthProvider;

public class PasswordValidator implements ConstraintValidator<PasswordRequiredForLocal, MemberRequest> {

    @Override
    public boolean isValid(MemberRequest request, ConstraintValidatorContext context) {
        if (request == null) return true; // 객체 자체가 없으면 skip

        if (request.providerType() == OAuthProvider.PINUP) {
            // LOCAL → 비밀번호 필수
            return request.password() != null && !request.password().isBlank();
        } else {
            // OAUTH → 비밀번호 없어야 함
            return request.password() == null || request.password().isBlank();
        }
    }
}
