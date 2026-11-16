package br.com.reservice.backend2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.logging.Level;
import java.util.logging.Logger;

public class RecuperacaoSenhaWorker implements Runnable {

    private static final Logger LOG =
            Logger.getLogger(
                    RecuperacaoSenhaWorker.class.getName());

    private static final ObjectMapper JSON =
            new ObjectMapper();

    private final ClienteBackend1 backend1;
    private final TwilioSms sms;

    public RecuperacaoSenhaWorker(
            ClienteBackend1 backend1,
            TwilioSms sms) {

        this.backend1 = backend1;
        this.sms = sms;
    }

    @Override
    public void run() {

        try {
            JsonNode pendente =
                    backend1.chamar(
                            "buscar_recuperacao_senha",
                            null);

            if (!pendente
                    .path("disponivel")
                    .asBoolean(false)) {

                return;
            }

            int recuperacaoId =
                    pendente.path(
                            "recuperacaoId").asInt();

            String telefone =
                    pendente.path(
                            "telefone").asText();

            String codigo =
                    pendente.path(
                            "codigo").asText();

            boolean sucesso = false;
            String erro = null;

            try {
                sms.enviar(
                        telefone,
                        "Seu código de recuperação do Reservice é: "
                                + codigo
                                + ". Ele é válido por 10 minutos.");

                sucesso = true;

            } catch (Exception e) {

                erro = e.getMessage();

                LOG.log(
                        Level.WARNING,
                        "Falha ao enviar SMS de recuperação",
                        e);
            }

            ObjectNode resultado =
                    JSON.createObjectNode();

            resultado.put(
                    "recuperacaoId",
                    recuperacaoId);

            resultado.put(
                    "sucesso",
                    sucesso);

            if (erro != null) {
                resultado.put(
                        "erro",
                        erro);
            }

            backend1.chamar(
                    "registrar_envio_recuperacao",
                    resultado);

        } catch (Exception e) {

            LOG.log(
                    Level.WARNING,
                    "Falha no ciclo de recuperação de senha",
                    e);
        }
    }
}