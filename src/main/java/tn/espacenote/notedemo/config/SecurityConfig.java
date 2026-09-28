package tn.espacenote.notedemo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

// Authentification HTTP Basic sans état : chaque requête porte ses identifiants,
// pas de session ni de cookie (adapté à Postman et à un futur front Angular).
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)   // sans cookie de session, pas de risque CSRF
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()   // préflight CORS d'Angular
                    .requestMatchers("/api/**").authenticated()
                    // Fichiers de l'application Angular (pages, JS, CSS, images), lecture seule et publics :
                    // ils ne contiennent aucune donnée. Toutes les données passent par /api, protégé ci-dessus
                    // (les règles sont évaluées dans l'ordre : /api/** est traité avant ce GET général).
                    .requestMatchers(HttpMethod.GET, "/**").permitAll()
                    .anyRequest().denyAll())
            // 401 sans en-tête WWW-Authenticate : sinon le navigateur ouvre sa propre fenêtre de
            // connexion par-dessus la page de login Angular quand le mot de passe est faux.
            .httpBasic(b -> b.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Deux comptes de démonstration. Le nom d'utilisateur est l'email de l'Enseignant en base,
    // ce qui relie l'utilisateur connecté à ses matières (voir NoteService.idEnseignantPar).
    // L'Enseignant n'a pas de champ mot de passe : l'authentification reste hors du modèle métier.
    // Les mots de passe viennent de la configuration (variables d'environnement en production).
    @Bean
    UserDetailsService userDetailsService(PasswordEncoder encoder,
                                          @Value("${app.demo.ali-password}") String motDePasseAli,
                                          @Value("${app.demo.sonia-password}") String motDePasseSonia) {
        return new InMemoryUserDetailsManager(
                User.withUsername("ali.benali@fds.tn").password(encoder.encode(motDePasseAli)).roles("ENSEIGNANT").build(),
                User.withUsername("sonia.trabelsi@fds.tn").password(encoder.encode(motDePasseSonia)).roles("ENSEIGNANT").build());
    }
}
