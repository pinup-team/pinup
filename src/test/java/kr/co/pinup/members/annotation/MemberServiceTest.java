package kr.co.pinup.members.annotation;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.*;

// annotation이 class, interface, record 등의 선언에 사용될 수 있도록 설정
@Target(ElementType.TYPE)
// annotation이 runtime 단계에서 사용될 수 있도록 설정
@Retention(RetentionPolicy.RUNTIME)
// annotation이 javadoc 문서화에 표현될 수 있도록 설정
@Documented
// annotation이 부모 클래스에 선언되어 있을 때 자식 클래스에도 상속되도록 설정
@Inherited
// mockito 사용 설정
@ExtendWith(MockitoExtension.class)
public @interface MemberServiceTest {
}
