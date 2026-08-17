# datum-srv-status-publisher

Serviço publicador de solicitações de alteração de status de cliente, da stack **Datum**.

## Função do serviço

O `datum-srv-status-publisher` expõe um único endpoint (`POST /customers/{id}/status`) que recebe uma solicitação de mudança de status (`ACTIVE`/`INACTIVE`) e a **publica de forma assíncrona no RabbitMQ** — ele não altera diretamente nenhum dado de cliente; quem aplica a mudança é o `datum-srv-clientes`, do outro lado da fila.

- **Recebe e valida** a requisição (`status` obrigatório, normalizado para `ACTIVE`/`INACTIVE`, case-insensitive).
- **Publica o evento** `CUSTOMER_STATUS_CHANGE` diretamente na fila `customer_status_changed` (exchange padrão do RabbitMQ, roteamento pelo nome da fila).
- **Responde `202 Accepted`** com o evento publicado assim que a mensagem é aceita pelo broker — publicar *é* a própria operação: se falhar, a exceção é propagada e mapeada para `502 Bad Gateway` (diferente do publisher do `datum-srv-clientes`, que é best-effort, pois lá a criação do cliente já foi persistida antes).
- **Protege a operação por autorização**: como *OAuth2 Resource Server*, exige um Access Token JWT válido (emitido pelo `datum-srv-auth`) com papel `ADMIN`.

## Tecnologias

| Categoria | Tecnologia |
|---|---|
| Linguagem / runtime | Java 21 |
| Framework | Spring Boot 3.5.16 |
| Web | Spring Web (REST), Bean Validation (`spring-boot-starter-validation`) |
| Segurança | Spring Security, OAuth2 Resource Server (validação de JWT via JWKS) |
| Mensageria | Spring AMQP (RabbitMQ) — apenas publisher |
| Build | Maven (via `mvnw`) |
| Empacotamento / execução | Docker (build multi-stage `eclipse-temurin:21-jdk`) |
| Testes | Spring Boot Test, Spring Security Test |

Não usa banco de dados nem Spring Data/JPA — é stateless, sua única responsabilidade é validar a requisição e publicar a mensagem.

## Dependências (serviços necessários para funcionar)

| Dependência | Uso | Obrigatório |
|---|---|---|
| **RabbitMQ** | Publica o evento `CUSTOMER_STATUS_CHANGE` na fila `customer_status_changed` (declarada pela própria aplicação na subida, de forma idempotente — funciona independente de qual serviço sobe primeiro em relação ao `datum-srv-clientes`, que também a declara). | Sim — publicar é a própria operação do serviço; falha aqui retorna erro ao chamador. |
| **datum-srv-auth** | Valida o Access Token JWT (assinatura + claim `roles`) via JWKS, resolvido automaticamente a partir do `issuer-uri`. | Sim, para qualquer chamada |

Não depende diretamente do `datum-srv-clientes` nem do MariaDB — a relação com o `datum-srv-clientes` é apenas através da fila (contrato de mensagem compartilhado).

Variáveis de ambiente relevantes (ver `docker-compose.yml` na raiz do projeto): `RMQ_HOST`/`RMQ_PORT`/`RMQ_USERNAME`/`RMQ_PASSWORD`, `AUTH_HOST`/`AUTH_PORT` (issuer do JWT).

## Arquitetura

### Endpoint

| Método | Caminho | Papel exigido | Corpo | Resposta |
|---|---|---|---|---|
| `POST` | `/customers/{id}/status` | `ADMIN` | `{ "status": "ACTIVE" \| "INACTIVE" }` | `202 Accepted` com o evento publicado |

### Fluxo de publicação

```mermaid
sequenceDiagram
    actor A as Sistema/usuário ADMIN
    participant SEC as SecurityConfig<br/>(Resource Server)
    participant CTRL as StatusChangeController
    participant VAL as StatusValidator
    participant SRV as StatusChangePublisherService
    participant MQ as RabbitMQ<br/>(fila customer_status_changed)
    participant CLI as datum-srv-clientes<br/>(consumidor)

    A->>SEC: POST /customers/{id}/status (Bearer JWT)
    SEC->>SEC: valida assinatura/issuer (JWKS)<br/>exige role ADMIN
    SEC->>CTRL: requisição autorizada
    CTRL->>VAL: normalize(status)
    alt status inválido
        VAL-->>CTRL: throw IllegalArgumentException
        CTRL-->>A: 400 Bad Request
    else status válido
        VAL-->>CTRL: ACTIVE | INACTIVE
        CTRL->>SRV: publish(customerId, status)
        SRV->>MQ: convertAndSend(evento CUSTOMER_STATUS_CHANGE)
        alt falha ao publicar
            MQ-->>SRV: AmqpException
            SRV-->>A: 502 Bad Gateway
        else publicado com sucesso
            SRV-->>CTRL: evento publicado
            CTRL-->>A: 202 Accepted
            MQ--)CLI: entrega assíncrona (fora deste serviço)
        end
    end
```

### Componentes internos

```mermaid
flowchart LR
    subgraph "datum-srv-status-publisher"
        SEC["SecurityConfig<br/>(Resource Server, role ADMIN)"]
        CTRL["StatusChangeController<br/>(/customers/id/status)"]
        VAL["StatusValidator"]
        SRV["StatusChangePublisherService"]
        EXC["ApiExceptionHandler<br/>(validação -> 400, AMQP -> 502)"]
    end

    AUTHX["datum-srv-auth (JWKS)"]
    MQX{{"RabbitMQ"}}

    SEC -. valida token .-> AUTHX
    CTRL --> VAL
    CTRL --> SRV --> MQX
    CTRL -.-> EXC
```

- **Contrato de mensagem compartilhado**: o payload publicado (`eventId`, `eventType=CUSTOMER_STATUS_CHANGE`, `customerId`, `status`) segue exatamente o formato esperado pelo `CustomerStatusChangeListener` do `datum-srv-clientes` — os dois serviços não se conhecem em código, apenas concordam nesse contrato via fila.
- **Sem exchange dedicado**: a publicação usa o exchange padrão do RabbitMQ (`""`), com routing key igual ao nome da fila — roteamento direto, sem binding customizado.

## Como subir

Este serviço faz parte da stack orquestrada pelo `docker-compose.yml` na raiz do repositório [`projeto-datum`](https://github.com/alexmart001/projeto-datum). Para subir apenas ele (com suas dependências):

```bash
docker compose up rabbitmq datum-srv-auth datum-srv-status-publisher
```
