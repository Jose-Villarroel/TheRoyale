package com.theroyale.backend.errors;

// ===== Base de las excepciones de la API que pueden llevar un código de error estable =====
// El código es la identidad del error en el contrato con el frontend (docs/i18n-codigos-error.md):
// está en inglés, nunca se traduce y nunca se muestra. ApiExceptionHandler lo devuelve en el JSON y
// resuelve el texto con MessageSource según el Accept-Language de la petición.
//
// Las subclases conservan su constructor de mensaje plano (codigo == null) para los sitios que no se
// migran (panel de administración, portal del operador, clientes externos): ahí el literal en inglés
// sigue siendo el mensaje y la respuesta sale con "codigo": null.
//
// Cuando sí hay código, getMessage() devuelve el propio código: así los logs y las trazas de las
// pruebas identifican el error sin depender del idioma.
public abstract class ExcepcionConCodigo extends RuntimeException {

    private final String codigo;
    private final Object[] args;

    // Mensaje plano, sin código: el literal viaja tal cual al cliente
    protected ExcepcionConCodigo(String mensaje) {
        super(mensaje);
        this.codigo = null;
        this.args = null;
    }

    // Con código: el texto lo resuelve MessageSource con estos argumentos posicionales ({0}, {1}...)
    protected ExcepcionConCodigo(String codigo, Object[] args, Throwable causa) {
        super(codigo, causa);
        this.codigo = codigo;
        this.args = args == null ? new Object[0] : args;
    }

    public String getCodigo() {
        return codigo;
    }

    public Object[] getArgs() {
        return args == null ? new Object[0] : args.clone();
    }

    public boolean tieneCodigo() {
        return codigo != null;
    }
}
