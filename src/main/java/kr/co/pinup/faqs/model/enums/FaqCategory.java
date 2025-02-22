package kr.co.pinup.faqs.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FaqCategory {
    USE("이용"),
    MEMBER("회원"),
    COMPANY("기업");

    private final String name;

}
