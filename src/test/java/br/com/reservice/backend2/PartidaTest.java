package br.com.reservice.backend2;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verificação de partida do Backend 2: retry com espera crescente e falha rápida com chave recusada. */
class PartidaTest {

    @Test
    void respondeNaPrimeiraTentativa() {
        AtomicInteger chamadas = new AtomicInteger();

        boolean ok = Partida.aguardarBackend1(chamadas::incrementAndGet, 5, 1, 2);

        assertTrue(ok);
        assertEquals(1, chamadas.get());
    }

    @Test
    void recuperaDepoisDeFalhasTemporarias() {
        AtomicInteger chamadas = new AtomicInteger();

        boolean ok = Partida.aguardarBackend1(() -> {
            if (chamadas.incrementAndGet() < 3) {
                throw new IOException("Connection refused");
            }
        }, 5, 1, 2);

        assertTrue(ok);
        assertEquals(3, chamadas.get());
    }

    @Test
    void desisteDepoisDeTodasAsTentativas() {
        AtomicInteger chamadas = new AtomicInteger();

        boolean ok = Partida.aguardarBackend1(() -> {
            chamadas.incrementAndGet();
            throw new IOException("Connection refused");
        }, 4, 1, 2);

        assertFalse(ok);
        assertEquals(4, chamadas.get());
    }

    @Test
    void chaveRecusadaNaoTentaDeNovo() {
        AtomicInteger chamadas = new AtomicInteger();

        boolean ok = Partida.aguardarBackend1(() -> {
            chamadas.incrementAndGet();
            throw new ErroBackend1("ACESSO_NEGADO", "Chave interna ausente ou errada.");
        }, 5, 1, 2);

        assertFalse(ok);
        assertEquals(1, chamadas.get());
    }
}
