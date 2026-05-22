package com.im.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${im.upload.path:./uploads}")
    private String uploadPath;

    @Value("${im.upload.url-prefix:/uploads}")
    private String urlPrefix;

    @Autowired
    private ApiKeyInterceptor apiKeyInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 确保路径以 / 结尾，并且加上 file: 前缀
        String path = uploadPath;
        if (!path.endsWith("/")) {
            path += "/";
        }
        
        // 打印日志方便调试
        System.out.println("Mapping " + urlPrefix + "/** to file:" + path);
        
        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations("file:" + path);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyInterceptor)
                .addPathPatterns("/api/external/**");
    }
}
