package kr.co.pinup.users.oauth.customTag;

import kr.co.pinup.config.OauthConfig;
import lombok.extern.slf4j.Slf4j;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.UUID;

@Slf4j
public class NaverAuthUriProcessor extends AbstractElementTagProcessor {

    private OauthConfig oauthConfig;
    private OauthConfig.Registration naverRegistration;
    private OauthConfig.Provider naverProvider;

    private static final String ATTR_NAME = "naverLoginUrl";
    private static final int PRECEDENCE = 10000;

    public NaverAuthUriProcessor(String dialectPrefix, OauthConfig oauthConfig) {
        super(TemplateMode.HTML, dialectPrefix, null, false, ATTR_NAME, true, PRECEDENCE);
        this.oauthConfig = oauthConfig;
        // OAuthConfig 초기화
        if (this.oauthConfig != null && this.oauthConfig.getRegistration() != null && this.oauthConfig.getProvider() != null) {
            this.naverRegistration = oauthConfig.getRegistration().get("naver");
            this.naverProvider = oauthConfig.getProvider().get("naver");
            if (this.naverRegistration == null || this.naverProvider == null) {
                log.info("Naver registration/provider is not found in OauthConfig.");
            }
        } else {
            log.info("OauthConfig or its registration/provider is null.");
        }
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag, IElementTagStructureHandler structureHandler) {
        if(naverRegistration == null) {
            System.out.println("naverRegistration is null");
            return;
        }

        String authUrl = String.format(
                "%s?response_type=code&client_id=%s&redirect_uri=%s&state=%s",
                naverProvider.getAuthorizationUri(),
                naverRegistration.getClientId(),
                naverRegistration.getRedirectUri(),
                generateState()
        );

        structureHandler.setAttribute("href", authUrl);
    }

    private String generateState() {
        return UUID.randomUUID().toString();
    }
}
