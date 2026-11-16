import java.io.*;
import java.net.*;

/**
 * Conexao por socket com o Backend 1, usada pelo Agendador (CT12 e CT13).
 * Abre a conexao quando precisa; se o socket cair (ou o Backend 1 nao responder a tempo),
 * descarta a conexao para que o proximo pedido conecte de novo, sem reiniciar nenhum backend.
 */
public class ConexaoComBackend1
{
    public static final int TIMEOUT_PADRAO = 10_000; // ms

    private String   host;
    private int      porta;
    private int      timeoutMs;
    private Parceiro backend1 = null;

    public ConexaoComBackend1 (String host, int porta) throws Exception
    {
        this (host, porta, TIMEOUT_PADRAO);
    }

    public ConexaoComBackend1 (String host, int porta, int timeoutMs) throws Exception
    {
        if (host==null || host.isBlank())
            throw new Exception ("Host ausente");

        if (porta<=0 || porta>65535)
            throw new Exception ("Porta invalida");

        this.host      = host;
        this.porta     = porta;
        this.timeoutMs = timeoutMs;
    }

    /** Manda o pedido e espera a resposta. Qualquer falha de comunicacao derruba a conexao e e relancada. */
    public synchronized Comunicado peca (Comunicado pedido) throws Exception
    {
        try
        {
            if (this.backend1==null)
                this.conecte ();

            this.backend1.receba (pedido);
            Comunicado resposta = this.backend1.envie ();

            if (resposta==null || resposta instanceof ComunicadoDeDesligamento)
                throw new Exception ("Backend 1 encerrou a conexao");

            return resposta;
        }
        catch (Exception erro)
        {
            this.descarte ();
            throw erro;
        }
    }

    public synchronized boolean estaConectado ()
    {
        return this.backend1!=null;
    }

    public synchronized void descarte ()
    {
        if (this.backend1==null)
            return;

        try
        {
            this.backend1.adeus ();
        }
        catch (Exception erro)
        {} // so tentando fechar; a conexao ja pode estar morta

        this.backend1 = null;
    }

    private void conecte () throws Exception
    {
        Socket conexao = new Socket ();
        try
        {
            conexao.connect      (new InetSocketAddress (this.host, this.porta), this.timeoutMs);
            conexao.setSoTimeout (this.timeoutMs); // sem resposta em 10 s conta como queda

            ObjectOutputStream transmissor = new ObjectOutputStream (conexao.getOutputStream());
            transmissor.flush ();
            ObjectInputStream  receptor    = new ObjectInputStream  (conexao.getInputStream ());

            this.backend1 = new Parceiro (conexao, receptor, transmissor);
        }
        catch (Exception erro)
        {
            try
            {
                conexao.close ();
            }
            catch (Exception falha)
            {}

            throw new Exception ("Nao foi possivel conectar ao Backend 1 em " + this.host + ":" + this.porta);
        }
    }
}
