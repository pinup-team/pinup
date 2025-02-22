package kr.co.pinup.locations.exception;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;

public class LocationNotFoundException extends GlobalCustomException {

  private static final String DEFAULT_MESSAGE = "해당 로케이션이 존재하지 않습니다.";

  public LocationNotFoundException(String message) {
    super(message);
  }

  public LocationNotFoundException() {
    super(DEFAULT_MESSAGE);
  }

  @Override
  protected int getHttpStatusCode() {
    return HttpStatus.NOT_FOUND.value();
  }
}
