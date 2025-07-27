package kr.co.pinup.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import kr.co.pinup.custom.ImageFileValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ImageFileValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidImageFile {

    String message() default "이미지는 필수 항목입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
