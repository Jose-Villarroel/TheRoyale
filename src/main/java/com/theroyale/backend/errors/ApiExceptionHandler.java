package com.theroyale.backend.errors;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

// ===== Equivalente de GlobalExceptionHandler para la API REST =====
// Devuelve { "status": 404, "codigo": "...", "message": "..." }, que es lo que leen los componentes
// de Angular en err.error?.message. Sin esto, Angular recibiria el HTML de error.html.
//
// El "codigo" es el contrato con el frontend (docs/i18n-codigos-error.md): estable, en ingles, nunca
// se traduce. El "message" se resuelve con MessageSource segun el Locale que dejo el
// AcceptHeaderLocaleResolver de I18nConfig, asi que es el texto de respaldo ya en el idioma pedido.
// Las excepciones que no se migran (panel de admin, portal del operador, clientes externos) no traen
// codigo: la respuesta sale con "codigo": null y con su literal en ingles como message.
@RestControllerAdvice(basePackages = "com.theroyale.backend.api")
public class ApiExceptionHandler {

    private final MessageSource messageSource;

    public ApiExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> manejarRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        return respuesta(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<Map<String, Object>> manejarAccesoDenegado(AccesoDenegadoException ex) {
        return respuesta(HttpStatus.FORBIDDEN, ex);
    }

    // El estado viaja en la propia excepcion: 400, 409, 502... (ver ReglaNegocioException)
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Map<String, Object>> manejarReglaNegocio(ReglaNegocioException ex) {
        return respuesta(ex.getEstado(), ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> manejarArgumentoInvalido(IllegalArgumentException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, null, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> manejarEstadoInvalido(IllegalStateException ex) {
        return respuesta(HttpStatus.CONFLICT, null, ex.getMessage());
    }

    @ExceptionHandler(LimiteExcedidoException.class)
    public ResponseEntity<Map<String, Object>> manejarLimiteExcedido(LimiteExcedidoException ex) {
        return respuesta(HttpStatus.TOO_MANY_REQUESTS, ex);
    }

    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> manejarServicioNoDisponible(ServicioNoDisponibleException ex) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, ex);
    }

    @ExceptionHandler(PlanNoConfiableException.class)
    public ResponseEntity<Map<String, Object>> manejarPlanNoConfiable(PlanNoConfiableException ex) {
        return respuesta(HttpStatus.BAD_GATEWAY, ex);
    }

    // 404 con el username como parametro. En el login no se usa: ahi se responde 401
    // auth.credenciales-invalidas para no revelar si el correo esta registrado (ver
    // UsuarioNoEncontradoException y AutenticacionRestController).
    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> manejarUsuarioNoEncontrado(UsuarioNoEncontradoException ex) {
        return respuesta(HttpStatus.NOT_FOUND, "auth.usuario-no-encontrado",
                mensaje("auth.usuario-no-encontrado", ex.getUsername()));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, Object>> manejarUsernameNoEncontrado(UsernameNotFoundException ex) {
        return respuesta(HttpStatus.UNAUTHORIZED, "auth.credenciales-invalidas",
                mensaje("auth.credenciales-invalidas"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> manejarIntegridadDatos(DataIntegrityViolationException ex) {
        return respuesta(HttpStatus.CONFLICT, null, mensaje("error.integridad-datos"));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<Map<String, Object>> manejarParametrosInvalidos(Exception ex) {
        return respuesta(HttpStatus.BAD_REQUEST, null, mensaje("error.parametros-invalidos"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> manejarCuerpoInvalido(HttpMessageNotReadableException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, null, mensaje("error.cuerpo-invalido"));
    }

    // Con codigo, el texto sale de MessageSource; sin codigo, del literal de la excepcion
    private ResponseEntity<Map<String, Object>> respuesta(HttpStatusCode estado, ExcepcionConCodigo ex) {
        if (!ex.tieneCodigo()) {
            return respuesta(estado, null, ex.getMessage());
        }
        return respuesta(estado, ex.getCodigo(), mensaje(ex.getCodigo(), ex.getArgs()));
    }

    private String mensaje(String codigo, Object... args) {
        Locale idioma = LocaleContextHolder.getLocale();
        // El tercer argumento es el respaldo: si falta la clave se devuelve el propio codigo en vez
        // de que la peticion falle con NoSuchMessageException.
        return messageSource.getMessage(codigo, textos(args), codigo, idioma);
    }

    // MessageFormat formatea los numeros segun el idioma: un {0} con 1000 saldria "1,000" en ingles
    // y "1.000" en espanol, y un saldo con decimales cambiaria de separador. Los ids, los limites y
    // los montos se pasan ya como texto para que se vean igual en los dos idiomas (los precios van
    // en formato en-US en ambos, por decisión de producto: ver docs/i18n-glosario.md).
    private static Object[] textos(Object[] args) {
        if (args == null || args.length == 0) {
            return new Object[0];
        }
        Object[] convertidos = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object valor = args[i];
            if (valor instanceof java.math.BigDecimal decimal) {
                convertidos[i] = decimal.toPlainString();
            } else if (valor instanceof Number numero) {
                convertidos[i] = numero.toString();
            } else {
                convertidos[i] = valor;
            }
        }
        return convertidos;
    }

    // El Content-Type se fija en JSON para que el error se pueda escribir aunque el cliente
    // pida text/event-stream (POST /api/chat); sin esto Spring respondería 406.
    // LinkedHashMap y no Map.of: el contrato manda "codigo": null en lo que no se migra y Map.of
    // rechaza los valores nulos (NullPointerException). El orden de las claves tambien se conserva.
    private ResponseEntity<Map<String, Object>> respuesta(HttpStatusCode estado, String codigo, String mensaje) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("status", estado.value());
        cuerpo.put("codigo", codigo);
        cuerpo.put("message", mensaje);
        return ResponseEntity.status(estado)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo);
    }
}
