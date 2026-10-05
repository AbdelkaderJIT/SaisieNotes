package tn.espacenote.notedemo.examensource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

// Appelle le webservice d'examens en HTTP, comme s'il s'agissait d'un système externe.
// Changer app.examens.source-url suffit, le jour venu, pour pointer vers un vrai système universitaire :
// aucun autre code de l'application (service, contrôleur, frontend) n'a besoin de changer.
@Component
public class ExamenSourceClient {

    private final RestClient restClient = RestClient.create();
    private final String sourceUrl;

    public ExamenSourceClient(@Value("${app.examens.source-url}") String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public List<ExamenSourceDto> recuperer() {
        ExamenSourceDto[] reponse = restClient.get().uri(sourceUrl).retrieve().body(ExamenSourceDto[].class);
        return reponse == null ? List.of() : List.of(reponse);
    }
}
