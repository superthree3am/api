package com.bni.api;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class ApiApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Jakarta"));
		SpringApplication.run(ApiApplication.class, args);
	}

	// @Bean
	// public WebMvcConfigurer corsConfigurer() {
	// 	return new WebMvcConfigurer() {
	// 		@Override
	// 		public void addCorsMappings(CorsRegistry registry) {
	// 			registry.addMapping("/api/**")
	// 					.allowedOrigins("http://localhost:3000", "http://localhost:8080", "http://127.0.0.1:3000", "https://openshift3am.42n.fun", "http://openshift3am.42n.fun", "http://34.101.118.205:8080")
	// 					.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
	// 					.allowedHeaders("*")
	// 					.allowCredentials(true);
	// 		}
	// 	};
	// }
}