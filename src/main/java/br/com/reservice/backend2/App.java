package br.com.reservice.backend2;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

/** Ponto de entrada do Backend 2 (agendador de lembretes). */
public final class App {

    private static final Logger LOG = Logger.getLogger(App.class.getName());

    private App() {
    }

    public static void main(String[] args) throws InterruptedException {
        Config config = Config.doAmbiente();
        ClienteBackend1 backend1 = new ClienteBackend1(config.backend1Host(), config.backend1Porta(), config.chaveInterna());

        // Fail-fast: sem Backend 1 (ou com a chave errada) este módulo não serve para nada. Cada chamada do
        // Backend 2 abre e fecha a sua conexão TCP, então não há conexão ociosa para manter viva.
        if (!Partida.aguardarBackend1(() -> backend1.chamar("ping", null), 8, 1_000, 15_000)) {
            LOG.severe("Não foi possível falar com o Backend 1; encerrando.");
            System.exit(1);
        }

        // O ciclo de 15 minutos (CT12, CT19, CT13) entra na história do E6; até lá, só mantém o processo vivo.
        CountDownLatch encerrar = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(encerrar::countDown));
        LOG.info("Backend 2 pronto");
        encerrar.await();
    }
}
