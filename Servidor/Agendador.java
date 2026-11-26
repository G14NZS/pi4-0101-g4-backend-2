import java.time.*;
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

    public static final ZoneId FUSO = ZoneId.of ("America/Sao_Paulo");

    public static final String ENVIADO = "enviado";
    public static final String FALHOU  = "falhou";

    private ConexaoComBackend1 backend1;
    private ServicoDeMensagens mensagens;
    private long               intervaloMs;
    private volatile boolean   ativo = true;

    // resultados de envio (id do agendamento -> enviado/falhou) que ainda nao chegaram ao Backend 1
    private LinkedHashMap<Integer,String> registrosPendentes = new LinkedHashMap<Integer,String> ();

    public Agendador (ConexaoComBackend1 backend1, ServicoDeMensagens mensagens, long intervaloMs)
    throws Exception
    {
        if (backend1==null)
            throw new Exception ("Conexao com o Backend 1 ausente");

        if (mensagens==null)
            throw new Exception ("Servico de mensagens ausente");

        if (intervaloMs<=0)
            throw new Exception ("Intervalo invalido");

        this.backend1    = backend1;
        this.mensagens   = mensagens;
        this.intervaloMs = intervaloMs;

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
    }

    /** Um ciclo completo: CT12, depois CT19 e CT13 para cada lembrete. */
    public synchronized void executeCiclo ()
    {
        // 1. resultados de ciclos anteriores que nao chegaram ao Backend 1
        for (Integer id : new ArrayList<Integer> (this.registrosPendentes.keySet()))
        {
            if (!this.registre (id, this.registrosPendentes.get (id)))
                return; // Backend 1 continua fora; tenta no proximo ciclo
        }

        // 2. CT12
        ArrayList<Lembrete> lembretes;
        try
        {
            // o Backend 1 grava e compara datas sem fuso, sempre no horario de Brasilia
            lembretes = this.backend1.listeLembretesPendentes (LocalDateTime.now (FUSO).plusHours (HORAS_A_FRENTE));
        }
        catch (ErroBackend1 erro)
        {
            log ("Backend 1 recusou o pedido de lembretes: " + erro);
            return;
        }
        catch (Exception erro)
        {
            log ("Backend 1 indisponivel; tenta no proximo ciclo (" + erro.getMessage() + ")");
            return;
        }

        log (lembretes.size() + " lembrete(s) para enviar");

        // 3. CT19 + CT13 para cada lembrete
        for (Lembrete lembrete : lembretes)
        {
            if (!this.ativo)
                return;

            String resultado;
            try
            {
                String sid = this.mensagens.envie (lembrete.getTelefone(), lembrete.getTextoDoSms());
                log (lembrete + " enviado (sid " + sid + ")");
                resultado = ENVIADO;
            }
            catch (FalhaDefinitivaDeEnvio erro)
            {
                log (lembrete + " falhou: " + erro.getMessage());
                resultado = FALHOU;
            }
            catch (FalhaTemporariaDeEnvio erro)
            {
                log (lembrete + " nao enviado; tenta no proximo ciclo (" + erro.getMessage() + ")");
                continue; // nada e registrado
            }

            this.registrosPendentes.put (lembrete.getIdAgendamento(), resultado);
            if (!this.registre (lembrete.getIdAgendamento(), resultado))
                return; // socket caiu: o resto dos lembretes volta no proximo ciclo
        }
    }

    /**
     * CT13. Devolve false se nao conseguiu falar com o Backend 1 (o registro continua pendente).
     * Qualquer resposta do Backend 1 tira o registro da lista de pendentes.
     */
    private boolean registre (int idAgendamento, String resultado)
    {
        try
        {
            this.backend1.registreLembrete (idAgendamento, resultado);
            log ("Registrado agendamento " + idAgendamento + ": " + resultado);
        }
        catch (ErroBackend1 erro)
        {
            if (ErroBackend1.LEMBRETE_JA_REGISTRADO.equals (erro.getCodigo()))
                log ("Lembrete ja registrado, ignorado: agendamento " + idAgendamento);
            else
                log ("Backend 1 recusou o registro do agendamento " + idAgendamento + ": " + erro);
        }
        catch (Exception erro)
        {
            log ("Nao foi possivel registrar o agendamento " + idAgendamento +
                 "; registra no proximo ciclo (" + erro.getMessage() + ")");
            return false;
        }

        this.registrosPendentes.remove (idAgendamento);
        return true;
    }

    /** Quantos resultados de envio ainda aguardam o CT13. */
    public synchronized int getQuantidadeDeRegistrosPendentes ()
    {
        return this.registrosPendentes.size ();
    }

    private static void log (String texto)
    {
        System.out.println ("[Agendador " + LocalTime.now().withNano(0) + "] " + texto);
    }
}
