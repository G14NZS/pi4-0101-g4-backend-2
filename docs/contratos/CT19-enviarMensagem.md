# CT19: enviarMensagem

| Linha | Valor |
|---|---|
| Fronteira | Externo |
| Quem chama | Backend 2 |
| Quem responde | Serviço de mensagens (Twilio, SMS) |
| Atende | E6 |
| Como chamar | `POST https://api.twilio.com/2010-04-01/Accounts/{AccountSid}/Messages.json`, autenticação Basic (AccountSid e AuthToken), corpo form-urlencoded. Documentação oficial: https://www.twilio.com/docs/messaging/api/message-resource. Os nomes dos campos são os do serviço. |

## Entrada

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| To | texto | sim | +5519988887777 |
| From | texto | sim | +15017122661 |
| Body | texto | sim | Olá, Maria! Lembrete: 20/10 às 14:00, Manicure simples com Ana Beatriz Costa. |

## Saída

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| sid | texto | sim | SM1f2e3d4c5b6a7980 |

## Erros

| Quando | O que volta |
|---|---|
| Telefone inválido (código Twilio 21211) | HTTP 400 do serviço. O Backend 2 chama o CT13 com resultado `falhou` e não tenta de novo. |
| Conta de teste e número não verificado (código Twilio 21608) | HTTP 400 do serviço. Mesmo tratamento: CT13 com resultado `falhou`. |
| Credenciais erradas (código Twilio 20003) | HTTP 401 do serviço. O Backend 2 não chama o CT13; o lembrete continua pendente e é tentado no próximo ciclo. |
| Serviço fora do ar ou sem resposta em 10 segundos | Nada. O Backend 2 não chama o CT13; o lembrete continua pendente e é tentado no próximo ciclo. |
