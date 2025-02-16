package kr.co.pinup.oauth;

public interface OAuthToken {
    String getAccessToken();
    String getRefreshToken();
}

/*
TODO
* 일단 oauth refresh하는거 만들엇으니까
* header에 있는거 accessToken 꺼내서 검증하는거 만들고 없으면
* OAuthAccessTokenNotFoundException 만들어서
* 이거 catch하는대로 바로 refresh 호출할 수 있도록 만들기
* */