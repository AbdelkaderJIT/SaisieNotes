package tn.espacenote.notedemo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tn.espacenote.notedemo.dto.ErreurResponse;
import tn.espacenote.notedemo.exception.*;

import java.util.LinkedHashMap;
import java.util.Map;

// Traduit chaque exception en statut HTTP + corps JSON ErreurResponse.
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    // Statut HTTP de chaque règle métier (les exceptions restent indépendantes du web)
    private static final Map<Class<? extends NoteException>, HttpStatus> STATUTS = Map.of(
            RessourceIntrouvableException.class, HttpStatus.NOT_FOUND,
            AccesMatiereRefuseException.class, HttpStatus.FORBIDDEN,
            MatiereClotureeException.class, HttpStatus.CONFLICT,
            NoteDejaExistanteException.class, HttpStatus.CONFLICT,
            EtudiantNonInscritException.class, HttpStatus.UNPROCESSABLE_ENTITY,
            ValeurInvalideException.class, HttpStatus.UNPROCESSABLE_ENTITY);

    @ExceptionHandler(NoteException.class)
    public ResponseEntity<ErreurResponse> regleMetier(NoteException e) {
        return reponse(STATUTS.getOrDefault(e.getClass(), HttpStatus.BAD_REQUEST), e.getMessage());
    }

    // Validation du format (NoteForm) : messages français issus de messages.properties
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErreurResponse> validation(MethodArgumentNotValidException e) {
        Map<String, String> champs = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fe ->
                champs.putIfAbsent(fe.getField(),
                        messageSource.getMessage(fe, LocaleContextHolder.getLocale())));
        return ResponseEntity.badRequest()
                .body(ErreurResponse.of(400, "Données invalides", champs));
    }

    // JSON mal formé, ou valeur non numérique ("abc")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErreurResponse> jsonIllisible(HttpMessageNotReadableException e) {
        return reponse(HttpStatus.BAD_REQUEST, "Corps de requête illisible ou valeur au mauvais format");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErreurResponse> typeInvalide(MethodArgumentTypeMismatchException e) {
        return reponse(HttpStatus.BAD_REQUEST, "Paramètre invalide : " + e.getName());
    }

    // Filet de sécurité : deux saisies simultanées passent l'existsBy, la contrainte unique refuse la seconde
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErreurResponse> contrainte(DataIntegrityViolationException e) {
        return reponse(HttpStatus.CONFLICT, "Conflit avec des données existantes (une note existe peut-être déjà)");
    }

    private ResponseEntity<ErreurResponse> reponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ErreurResponse.of(status.value(), message));
    }
}
