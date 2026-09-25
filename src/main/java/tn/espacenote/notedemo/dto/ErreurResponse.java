package tn.espacenote.notedemo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

// Corps JSON de toutes les erreurs. "champs" n'apparaît que pour les erreurs de validation.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErreurResponse(int status, String message, Map<String, String> champs, LocalDateTime timestamp) {

    public static ErreurResponse of(int status, String message) {
        return new ErreurResponse(status, message, null, LocalDateTime.now());
    }

    public static ErreurResponse of(int status, String message, Map<String, String> champs) {
        return new ErreurResponse(status, message, champs, LocalDateTime.now());
    }
}
