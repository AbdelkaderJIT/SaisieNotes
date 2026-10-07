package tn.espacenote.notedemo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Métadonnées Swagger UI (/swagger-ui/index.html) et bouton "Authorize" : HTTP Basic, les mêmes
// identifiants que Postman (un compte ENSEIGNANT pour /api, le compte ADMIN pour /api/admin).
@Configuration
public class OpenApiConfig {

    private static final String SCHEMA_BASIC = "basicAuth";

    @Bean
    OpenAPI api() {
        return new OpenAPI()
                .info(new Info()
                        .title("Saisie des notes — API")
                        .version("1.0")
                        .description("""
                                API de la Faculté de Droit de Sfax. Deux espaces :
                                - /api/** (rôle ENSEIGNANT) : examens affectés, saisie et modification des notes.
                                - /api/admin/** (rôle ADMIN) : création, modification et suppression des examens,
                                  et listes de référence (enseignants, étudiants, matières, groupes).

                                Utilisez le bouton Authorize avec un des comptes de démonstration."""))
                .components(new Components().addSecuritySchemes(SCHEMA_BASIC,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEMA_BASIC));
    }
}
