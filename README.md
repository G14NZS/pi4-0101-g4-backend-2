# pi4-0101-g4-backend-2 — Reservice Worker SMS (v1.0)

Servidor em **Java puro (sem Maven)** responsável pelo envio automático de lembretes por SMS via **Twilio** (épico E6, história H08.2, contratos `CT12`, `CT13` e `CT19`).

- **Repositório:** https://github.com/G14NZS/pi4-0101-g4-backend-2.git

---

## Arquitetura e Funcionamento

O servidor segue a arquitetura cliente-servidor do material "Fazedor de Continhas" e acrescenta uma thread agendadora de lembretes.

1. **Servidor de conexões (porta 3000):**
   - `AceitadoraDeConexao` aceita clientes e cria uma `SupervisoraDeConexao` (uma thread) para cada um.
   - As mensagens trafegam como objetos `Comunicado` serializados (`Parceiro`, `PedidoParaSair`, `ComunicadoDeDesligamento`).
   - O comando `desativar` no terminal avisa todos os clientes conectados e encerra o servidor.
2. **Agendador de lembretes (`Agendador`):** a cada ciclo (padrão de 15 minutos):
   1. Reenvia ao Backend 1 os resultados que ficaram pendentes em ciclos anteriores.
   2. **CT12 — `listarLembretesPendentes`:** pede ao Backend 1 os agendamentos das próximas 3 horas (horário de Brasília).
   3. **CT19 — envio do SMS:** para cada lembrete, chama a API REST do Twilio (`TwilioSms`).
   4. **CT13 — `registrarLembrete`:** registra no Backend 1 o resultado (`enviado` ou `falhou`).
3. **Comunicação com o Backend 1 (ADR 0002):** `ConexaoComBackend1` abre um socket TCP por chamada no `ServidorSocket` do `pi4-0101-g4-backend-1` e troca **uma linha de JSON** por mensagem:
   - pedido: `{"operacao": ..., "dados": {...}, "chaveInterna": ...}`
   - resposta: `{"ok": true, "dados": {...}}` ou `{"ok": false, "erro": CODIGO, "mensagem": texto}`

### Tratamento de falhas

| Situação | O que acontece |
|---|---|
| Telefone inválido ou não verificado (Twilio 21211 / 21608) | Registra `falhou`; o lembrete não volta |
| Twilio fora do ar, credencial errada ou sem resposta em 10 s | Nada é registrado; o lembrete volta no ciclo seguinte |
| Backend 1 cai depois de o SMS já ter saído | O resultado fica guardado e é registrado no ciclo seguinte, sem novo SMS |
| Backend 1 responde `LEMBRETE_JA_REGISTRADO` | Ignorado |
| `CHAVE_INTERNA` ausente ou com menos de 16 caracteres | O agendador não liga; o servidor de conexões continua funcionando |

---

## Estrutura

```
Servidor/
  Servidor.java                 Ponto de entrada; lê as variáveis de ambiente e inicia o agendador
  AceitadoraDeConexao.java      Thread que aceita conexões na porta 3000
  SupervisoraDeConexao.java     Thread que atende um cliente
  Parceiro.java, Comunicado.java, ComunicadoDeDesligamento.java, PedidoParaSair.java, Teclado.java
  Agendador.java                Ciclo CT12 -> CT19 -> CT13
  ConexaoComBackend1.java       Cliente do socket JSON do Backend 1
  Json.java                     Leitura e escrita mínima de JSON
  Lembrete.java                 Dados de um lembrete e texto do SMS
  ServicoDeMensagens.java       Interface de envio de SMS
  TwilioSms.java                Implementação com a API REST do Twilio
  ErroBackend1.java, FalhaDefinitivaDeEnvio.java, FalhaTemporariaDeEnvio.java
Cliente/                        Cliente de terminal (conecta na porta 3000 e permite sair)
docs/adr/                       ADR 0001 (MySQL) e ADR 0002 (Java com sockets)
docs/contratos/CT19-enviarMensagem.md
Dockerfile                      Compila com JDK 21 e roda com JRE 21
.github/workflows/ci.yml        Compila Servidor e Cliente e constrói a imagem Docker
```

---

## Variáveis de Ambiente

| Variável | Obrigatória | Descrição |
|---|---|---|
| `BACKEND1_HOST` | Sim | Host do Backend 1 (padrão `localhost`; no Compose, `backend1`) |
| `BACKEND1_PORTA` | Não | Porta do socket do Backend 1 (padrão `9090`) |
| `CHAVE_INTERNA` | Sim | Segredo compartilhado com o Backend 1, com 16 caracteres ou mais |
| `TWILIO_ACCOUNT_SID` | Sim | Account SID da conta Twilio |
| `TWILIO_AUTH_TOKEN` | Sim | Auth Token da conta Twilio |
| `TWILIO_FROM` | Sim | Número remetente do Twilio, no formato E.164 (ex.: `+15017122661`) |
| `TWILIO_MODELO_TRIAL` | Não | Em conta trial, nome do modelo enviado no lugar do texto livre (ex.: `sms_appointment_reminders`) |
| `INTERVALO_LEMBRETES_SEGUNDOS` | Não | Intervalo entre ciclos, em segundos (padrão 900; mínimo 10). Útil para demonstração |

Sem as credenciais do Twilio, o agendador continua rodando, mas não envia SMS: os lembretes ficam pendentes e são tentados de novo no ciclo seguinte.

---

## Execução

### Via Docker Compose (recomendado)

O `docker-compose.yml` fica no repositório `pi4-0101-g4-backend-1` e sobe o MySQL, o Backend 1, este Backend 2 e o frontend. Os três repositórios precisam estar lado a lado na mesma pasta. Consulte o [README do `pi4-0101-g4-backend-1`](https://github.com/G14NZS/pi4-0101-g4-backend-1).

```sh
docker compose logs -f backend2   # acompanha os ciclos do agendador
```

### Execução local (JDK 21)

```sh
# Servidor
javac -encoding UTF-8 -d out Servidor/*.java
java -cp out Servidor [PORTA]          # porta padrão 3000

# Cliente (em outro terminal)
javac -encoding UTF-8 -d out-cliente Cliente/*.java
java -cp out-cliente Cliente [HOST [PORTA]]
```

### Imagem Docker isolada

```sh
docker build -t backend2 .
docker run --rm -e BACKEND1_HOST=... -e CHAVE_INTERNA=... -e TWILIO_ACCOUNT_SID=... \
  -e TWILIO_AUTH_TOKEN=... -e TWILIO_FROM=... backend2
```

Sem teclado (por exemplo, no Docker sem `-it`), o servidor segue ativo até o processo ser encerrado.
