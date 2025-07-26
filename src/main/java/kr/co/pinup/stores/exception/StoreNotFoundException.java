package kr.co.pinup.stores.exception;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;

public class StoreNotFoundException extends GlobalCustomException {

  private static final String DEFAULT_MESSAGE = "해당 스토어가 존재하지 않습니다.";

  public StoreNotFoundException() {
    this(DEFAULT_MESSAGE);
  }

  public StoreNotFoundException(String message) {
    super(message);
  }

  @Override
  protected int getHttpStatusCode() {
    return HttpStatus.NOT_FOUND.value();
  }
}
