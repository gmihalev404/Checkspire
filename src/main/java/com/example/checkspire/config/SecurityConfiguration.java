package com.example.checkspire.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers(
                                        "/",
                                        "/about",
                                        "/login",
                                        "/register",
                                        "/play"
                                )
                                .permitAll()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/tournaments"
                                )
                                .permitAll()

                                .requestMatchers(
                                        "/ws",
                                        "/ws/**",
                                        "/",
                                        "/about",
                                        "/login",
                                        "/register",
                                        "/games/**",
                                        "/friends/**",
                                        "/challenges/**",
                                        "/tournaments/**"
                                )
                                .authenticated()

                                .anyRequest()
                                .permitAll()
                )
                .formLogin(form ->
                        form
                                .loginPage(
                                        "/login"
                                )
                                .defaultSuccessUrl(
                                        "/",
                                        true
                                )
                                .permitAll()
                )
                .logout(
                        Customizer.withDefaults()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }
}