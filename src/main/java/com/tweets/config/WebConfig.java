package com.tweets.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Autowired
	private SystemContextSetterInterceptor systemContextInterceptor;

	@Autowired
	private CorrelationIdInterceptor correlationIdInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(correlationIdInterceptor);
		registry.addInterceptor(systemContextInterceptor);
	}
}
