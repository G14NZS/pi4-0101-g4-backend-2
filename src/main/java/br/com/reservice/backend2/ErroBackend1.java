package br.com.reservice.backend2;

/** O Backend 1 respondeu {@code ok: false} (por exemplo ACESSO_NEGADO). */
public class ErroBackend1 extends Exception {

    private final String codigo;

    public ErroBackend1(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
