# pi4-0101-g4-backend-2 — Reservice Worker SMS (v1.0)

Serviço worker em **Java (Maven)** responsável pelo agendamento periódico e envio automático de lembretes por SMS via **Twilio** (História H08.1 / Contrato `CT19-enviarMensagem`).

- **Repositório:** https://github.com/G14NZS/pi4-0101-g4-backend-2.git

---

## Arquitetura e Funcionamento

1. **Comunicação Interna via TCP Socket (ADR 0002):**
   - Conecta-se ao `ServidorSocket` do `pi4-0101-g4-backend-1` (`ClienteBackend1.java` / `ConexaoComBackend1.java`) autenticando cada requisição com `CHAVE_INTERNA`.
   - Consulta os lembretes elegíveis pendentes (`CT12 — listarLembretesPendentes`).
2. **Disparo de Mensagens SMS (`CT19`):**
   - Para cada lembrete pendente, invoca a API REST da Twilio através de `TwilioSms.java` (`ServicoDeMensagens.java`).
3. **Confirmação de Envio (`CT13`):**
   - Após confirmação da Twilio, envia o registro de conclusão via socket ao Back-end 1 (`CT13 — registrarLembrete`), garantindo idempotência e evitando envios duplicados.

---

## Estrutura Principal

- `src/main/java/br/com/reservice/backend2/App.java` — Ponto de entrada da aplicação.
- `src/main/java/br/com/reservice/backend2/Config.java` — Leitura das variáveis de ambiente (host/porta do socket, chave interna e credenciais Twilio).
- `src/main/java/br/com/reservice/backend2/ClienteBackend1.java` — Cliente de comunicação com o Back-end 1.
- `Servidor/Agendador.java` — Orquestrador cíclico de verificação e disparo de lembretes.
- `Servidor/TwilioSms.java` — Implementação do envio de SMS via Twilio.
- `src/test/java/br/com/reservice/backend2/` — Suíte de testes JUnit 5 (`ClienteBackend1Test.java`, `PartidaTest.java`).

---

## Variáveis de Ambiente

| Variável | Obrigatória | Descrição |
|---|---|---|
| `BACKEND1_HOST` | Sim | Hostname do container/servidor do Back-end 1 (padrão `backend1` no Compose) |
| `PORTA_SOCKET` | Não | Porta TCP do socket no Back-end 1 (padrão `9090`) |
| `CHAVE_INTERNA` | Sim | Segredo compartilhado idêntico ao configurado no Back-end 1 |
| `TWILIO_ACCOUNT_SID` | Sim | Account SID da conta Twilio |
| `TWILIO_AUTH_TOKEN` | Sim | Auth Token da conta Twilio |
| `TWILIO_PHONE_NUMBER` | Sim | Número remetente Twilio habilitado para SMS (formato E.164) |

---

## Execução

### Via Docker Compose (Recomendado)
Consulte o [`README.md` do `pi4-0101-g4-backend-1`](https://github.com/G14NZS/pi4-0101-g4-backend-1) para subir toda a stack integrada.

### Build e Testes Locais com Maven
```sh
mvn clean test
mvn package
```