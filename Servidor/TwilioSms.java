import java.net.*;
import java.net.http.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;

/**
 * CT19: envia o SMS pela API do Twilio
 * (POST {base}/2010-04-01/Accounts/{AccountSid}/Messages.json, Basic auth, form-urlencoded).
 */
public class TwilioSms implements ServicoDeMensagens
{
    public static final String URL_PADRAO     = "https://api.twilio.com";
    public static final long   TIMEOUT_PADRAO = 10_000; // ms

    public static final int TELEFONE_INVALIDO     = 21211;
    public static final int NUMERO_NAO_VERIFICADO = 21608;

    private static final Pattern SID    = Pattern.compile ("\"sid\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern CODIGO = Pattern.compile ("\"code\"\\s*:\\s*(\\d+)");
    private static final Pattern MENSAGEM = Pattern.compile ("\"message\"\\s*:\\s*\"([^\"]*)\"");

    private String     urlBase;
    private String     accountSid;
    private String     authToken;
    private String     de;
    private Duration   timeout;
    private HttpClient http;

    public TwilioSms (String accountSid, String authToken, String de)
    {
        this (URL_PADRAO, accountSid, authToken, de, TIMEOUT_PADRAO);
    }

    public TwilioSms (String urlBase, String accountSid, String authToken, String de, long timeoutMs)
    {
        this.urlBase    = urlBase;
        this.accountSid = accountSid==null ? "" : accountSid;
        this.authToken  = authToken ==null ? "" : authToken;
        this.de         = de        ==null ? "" : de;
        this.timeout    = Duration.ofMillis (timeoutMs);
        this.http       = HttpClient.newBuilder ()
                                    .connectTimeout (this.timeout)
                                    .build ();
    }

    public String envie (String para, String texto)
    throws FalhaDefinitivaDeEnvio, FalhaTemporariaDeEnvio
    {
        if (this.accountSid.isBlank() || this.authToken.isBlank() || this.de.isBlank())
            throw new FalhaTemporariaDeEnvio ("Twilio nao configurado (TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_FROM)");

        // Conta trial do Twilio so aceita modelos prontos no Body (erro 572006 com texto livre).
        // Com TWILIO_MODELO_TRIAL (ex.: sms_appointment_reminders), manda o modelo no lugar do texto.
        String modeloTrial = System.getenv ("TWILIO_MODELO_TRIAL");
        String body        = modeloTrial==null || modeloTrial.isBlank() ? texto : modeloTrial.trim();

        String corpo = "To="    + codifique (para)    +
                       "&From=" + codifique (this.de) +
                       "&Body=" + codifique (body);

        String credenciais = Base64.getEncoder ().encodeToString (
                             (this.accountSid + ":" + this.authToken).getBytes (StandardCharsets.UTF_8));

        HttpResponse<String> resposta;
        try
        {
            HttpRequest pedido =
            HttpRequest.newBuilder ()
                       .uri     (URI.create (this.urlBase + "/2010-04-01/Accounts/" + this.accountSid + "/Messages.json"))
                       .timeout (this.timeout)
                       .header  ("Authorization", "Basic " + credenciais)
                       .header  ("Content-Type",  "application/x-www-form-urlencoded")
                       .POST    (HttpRequest.BodyPublishers.ofString (corpo, StandardCharsets.UTF_8))
                       .build   ();

            resposta = this.http.send (pedido, HttpResponse.BodyHandlers.ofString (StandardCharsets.UTF_8));
        }
        catch (HttpTimeoutException erro)
        {
            throw new FalhaTemporariaDeEnvio ("Twilio sem resposta em " + this.timeout.toMillis() + " ms");
        }
        catch (InterruptedException erro)
        {
            Thread.currentThread().interrupt();
            throw new FalhaTemporariaDeEnvio ("Envio interrompido");
        }
        catch (Exception erro) // servico fora do ar, DNS, conexao recusada...
        {
            throw new FalhaTemporariaDeEnvio ("Twilio fora do ar: " + erro);
        }

        int    status = resposta.statusCode ();
        String json   = resposta.body ()==null ? "" : resposta.body ();

        if (status>=200 && status<300)
        {
            Matcher sid = SID.matcher (json);
            return sid.find() ? sid.group(1) : "";
        }

        int codigo = 0;
        Matcher m = CODIGO.matcher (json);
        if (m.find())
            codigo = Integer.parseInt (m.group(1));

        if (status==400 && (codigo==TELEFONE_INVALIDO || codigo==NUMERO_NAO_VERIFICADO))
            throw new FalhaDefinitivaDeEnvio (codigo, "Twilio recusou o telefone (codigo " + codigo + ")");

        // 401/20003 (credencial errada), 5xx e qualquer outro caso: tenta de novo no proximo ciclo
        Matcher msg = MENSAGEM.matcher (json);
        throw new FalhaTemporariaDeEnvio ("Twilio respondeu HTTP " + status + (codigo!=0 ? " (codigo " + codigo + ")" : "") +
                                          (msg.find() ? ": " + msg.group(1) : ""));
    }

    private static String codifique (String valor)
    {
        return URLEncoder.encode (valor==null ? "" : valor, StandardCharsets.UTF_8);
    }
}
