import java.util.*;

/**
 * H08.2: a cada 15 minutos pede ao Backend 1 os lembretes das proximas 3 horas (CT12),
 * manda o SMS de cada um (CT19) e registra o resultado (CT13).
 *
 *  - Telefone invalido / nao verificado: registra "falhou" e o lembrete nao volta.
 *  - Credencial errada, servico fora do ar ou sem resposta em 10 s: nao registra nada;
 *    o Backend 1 devolve o lembrete de novo no ciclo seguinte.
 *  - "Lembrete ja registrado": ignorado.
 *  - Socket caiu: o ciclo termina e o seguinte conecta de novo. Se o SMS ja tinha saido,
 *    o resultado fica guardado e e registrado no ciclo seguinte, sem mandar outro SMS.
 */
public class Agendador extends Thread
{
    public static final long INTERVALO_PADRAO = 15 * 60 * 1000L; // 15 minutos
    public static final int  HORAS_A_FRENTE   = 3;

    private ConexaoComBackend1 backend1;
    private ServicoDeMensagens mensagens;
    private String             chaveInterna;
    private long               intervaloMs;
    private volatile boolean   ativo = true;

    // resultados de envio que ainda nao chegaram ao Backend 1 (o socket caiu antes do CT13)
    private LinkedHashMap<Integer,PedidoDeRegistroDeLembrete> registrosPendentes =
        new LinkedHashMap<Integer,PedidoDeRegistroDeLembrete> ();

    public Agendador (ConexaoComBackend1 backend1, ServicoDeMensagens mensagens,
                      String chaveInterna, long intervaloMs)
    throws Exception
    {
        if (backend1==null)
            throw new Exception ("Conexao com o Backend 1 ausente");

        if (mensagens==null)
            throw new Exception ("Servico de mensagens ausente");

        if (intervaloMs<=0)
            throw new Exception ("Intervalo invalido");

        this.backend1     = backend1;
        this.mensagens    = mensagens;
        this.chaveInterna = chaveInterna;
        this.intervaloMs  = intervaloMs;

        this.setName   ("agendador-de-lembretes");
        this.setDaemon (true);
    }

    public void run ()
    {
        while (this.ativo)
        {
            try
            {
                this.executeCiclo ();
            }
            catch (Exception erro) // nunca deixa a thread morrer por causa de um ciclo
            {
                log ("Erro inesperado no ciclo: " + erro);
            }

            try
            {
                Thread.sleep (this.intervaloMs);
            }
            catch (InterruptedException erro)
            {
                break;
            }
        }
    }

    public void desative ()
    {
        this.ativo = false;
        this.interrupt ();
        this.backend1.descarte ();
    }

    /** Um ciclo completo: CT12, depois CT19 e CT13 para cada lembrete. */
    public synchronized void executeCiclo ()
    {
        // 1. resultados de ciclos anteriores que nao chegaram ao Backend 1
        for (Integer id : new ArrayList<Integer> (this.registrosPendentes.keySet()))
        {
            if (!this.registre (this.registrosPendentes.get (id)))
                return; // Backend 1 continua fora; tenta no proximo ciclo
        }

        // 2. CT12
        Comunicado resposta;
        try
        {
            resposta = this.backend1.peca (new PedidoDeLembretes (this.chaveInterna, HORAS_A_FRENTE));
        }
        catch (Exception erro)
        {
            log ("Backend 1 indisponivel; tenta no proximo ciclo (" + erro.getMessage() + ")");
            return;
        }

        if (resposta instanceof RespostaDeErro)
        {
            log ("Backend 1 recusou o pedido de lembretes: " + resposta);
            return;
        }

        if (!(resposta instanceof RespostaDeLembretes))
        {
            log ("Resposta inesperada do Backend 1 ao pedido de lembretes");
            this.backend1.descarte (); // conversa fora de sincronia
            return;
        }

        ArrayList<Lembrete> lembretes = ((RespostaDeLembretes)resposta).getLembretes();
        log (lembretes.size() + " lembrete(s) para enviar");

        // 3. CT19 + CT13 para cada lembrete
        for (Lembrete lembrete : lembretes)
        {
            if (!this.ativo)
                return;

            PedidoDeRegistroDeLembrete registro;
            try
            {
                String sid = this.mensagens.envie (lembrete.getTelefone(), lembrete.getTextoDoSms());
                registro = new PedidoDeRegistroDeLembrete (this.chaveInterna, lembrete.getIdAgendamento(),
                                                           PedidoDeRegistroDeLembrete.ENVIADO, sid, null);
            }
            catch (FalhaDefinitivaDeEnvio erro)
            {
                registro = new PedidoDeRegistroDeLembrete (this.chaveInterna, lembrete.getIdAgendamento(),
                                                           PedidoDeRegistroDeLembrete.FALHOU, null, erro.getMessage());
            }
            catch (FalhaTemporariaDeEnvio erro)
            {
                log (lembrete + " nao enviado; tenta no proximo ciclo (" + erro.getMessage() + ")");
                continue; // nada e registrado
            }

            this.registrosPendentes.put (lembrete.getIdAgendamento(), registro);
            if (!this.registre (registro))
                return; // socket caiu: o resto dos lembretes volta no proximo ciclo
        }
    }

    /**
     * CT13. Devolve false se nao conseguiu falar com o Backend 1 (o registro continua pendente).
     * Qualquer resposta do Backend 1 tira o registro da lista de pendentes.
     */
    private boolean registre (PedidoDeRegistroDeLembrete registro)
    {
        Comunicado resposta;
        try
        {
            resposta = this.backend1.peca (registro);
        }
        catch (Exception erro)
        {
            log ("Nao foi possivel registrar " + registro + "; registra no proximo ciclo (" + erro.getMessage() + ")");
            return false;
        }

        this.registrosPendentes.remove (registro.getIdAgendamento());

        if (resposta instanceof RespostaDeRegistro)
            log ("Registrado " + registro);
        else if (resposta instanceof RespostaDeErro &&
                 RespostaDeErro.LEMBRETE_JA_REGISTRADO.equals (((RespostaDeErro)resposta).getCodigo()))
            log ("Lembrete ja registrado, ignorado: agendamento " + registro.getIdAgendamento());
        else if (resposta instanceof RespostaDeErro)
            log ("Backend 1 recusou o registro de " + registro + ": " + resposta);
        else
        {
            log ("Resposta inesperada do Backend 1 ao registro de " + registro);
            this.backend1.descarte ();
        }

        return true;
    }

    /** Quantos resultados de envio ainda aguardam o CT13. */
    public synchronized int getQuantidadeDeRegistrosPendentes ()
    {
        return this.registrosPendentes.size ();
    }

    private static void log (String texto)
    {
        System.out.println ("[Agendador " + java.time.LocalTime.now().withNano(0) + "] " + texto);
    }
}
