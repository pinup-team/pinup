package kr.co.pinup.custom.customTag;

import kr.co.pinup.config.OauthConfig;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;

import java.util.HashSet;
import java.util.Set;

public class CustomDialect extends AbstractProcessorDialect {
    private final OauthConfig oauthConfig;

    public CustomDialect(OauthConfig oauthConfig) {
        super("CustomDialect", "oauth", 1000);
        this.oauthConfig = oauthConfig;
    }

    @Override
    public Set<IProcessor> getProcessors(String dialectPrefix) {
        Set<IProcessor> processors = new HashSet<>();
        processors.add(new NaverAuthUriProcessor(dialectPrefix, oauthConfig));
        processors.add(new GoogleAuthUriProcessor(dialectPrefix, oauthConfig));
        return processors;
    }
}
