package com.example.coopachat.exceptions;

import com.example.coopachat.dtos.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Gérer EmailAlreadyExistsException
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDTO> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                ex.getMessage(),
                null,
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value()
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    // Gérer PhoneAlreadyExistsException
    @ExceptionHandler(PhoneAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDTO> handlePhoneAlreadyExists(PhoneAlreadyExistsException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                ex.getMessage(),
                null,
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value()
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    // Gérer ResourceNotFoundException
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFound(ResourceNotFoundException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                ex.getMessage(),
                null,
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /** Aucun contrôleur ne correspond à la requête (ex. mauvaise URL ou méthode HTTP). */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoResourceFound(NoResourceFoundException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                "Ressource non trouvée: " + ex.getResourcePath() + ". Vérifiez l'URL et la méthode (GET/POST/PUT/DELETE).",
                null,
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /** Négociation de contenu : le client demande un format que l'API ne produit pas (ex. XML). */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponseDTO> handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                "Format de réponse non accepté. Utilisez Accept: application/json",
                null,
                LocalDateTime.now(),
                HttpStatus.NOT_ACCEPTABLE.value()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_ACCEPTABLE);
    }

    // Gérer les erreurs de validation (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> errors = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.add(fieldName + ": " + errorMessage);
        });

        ErrorResponseDTO error = new ErrorResponseDTO(
                "Erreurs de validation",
                errors,
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // Gérer les erreurs d'authentification (RuntimeException avec messages spécifiques)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthenticationException(RuntimeException ex) {
        // Vérifier si c'est une erreur d'authentification
        String message = ex.getMessage();
        if (message != null && (
                message.contains("Email ou mot de passe incorrect") ||
                        message.contains("compte n'est pas actif") ||
                        message.contains("compte est inactif")
        )) {
            ErrorResponseDTO error = new ErrorResponseDTO(
                    ex.getMessage(),
                    null,
                    LocalDateTime.now(),
                    HttpStatus.UNAUTHORIZED.value()
            );
            return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
        }

        // Pour les autres RuntimeException, retourner 500 (éviter d'afficher "null" si pas de message)
        String msg = ex.getMessage() != null ? ex.getMessage() : "Erreur inconnue";
        ErrorResponseDTO error = new ErrorResponseDTO(
                "Une erreur inattendue s'est produite: " + msg,
                null,
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Gérer toutes les autres exceptions (fallback)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Erreur inconnue";
        ErrorResponseDTO error = new ErrorResponseDTO(
                "Une erreur inattendue s'est produite: " + msg,
                null,
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}