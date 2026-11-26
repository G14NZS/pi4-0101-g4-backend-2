/**
 * O servico de mensagens recusou o envio de vez (telefone invalido, 21211, ou numero
 * nao verificado em conta de teste, 21608). O lembrete e registrado como "falhou" e nao volta.
 */
public class FalhaDefinitivaDeEnvio extends Exception
{
    private int codigo;

    public FalhaDefinitivaDeEnvio (int codigo, String mensagem)
    {
        super (mensagem);
        this.codigo = codigo;
    }

    public int getCodigo ()
    {
        return this.codigo;
    }
}
