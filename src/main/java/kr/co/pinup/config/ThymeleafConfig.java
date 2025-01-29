package kr.co.pinup.config;

import kr.co.pinup.custom.customTag.CustomDialect;
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;

@Configuration
public class ThymeleafConfig {
    private final OauthConfig oauthConfig;

    public ThymeleafConfig(OauthConfig oauthConfig) {
        this.oauthConfig = oauthConfig;
    }

    @Bean
    public CustomDialect customDialect() {
        return new CustomDialect(oauthConfig);
    }

    @Bean
    public SpringResourceTemplateResolver templateResolver() {
        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        // TODO: hot reload를 위해 임시로 file 파일 자체를 읽도록 수정
        templateResolver.setPrefix("file:src/main/resources/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        // TODO: Template cache is true by default. Set to false if you want
        // templates to be automatically updated when modified.
        // TODO: hot reload를 위해 임시로 false로 설정
        templateResolver.setCacheable(false);
        return templateResolver;
    }

    @Bean
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setEnableSpringELCompiler(true);
        templateEngine.setTemplateResolver(templateResolver());
        templateEngine.addDialect(customDialect());  // CustomDialect 추가
        templateEngine.addDialect(new LayoutDialect());
        return templateEngine;
    }

    @Bean
    public ThymeleafViewResolver viewResolver() {
        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine());
        viewResolver.setCharacterEncoding("UTF-8");
        viewResolver.setOrder(0);
        return viewResolver;
    }
}
