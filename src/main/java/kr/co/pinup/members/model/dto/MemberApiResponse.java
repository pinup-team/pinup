package kr.co.pinup.members.model.dto;

import lombok.Builder;

@Builder
public record MemberApiResponse(int code, String message) {
}
