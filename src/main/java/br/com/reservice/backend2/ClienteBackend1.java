package br.com.reservice.backend2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Cliente do socket TCP do Backend 1 (CT12 e CT13). Uma mensagem JSON por linha:
 * pedido {@code {operacao, dados, chaveInterna}}; resposta {@code {ok:true, dados}} ou {@code {ok:false, erro, mensagem}}.
 */
public class ClienteBackend1 {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int TIMEOUT_MS = 10_000;

    private final String host;
    private final int porta;
    private final String chaveInterna;

    public ClienteBackend1(String host, int porta, String chaveInterna) {
        this.host = host;
        this.porta = porta;
        this.chaveInterna = chaveInterna;
    }

    /** Envia a operação e devolve o campo {@code dados} da resposta. */
    public JsonNode chamar(String operacao, JsonNode dados) throws IOException, ErroBackend1 {
        ObjectNode pedido = JSON.createObjectNode();
        pedido.put("operacao", operacao);
        pedido.set("dados", dados == null ? JSON.createObjectNode() : dados);
        pedido.put("chaveInterna", chaveInterna);

        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, porta), TIMEOUT_MS);
            s.setSoTimeout(TIMEOUT_MS);
            BufferedWriter saida = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader entrada = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            saida.write(JSON.writeValueAsString(pedido));
            saida.write("\n");
            saida.flush();

            String linha = entrada.readLine();
            if (linha == null) {
                throw new IOException("O Backend 1 fechou a conexão sem responder");
            }
            JsonNode resposta = JSON.readTree(linha);
            if (resposta.path("ok").asBoolean(false)) {
                return resposta.path("dados");
            }
            throw new ErroBackend1(resposta.path("erro").asText("ERRO_DESCONHECIDO"),
                    resposta.path("mensagem").asText(""));
        }
    }
}
