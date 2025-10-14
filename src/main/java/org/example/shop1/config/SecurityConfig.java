package org.example.shop1.config;




import org.example.shop1.model.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    // =======================
    //  Password Encoder
    // =======================
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // =======================
    //  Authentication Manager
    // =======================
    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder())
                .and()
                .build();
    }

    // =======================
    //  Security Filter Chain
    // =======================
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // برای تست، بعدا فعالش می‌کنی
                .authorizeHttpRequests(auth -> auth
                        // این دو مسیر آزاد باشن:
                        .requestMatchers("/userAuth.html", "/users/signup","/home.html").permitAll()
                                .requestMatchers( "/api/current-user").authenticated()


//                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
//                        .requestMatchers("/api/support/**").hasRole("SUPPORT")
//                        .requestMatchers("/api/users/**").hasAnyRole("ADMIN", "SUPPORT")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/userAuth.html") // صفحه HTML ساده
                        .loginProcessingUrl("/perform_login") // مسیر پردازش لاگین (بعدا اضافه می‌کنیم)
                        .defaultSuccessUrl("/users/home", true)
                        .failureUrl("/userAuth.html?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/userAuth.html?logout=true")
                        .permitAll()
                );

        return http.build();
    }

}
