package kr.co.pinup.api.kakao.model.dto;

import kr.co.pinup.api.kakao.exception.KakaoApiException;

import java.util.List;

public record KakaoAddressResponse(List<KakaoAddressDocument> documents) {

    public KakaoAddressDocument toDocument() {
        if (documents == null || documents.isEmpty()) {
            throw new KakaoApiException("잘못된 주소입니다.");
        }
        return documents.get(0);
    }
}
