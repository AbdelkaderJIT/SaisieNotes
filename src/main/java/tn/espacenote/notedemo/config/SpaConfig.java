package tn.espacenote.notedemo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

// Sert l'application Angular (copiée dans src/main/resources/static lors du build Docker) depuis Spring Boot.
// Angular gère ses pages côté navigateur (/login, /matieres/1/notes...) : le serveur ne connaît pas ces URLs.
// Si on actualise la page sur l'une d'elles, il faut donc renvoyer index.html au lieu d'une erreur 404,
// et Angular affiche alors la bonne page.
@Configuration
public class SpaConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource demande = location.createRelative(resourcePath);
                        if (demande.exists() && demande.isReadable()) {
                            return demande;                   // un vrai fichier : main-xxx.js, styles, image...
                        }
                        // Une URL d'API inconnue, ou un fichier manquant (avec une extension), reste une 404
                        if (resourcePath.startsWith("api/") || resourcePath.contains(".")) {
                            return null;
                        }
                        // Une page d'Angular : on renvoie l'application (s'il y en a une, absente en développement)
                        Resource index = new ClassPathResource("static/index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}
