package br.com.reservice.backend2;

import java.util.logging.Logger;

/** Confere, uma vez, na partida, se o Backend 1 está de pé e aceita a chave interna. */
public final class Partida {

    private static final Logger LOG = Logger.getLogger(Partida.class.getName());

    @FunctionalInterface
    public interface Verificacao {
        void executar() throws Exception;
    }

    private Partida() {
    }

    /**
     * Tenta a verificação até {@code tentativas} vezes, dobrando a espera entre elas (limitada a
     * {@code esperaMaximaMs}). Chave recusada é erro de configuração: não adianta tentar de novo.
     *
     * @return {@code true} se o Backend 1 respondeu; {@code false} se desistiu
     */
    public static boolean aguardarBackend1(Verificacao verificacao, int tentativas,
                                           long esperaInicialMs, long esperaMaximaMs) {
        long espera = esperaInicialMs;
        for (int tentativa = 1; tentativa <= tentativas; tentativa++) {
            try {
                verificacao.executar();
                LOG.info("Backend 1 respondeu (tentativa " + tentativa + ")");
                return true;
            } catch (ErroBackend1 e) {
                LOG.severe("Backend 1 recusou a conexão: " + e.codigo() + ". Confira CHAVE_INTERNA nos dois lados.");
                return false;
            } catch (Exception e) {
                LOG.warning("Backend 1 ainda indisponível (tentativa " + tentativa + "/" + tentativas + "): " + e.getMessage());
            }
            if (tentativa < tentativas) {
                try {
                    Thread.sleep(espera);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
                espera = Math.min(espera * 2, esperaMaximaMs);
            }
        }
        return false;
    }
}
