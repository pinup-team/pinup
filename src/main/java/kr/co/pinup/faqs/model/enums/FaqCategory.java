package kr.co.pinup.faqs.model.enums;

import kr.co.pinup.faqs.exception.FaqCategoryNotFound;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum FaqCategory {
    USE("이용"),
    MEMBER("회원"),
    COMPANY("기업");

    private final String name;

    FaqCategory(String name) {
        this.name = name;
    }

    public static FaqCategory ofName(String dbData) {
        return Arrays.stream(FaqCategory.values())
                .filter(v -> v.getName().equals(dbData))
                .findAny()
                .orElseThrow(() -> new FaqCategoryNotFound(
                        String.format("FAQ 카테고리에 %s가 존재하지 않습니다.", dbData)));
    }
}
