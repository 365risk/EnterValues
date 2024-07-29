package com.example.demo;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve images from the Codespace directory
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:/workspaces/EnterValues/src/main/resources/images/");
        
        // Serve HTML files from the Codespace directory
        registry.addResourceHandler("/html/**")
                .addResourceLocations("file:/workspaces/EnterValues/src/main/resources/html/");
    }
}
