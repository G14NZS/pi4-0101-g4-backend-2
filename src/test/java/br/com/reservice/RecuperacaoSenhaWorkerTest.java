package br.com.reservice.backend2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class RecuperacaoSenhaWorkerTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void enviaSmsERegistraSucesso() {

        Backend1Fake backend1 = new Backend1Fake();

        ObjectNode pendente = JSON.createObjectNode();
        pendente.put("disponivel", true);
        pendente.put("recuperacaoId", 10);
        pendente.put("telefone", "+5519999999999");
        pendente.put("codigo", "123456");

        backend1.pendente = pendente;

        SmsFake sms = new SmsFake();

        RecuperacaoSenhaWorker worker =
                new RecuperacaoSenhaWorker(
                        backend1,
                        sms);

        worker.run();

        assertEquals(
                "+5519999999999",
                sms.destino);

        assertTrue(
                sms.mensagem.contains("123456"));

        assertNotNull(
                backend1.resultado);

        assertEquals(
                10,
                backend1.resultado
                        .path("recuperacaoId")
                        .asInt());

        assertTrue(
                backend1.resultado
                        .path("sucesso")
                        .asBoolean());
    }

    @Test
    void semRecuperacaoPendenteNaoEnviaSms() {

        Backend1Fake backend1 = new Backend1Fake();

        backend1.pendente =
                JSON.createObjectNode()
                        .put("disponivel", false);

        SmsFake sms = new SmsFake();

        RecuperacaoSenhaWorker worker =
                new RecuperacaoSenhaWorker(
                        backend1,
                        sms);

        worker.run();

        assertNull(sms.destino);
        assertNull(backend1.resultado);
    }

    @Test
    void falhaNoSmsEhRegistradaNoBackend1() {

        Backend1Fake backend1 = new Backend1Fake();

        ObjectNode pendente = JSON.createObjectNode();
        pendente.put("disponivel", true);
        pendente.put("recuperacaoId", 20);
        pendente.put("telefone", "+5519888888888");
        pendente.put("codigo", "654321");

        backend1.pendente = pendente;

        SmsFake sms = new SmsFake();
        sms.falhar = true;

        RecuperacaoSenhaWorker worker =
                new RecuperacaoSenhaWorker(
                        backend1,
                        sms);

        worker.run();

        assertNotNull(
                backend1.resultado);

        assertEquals(
                20,
                backend1.resultado
                        .path("recuperacaoId")
                        .asInt());

        assertFalse(
                backend1.resultado
                        .path("sucesso")
                        .asBoolean());

        assertTrue(
                backend1.resultado
                        .path("erro")
                        .asText()
                        .contains("Falha simulada"));
    }

    private static class Backend1Fake
            extends ClienteBackend1 {

        JsonNode pendente;
        JsonNode resultado;

        Backend1Fake() {
            super(
                    "localhost",
                    9090,
                    "chave-interna-teste-123");
        }

        @Override
        public JsonNode chamar(
                String operacao,
                JsonNode dados) {

            if ("buscar_recuperacao_senha"
                    .equals(operacao)) {

                return pendente;
            }

            if ("registrar_envio_recuperacao"
                    .equals(operacao)) {

                resultado = dados;

                return JSON
                        .createObjectNode()
                        .put("registrado", true);
            }

            throw new IllegalArgumentException(
                    "Operação inesperada: "
                            + operacao);
        }
    }

    private static class SmsFake
            extends TwilioSms {

        String destino;
        String mensagem;
        boolean falhar;

        SmsFake() {
            super(
                    "sid-teste",
                    "token-teste",
                    "+5511999999999");
        }

        @Override
        public void enviar(
                String destino,
                String mensagem)
                throws IOException {

            if (falhar) {
                throw new IOException(
                        "Falha simulada no SMS");
            }

            this.destino = destino;
            this.mensagem = mensagem;
        }
    }
}
