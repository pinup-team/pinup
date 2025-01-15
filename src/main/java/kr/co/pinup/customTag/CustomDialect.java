package kr.co.pinup.customTag;

import kr.co.pinup.config.OauthConfig;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;

import java.util.HashSet;
import java.util.Set;

// CustomDialect 클래스는 커스텀 태그가 Thymeleaf 템플릿에서 작동하도록 설정하는 중요한 역할을 하므로, 이 클래스는 여전히 필요합니다. CustomDialect가 없으면, NaverAuthUriProcessor 같은 프로세서를 Thymeleaf가 인식하지 못하므로, 커스텀 태그를 사용할 수 없습니다.
public class CustomDialect extends AbstractProcessorDialect {
    private final OauthConfig oauthConfig;

    public CustomDialect(OauthConfig oauthConfig) {
        super("CustomDialect", "oauth", 1000); // 다이얼렉트 접두어 설정
        this.oauthConfig = oauthConfig;
    }

    @Override
    public Set<IProcessor> getProcessors(String dialectPrefix) {
        Set<IProcessor> processors = new HashSet<>();
        processors.add(new NaverAuthUriProcessor(dialectPrefix, oauthConfig)); // oauthConfig를 NaverAuthUriProcessor로 전달
        processors.add(new GoogleAuthUriProcessor(dialectPrefix, oauthConfig));
        return processors;
    }
}
