package br.com.reservice.backend2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** H01.3 (lado do Backend 2): conversa com um servidor de mentira, sem depender do Backend 1. */
class ClienteBackend1Test {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CHAVE = "chave-interna-de-teste-123456";

    /** Aceita uma conexão, guarda a linha recebida e responde com {@code resposta}. */
    private static CompletableFuture<String> servidorQueResponde(ServerSocket servidor, String resposta) {
        return CompletableFuture.supplyAsync(() -> {
            try (Socket s = servidor.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                 BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8))) {
                String recebida = in.readLine();
                out.write(resposta + "\n");
                out.flush();
                return recebida;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void enviaOperacaoDadosEChaveInternaEDevolveOsDados() throws Exception {
        try (ServerSocket servidor = new ServerSocket(0)) {
            var recebida = servidorQueResponde(servidor, "{\"ok\":true,\"dados\":{\"pong\":true}}");
            var cliente = new ClienteBackend1("localhost", servidor.getLocalPort(), CHAVE);

            JsonNode dados = cliente.chamar("ping", JSON.createObjectNode().put("x", 1));

            assertTrue(dados.get("pong").asBoolean());
            JsonNode pedido = JSON.readTree(recebida.get());
            assertEquals("ping", pedido.get("operacao").asText());
            assertEquals(1, pedido.get("dados").get("x").asInt());
            assertEquals(CHAVE, pedido.get("chaveInterna").asText());
        }
    }

    @Test
    void respostaDeErroViraErroBackend1() throws Exception {
        try (ServerSocket servidor = new ServerSocket(0)) {
            servidorQueResponde(servidor, "{\"ok\":false,\"erro\":\"ACESSO_NEGADO\",\"mensagem\":\"Chave interna ausente ou errada.\"}");
            var cliente = new ClienteBackend1("localhost", servidor.getLocalPort(), "chave-errada-123456789");

            var e = assertThrows(ErroBackend1.class, () -> cliente.chamar("ping", null));

            assertEquals("ACESSO_NEGADO", e.codigo());
        }
    }

    @Test
    void configExigeChaveInterna() {
        assertThrows(IllegalStateException.class, () -> Config.de(java.util.Map.of()));
        assertThrows(IllegalStateException.class, () -> Config.de(java.util.Map.of("CHAVE_INTERNA", "curta")));
        assertEquals(9090, Config.de(java.util.Map.of("CHAVE_INTERNA", CHAVE)).backend1Porta());
    }
}
