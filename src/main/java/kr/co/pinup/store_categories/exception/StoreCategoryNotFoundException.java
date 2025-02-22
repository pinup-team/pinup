package kr.co.pinup.store_categories.exception;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;

public class StoreCategoryNotFoundException extends GlobalCustomException {

  private static final String DEFAULT_MESSAGE = "해당 스토어 카테고리가 존재하지 않습니다.";

  public StoreCategoryNotFoundException(String message) {
    super(message);
  }

  public StoreCategoryNotFoundException() {
    super(DEFAULT_MESSAGE);
  }

  @Override
  protected int getHttpStatusCode() {
    return HttpStatus.NOT_FOUND.value();
  }
}
