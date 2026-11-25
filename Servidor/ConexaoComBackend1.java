import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

/**
 * Cliente por socket TCP do Backend 1 (CT12 e CT13). Protocolo: uma linha JSON por mensagem.
 *   pedido   {"operacao":..., "dados":{...}, "chaveInterna":...}
 *   resposta {"ok":true,"dados":{...}}  ou  {"ok":false,"erro":CODIGO,"mensagem":texto}
 *
 * Cada chamada abre a sua conexao e a fecha ao terminar: o Backend 1 derruba conexoes ociosas
 * por mais de 10 s e o ciclo do agendador e de 15 minutos, entao nao ha conexao para manter viva.
 */
public class ConexaoComBackend1
{
    public static final int TIMEOUT_PADRAO = 10_000; // ms

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern ("yyyy-MM-dd'T'HH:mm:ss");

    private String host;
    private int    porta;
    private String chaveInterna;
    private int    timeoutMs;

    public ConexaoComBackend1 (String host, int porta, String chaveInterna) throws Exception
    {
        this (host, porta, chaveInterna, TIMEOUT_PADRAO);
    }

    public ConexaoComBackend1 (String host, int porta, String chaveInterna, int timeoutMs) throws Exception
    {
        if (host==null || host.isBlank())
            throw new Exception ("Host ausente");

        if (porta<=0 || porta>65535)
            throw new Exception ("Porta invalida");

        if (chaveInterna==null || chaveInterna.isBlank())
            throw new Exception ("Chave interna ausente");

        this.host         = host;
        this.porta        = porta;
        this.chaveInterna = chaveInterna;
        this.timeoutMs    = timeoutMs;
    }

    /** Operacao "ping": confirma que o Backend 1 responde e aceita a chave. */
    public void ping () throws IOException, ErroBackend1
    {
        this.chame ("ping", "{}");
    }

    /** CT12: lembretes cujo agendamento comeca ate o instante dado e ainda nao foram registrados. */
    public ArrayList<Lembrete> listeLembretesPendentes (LocalDateTime ate) throws IOException, ErroBackend1
    {
        Map<?,?> dados = this.chame ("listarLembretesPendentes",
                                     "{\"ate\":" + Json.texto (ate.format (DATA_HORA)) + "}");

        ArrayList<Lembrete> lembretes = new ArrayList<Lembrete> ();
        Object lista = dados.get ("lembretes");
        if (!(lista instanceof List))
            throw new IOException ("Resposta do Backend 1 sem a lista de lembretes");

        for (Object item : (List<?>)lista)
        {
            try
            {
                lembretes.add (Lembrete.doJson ((Map<?,?>)item));
            }
            catch (Exception erro)
            {
                System.err.println ("Lembrete ignorado (dados invalidos): " + erro.getMessage());
            }
        }
        return lembretes;
    }

    /** CT13: registra o resultado do envio ("enviado" ou "falhou"). */
    public void registreLembrete (int idAgendamento, String resultado) throws IOException, ErroBackend1
    {
        this.chame ("registrarLembrete",
                    "{\"agendamentoId\":" + idAgendamento + ",\"resultado\":" + Json.texto (resultado) + "}");
    }

    /** Devolve o campo "dados" da resposta; IOException se nao deu para falar, ErroBackend1 se foi recusado. */
    private Map<?,?> chame (String operacao, String dadosJson) throws IOException, ErroBackend1
    {
        String pedido = "{\"operacao\":" + Json.texto (operacao) +
                        ",\"dados\":"    + dadosJson +
                        ",\"chaveInterna\":" + Json.texto (this.chaveInterna) + "}";

        String linha;
        try (Socket conexao = new Socket ())
        {
            conexao.connect      (new InetSocketAddress (this.host, this.porta), this.timeoutMs);
            conexao.setSoTimeout (this.timeoutMs); // sem resposta em 10 s conta como queda

            BufferedWriter saida   = new BufferedWriter (new OutputStreamWriter (conexao.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader entrada = new BufferedReader (new InputStreamReader  (conexao.getInputStream (), StandardCharsets.UTF_8));

            saida.write (pedido);
            saida.write ("\n");
            saida.flush ();

            linha = entrada.readLine ();
        }

        if (linha==null)
            throw new IOException ("O Backend 1 fechou a conexao sem responder");

        Object resposta;
        try
        {
            resposta = Json.leia (linha);
        }
        catch (Exception erro)
        {
            throw new IOException ("Resposta invalida do Backend 1: " + erro.getMessage());
        }

        if (!(resposta instanceof Map))
            throw new IOException ("Resposta invalida do Backend 1");

        Map<?,?> mapa = (Map<?,?>)resposta;
        if (Boolean.TRUE.equals (mapa.get ("ok")))
        {
            Object dados = mapa.get ("dados");
            return dados instanceof Map ? (Map<?,?>)dados : new HashMap<Object,Object> ();
        }

        Object erro     = mapa.get ("erro");
        Object mensagem = mapa.get ("mensagem");
        throw new ErroBackend1 (erro instanceof String ? (String)erro : "ERRO_DESCONHECIDO",
                                mensagem instanceof String ? (String)mensagem : "");
    }
}
