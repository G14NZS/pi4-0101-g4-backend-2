/** O Backend 1 respondeu {"ok":false,...}: o pedido chegou, mas foi recusado (ACESSO_NEGADO, LEMBRETE_JA_REGISTRADO...). */
public class ErroBackend1 extends Exception
{
    public static final String LEMBRETE_JA_REGISTRADO = "LEMBRETE_JA_REGISTRADO";

    private String codigo;

    public ErroBackend1 (String codigo, String mensagem)
    {
        super (mensagem);
        this.codigo = codigo;
    }

    public String getCodigo ()
    {
        return this.codigo;
    }

    public String toString ()
    {
        return this.codigo + ": " + this.getMessage ();
    }
}
