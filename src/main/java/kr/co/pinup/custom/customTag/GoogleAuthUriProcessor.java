package kr.co.pinup.custom.customTag;

import kr.co.pinup.members.config.OauthConfig;
import lombok.extern.slf4j.Slf4j;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.UUID;

@Slf4j
public class GoogleAuthUriProcessor extends AbstractElementTagProcessor {

    private OauthConfig oauthConfig;
    private OauthConfig.Registration googleRegistration;
    private OauthConfig.Provider googleProvider;

    private static final String ATTR_NAME = "googleLoginUrl";
    private static final int PRECEDENCE = 10000;

    public GoogleAuthUriProcessor(String dialectPrefix, OauthConfig oauthConfig) {
        super(TemplateMode.HTML, dialectPrefix, null, false, ATTR_NAME, true, PRECEDENCE);
        this.oauthConfig = oauthConfig;

        if (this.oauthConfig != null && this.oauthConfig.getRegistration() != null && this.oauthConfig.getProvider() != null) {
            this.googleRegistration = oauthConfig.getRegistration().get("google");
            this.googleProvider = oauthConfig.getProvider().get("google");
            if (this.googleRegistration == null || this.googleProvider == null) {
                log.info("Google registration/provider is not found in OauthConfig.");
            }
        } else {
            log.info("OauthConfig or its registration/provider is null.");
        }
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag, IElementTagStructureHandler structureHandler) {
        if(googleRegistration == null) {
            System.out.println("googleRegistration is null");
            return;
        }

        try {
            String redirectUri = URLEncoder.encode(googleRegistration.getRedirectUri(), "UTF-8");
            String scope = "https://www.googleapis.com/auth/userinfo.email+profile+https://www.googleapis.com/auth/user.gender.read+https://www.googleapis.com/auth/user.birthday.read";
            String state = URLEncoder.encode(generateState(), "UTF-8");

            String authUrl = String.format(
                    "%s?client_id=%s&redirect_uri=%s&response_type=code&scope=%s&state=%s",
                    googleProvider.getAuthorizationUri(),
                    googleRegistration.getClientId(),
                    redirectUri,
                    scope,
                    state
            );

            structureHandler.setAttribute("href", authUrl);
        } catch (UnsupportedEncodingException e) {
            System.out.println("Error encoding URL parameters: " + e.getMessage());
        }
    }

    private String generateState() {
        return UUID.randomUUID().toString();
    }
}
