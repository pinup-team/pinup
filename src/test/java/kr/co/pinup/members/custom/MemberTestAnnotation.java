package kr.co.pinup.members.custom;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Transactional
@ExtendWith(MockitoExtension.class)
public @interface MemberTestAnnotation {
}
