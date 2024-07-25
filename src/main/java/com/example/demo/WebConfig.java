//package com.example.demo;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//@Configuration
//public class WebConfig implements WebMvcConfigurer {
//
//    @Override
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        registry.addResourceHandler("/images/**")
//                .addResourceLocations("file:D:/eclipse jee/Reactjs/365risk/src/images/");
//    }
//}

package com.example.demo;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

   
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
    	 registry
         .addResourceHandler("/images/**")
         .addResourceLocations("classpath:/images/");
        
        registry
        .addResourceHandler("/html/**")
        .addResourceLocations("classpath:/html/");
    }
}
