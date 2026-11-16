/** Resposta de erro do Backend 1 (CT12 ou CT13). */
public class RespostaDeErro extends Comunicado
{
    public static final String LEMBRETE_JA_REGISTRADO = "LEMBRETE_JA_REGISTRADO";
    public static final String ACESSO_NEGADO          = "ACESSO_NEGADO";

    private String codigo;
    private String mensagem;

    public RespostaDeErro (String codigo, String mensagem)
    {
        this.codigo   = codigo;
        this.mensagem = mensagem;
    }

    public String getCodigo   () { return this.codigo;   }
    public String getMensagem () { return this.mensagem; }

    public String toString ()
    {
        return this.codigo + (this.mensagem!=null ? ": " + this.mensagem : "");
    }
}
