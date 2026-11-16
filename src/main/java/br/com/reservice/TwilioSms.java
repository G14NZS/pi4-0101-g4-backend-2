package br.com.reservice.backend2;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TwilioSms {

    private final String accountSid;
    private final String authToken;
    private final String from;
    private final HttpClient http;

    public TwilioSms(
            String accountSid,
            String authToken,
            String from) {

        this.accountSid = accountSid;
        this.authToken = authToken;
        this.from = from;
        this.http = HttpClient.newHttpClient();
    }

    public void enviar(
            String destino,
            String mensagem)
            throws IOException, InterruptedException {

        if (accountSid.isBlank()
                || authToken.isBlank()
                || from.isBlank()) {

            throw new IllegalStateException(
                    "Twilio não configurado.");
        }

        String corpo =
                "To=" + encode(destino)
                        + "&From=" + encode(from)
                        + "&Body=" + encode(mensagem);

        String credenciais =
                Base64.getEncoder()
                        .encodeToString(
                                (accountSid + ":" + authToken)
                                        .getBytes(StandardCharsets.UTF_8));

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(
                                "https://api.twilio.com/2010-04-01/Accounts/"
                                        + accountSid
                                        + "/Messages.json"))
                        .header(
                                "Authorization",
                                "Basic " + credenciais)
                        .header(
                                "Content-Type",
                                "application/x-www-form-urlencoded")
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(corpo))
                        .build();

        HttpResponse<String> resposta =
                http.send(
                        request,
                        HttpResponse.BodyHandlers.ofString());

        if (resposta.statusCode() < 200
                || resposta.statusCode() >= 300) {

            throw new IOException(
                    "Twilio respondeu HTTP "
                            + resposta.statusCode());
        }
    }

    private static String encode(String valor) {
        return URLEncoder.encode(
                valor,
                StandardCharsets.UTF_8);
    }
}