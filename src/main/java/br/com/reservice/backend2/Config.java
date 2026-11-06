package br.com.reservice.backend2;

import java.util.Map;

/** Configuração lida das variáveis de ambiente (ver o docker-compose.yml do Backend 1). */
public record Config(
        String backend1Host,
        int backend1Porta,
        String chaveInterna,
        String twilioAccountSid,
        String twilioAuthToken,
        String twilioFrom) {

    public static Config doAmbiente() {
        return de(System.getenv());
    }

    static Config de(Map<String, String> env) {
        String chave = env.get("CHAVE_INTERNA");
        if (chave == null || chave.length() < 16) {
            throw new IllegalStateException("CHAVE_INTERNA obrigatória, com pelo menos 16 caracteres");
        }
        return new Config(
                env.getOrDefault("BACKEND1_HOST", "localhost"),
                Integer.parseInt(env.getOrDefault("BACKEND1_PORTA", "9090")),
                chave,
                env.getOrDefault("TWILIO_ACCOUNT_SID", ""),
                env.getOrDefault("TWILIO_AUTH_TOKEN", ""),
                env.getOrDefault("TWILIO_FROM", ""));
    }
}
