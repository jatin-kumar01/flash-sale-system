////////package com.flashsale.gateway.config;
////////
////////import org.springframework.context.annotation.Bean;
////////import org.springframework.context.annotation.Configuration;
////////import org.springframework.core.Ordered;
////////import org.springframework.core.annotation.Order;
////////import org.springframework.web.cors.CorsConfiguration;
////////import org.springframework.web.cors.reactive.CorsWebFilter;
////////import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
////////
////////import java.util.Arrays;
////////import java.util.List;
////////
////////@Configuration
////////public class CorsConfig {
////////
////////    @Bean
////////    @Order(Ordered.HIGHEST_PRECEDENCE)
////////    public CorsWebFilter corsWebFilter() {
////////        CorsConfiguration config = new CorsConfiguration();
////////        config.setAllowCredentials(true);
////////        config.setAllowedOrigins(List.of(
////////                "http://localhost:3000",
////////                "http://localhost:5173",
////////                "http://127.0.0.1:3000",
////////                "http://127.0.0.1:5173"
////////        ));
////////        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
////////        config.setAllowedHeaders(Arrays.asList(
////////                "Content-Type",
////////                "Authorization",
////////                "X-Requested-With",
////////                "X-Correlation-Id",
////////                "Idempotency-Key",
////////                "traceparent"
////////        ));
////////        config.setExposedHeaders(Arrays.asList("Authorization", "X-Trace-Id", "Idempotency-Key"));
////////        config.setMaxAge(3600L);
////////
////////        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
////////        source.registerCorsConfiguration("/**", config);
////////
////////        return new CorsWebFilter(source);
////////    }
////////}
//////package com.flashsale.gateway.config;
//////
//////import org.springframework.context.annotation.Bean;
//////import org.springframework.context.annotation.Configuration;
//////import org.springframework.core.Ordered;
//////import org.springframework.core.annotation.Order;
//////import org.springframework.web.cors.CorsConfiguration;
//////import org.springframework.web.cors.reactive.CorsWebFilter;
//////import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
//////
//////import java.util.Arrays;
//////import java.util.List;
//////
//////@Configuration
//////public class CorsConfig {
//////
//////    @Bean
//////    @Order(Ordered.HIGHEST_PRECEDENCE)
//////    public CorsWebFilter corsWebFilter() {
//////        CorsConfiguration config = new CorsConfiguration();
//////        config.setAllowCredentials(true);
//////        config.setAllowedOrigins(List.of(
//////                "http://localhost:3000",
//////                "http://localhost:5173",
//////                "http://localhost:5174",
//////                "http://127.0.0.1:3000",
//////                "http://127.0.0.1:5173",
//////                "http://127.0.0.1:5174"
//////        ));
//////        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//////        config.setAllowedHeaders(Arrays.asList(
//////                "Content-Type",
//////                "Authorization",
//////                "X-Requested-With",
//////                "X-Correlation-Id",
//////                "Idempotency-Key",
//////                "traceparent"
//////        ));
//////        config.setExposedHeaders(Arrays.asList("Authorization", "X-Trace-Id", "Idempotency-Key"));
//////        config.setMaxAge(3600L);
//////
//////        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//////        source.registerCorsConfiguration("/**", config);
//////
//////        return new CorsWebFilter(source);
//////    }
//////}
////
////package com.flashsale.gateway.config;
////
////import org.springframework.context.annotation.Bean;
////import org.springframework.context.annotation.Configuration;
////import org.springframework.core.Ordered;
////import org.springframework.core.annotation.Order;
////import org.springframework.web.cors.CorsConfiguration;
////import org.springframework.web.cors.reactive.CorsWebFilter;
////import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
////
////import java.util.Arrays;
////import java.util.List;
////
////@Configuration
////public class CorsConfig {
////
////    @Bean
////    @Order(Ordered.HIGHEST_PRECEDENCE)
////    public CorsWebFilter corsWebFilter() {
////
////        CorsConfiguration config = new CorsConfiguration();
////
////        config.setAllowCredentials(true);
////
////        config.setAllowedOrigins(List.of(
////                "http://localhost:3000",
////                "http://localhost:3001",
////                "http://localhost:5173",
////                "http://localhost:5174",
////
////                "http://127.0.0.1:3000",
////                "http://127.0.0.1:3001",
////                "http://127.0.0.1:5173",
////                "http://127.0.0.1:5174"
////        ));
////
////        config.setAllowedMethods(Arrays.asList(
////                "GET",
////                "POST",
////                "PUT",
////                "DELETE",
////                "OPTIONS"
////        ));
////
////        config.setAllowedHeaders(Arrays.asList(
////                "Content-Type",
////                "Authorization",
////                "X-Requested-With",
////                "X-Correlation-Id",
////                "Idempotency-Key",
////                "traceparent"
////        ));
////
////        config.setExposedHeaders(Arrays.asList(
////                "Authorization",
////                "X-Trace-Id",
////                "Idempotency-Key"
////        ));
////
////        config.setMaxAge(3600L);
////
////        UrlBasedCorsConfigurationSource source =
////                new UrlBasedCorsConfigurationSource();
////
////        source.registerCorsConfiguration("/**", config);
////
////        return new CorsWebFilter(source);
////    }
////}
//package com.flashsale.gateway.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.Ordered;
//import org.springframework.core.annotation.Order;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.reactive.CorsWebFilter;
//import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
//
//import java.util.Arrays;
//import java.util.List;
//
//@Configuration
//public class CorsConfig {
//
//    @Bean
//    @Order(Ordered.HIGHEST_PRECEDENCE)
//    public CorsWebFilter corsWebFilter() {
//
//        CorsConfiguration config = new CorsConfiguration();
//
//        config.setAllowCredentials(true);
//
//        config.setAllowedOrigins(List.of(
//                "http://localhost:3000",
//                "http://localhost:3001",
//                "http://localhost:5173",
//                "http://localhost:5174",
//
//                "http://127.0.0.1:3000",
//                "http://127.0.0.1:3001",
//                "http://127.0.0.1:5173",
//                "http://127.0.0.1:5174"
//        ));
//
//        config.setAllowedMethods(Arrays.asList(
//                "GET",
//                "POST",
//                "PUT",
//                "DELETE",
//                "OPTIONS"
//        ));
//
//        config.setAllowedHeaders(Arrays.asList(
//                "Content-Type",
//                "Authorization",
//                "X-Requested-With",
//                "X-Correlation-Id",
//                "Idempotency-Key",
//                "traceparent"
//        ));
//
//        config.setExposedHeaders(Arrays.asList(
//                "Authorization",
//                "X-Trace-Id",
//                "Idempotency-Key"
//        ));
//
//        config.setMaxAge(3600L);
//
//        UrlBasedCorsConfigurationSource source =
//                new UrlBasedCorsConfigurationSource();
//
//        source.registerCorsConfiguration("/**", config);
//
//        return new CorsWebFilter(source);
//    }
//}

package com.flashsale.gateway.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class CorsConfig {
    // CORS is configured in application.yml
}