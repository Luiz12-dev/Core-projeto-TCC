# 💈 Core BarberShop API — Motor de Negócios e Agendamento

API RESTful que concentra toda a lógica de negócio de um sistema de gestão de barbearias. Desenvolvida com **Java 21** e **Spring Boot 4.0**, esta API gerencia o **catálogo de serviços**, **agendamentos inteligentes**, **horários de funcionamento**, **bloqueios de agenda** e **relatórios de receita mensal**.

> Funciona como o segundo microsserviço do sistema BarberShop — consome os tokens JWT emitidos pela [Auth API](https://github.com/Luiz12-dev/Autentica-o-projeto-TCC) para autenticação e autorização stateless, sem precisar de banco de usuários próprio.

---

## 🚀 Tecnologias

| Tecnologia | Versão | Descrição |
|-----------|--------|-----------|
| **Java** | 21 | Linguagem principal (LTS) |
| **Spring Boot** | 4.0.0 | Framework principal |
| **Spring Security** | 6.x | Autorização via JWT |
| **Spring Data JPA** | — | Persistência e ORM |
| **Flyway** | — | Versionamento de migrations SQL |
| **PostgreSQL** | 15 | Banco de dados relacional |
| **JJWT** | 0.11.5 | Validação de tokens JWT (HS512) |
| **Lombok** | — | Redução de boilerplate |
| **Bean Validation** | 3.x | Validação de dados de entrada |
| **SpringDoc OpenAPI** | 2.5.0 | Swagger UI interativo |
| **Docker Compose** | — | Infraestrutura containerizada |
| **JUnit 5** | — | Framework de testes |
| **Mockito + AssertJ** | — | Testes unitários |
| **H2 Database** | — | Banco em memória para testes |

---

## 📐 Arquitetura

O projeto segue a **Arquitetura em Camadas** com separação clara de responsabilidades:

```
src/main/java/br/com/core/barbershop/
├── config/                # OpenApiConfig (Swagger + JWT SecurityScheme)
├── controller/            # 4 Controllers REST
│   ├── AppointmentController      # Agendamentos (8 endpoints)
│   ├── BarberServiceController    # Catálogo de serviços (5 endpoints)
│   ├── BlockedPeriodController    # Bloqueios de agenda (3 endpoints)
│   └── BusinessHoursController    # Horários de funcionamento (4 endpoints)
├── domain/                # 5 Entidades JPA
│   ├── Client             # Cliente (1:N com Appointment)
│   ├── BarberService      # Serviço (nome, preço, duração)
│   ├── Appointment        # Agendamento (state machine)
│   ├── BusinessHours      # Horário de funcionamento por dia
│   └── BlockedPeriod      # Período bloqueado pelo owner
├── dto/                   # Java Records (request/response)
├── enuns/                 # AppointmentStatus (5 estados)
├── exception/             # Exceções + GlobalExceptionHandler
├── repository/            # Spring Data JPA Repositories
├── security/
│   ├── config/            # SecurityConfig + SecurityFilter (JWT)
│   └── jwt/               # TokenService (validação do token da Auth API)
├── service/               # Regras de negócio
│   ├── AppointmentService         # Motor de agendamento
│   ├── AppointmentValidator       # Validações de negócio isoladas
│   ├── BarberServiceService       # CRUD do catálogo
│   ├── BlockedPeriodService       # Gestão de bloqueios
│   └── BusinessHoursService       # Gestão de horários
└── BarbershopApplication.java

src/main/resources/
└── db/migration/          # 5 Flyway migrations (V1 a V5)
```

---

## 🔗 Integração com a Auth API

Esta API **não possui cadastro nem login** — ela consome tokens JWT emitidos pela Auth API (porta `8081`). O fluxo é:

```
1. Usuário faz login na Auth API → Recebe JWT com email + roles (ROLE_CLIENT ou ROLE_OWNER)
2. Usuário envia request para a Core API com header: Authorization: Bearer <token>
3. SecurityFilter intercepta → TokenService valida assinatura HS512 + extrai email e roles
4. SecurityContext é preenchido → @PreAuthorize verifica a role → Acesso concedido ou 403
5. Para endpoints de CLIENT: Principal.getName() retorna o email → identifica o cliente
```

As duas APIs compartilham a **mesma secret key JWT**, garantindo que tokens emitidos pela Auth API sejam aceitos pela Core API.

---

## 🔐 Controle de Acesso (RBAC)

Cada endpoint possui uma política de acesso definida:

| Endpoint | Método | Acesso | Descrição |
|----------|--------|--------|-----------|
| `/api/services` | `GET` | 🌐 Público | Listar serviços ativos |
| `/api/services/{id}` | `GET` | 🌐 Público | Buscar serviço por ID |
| `/api/services` | `POST` | 🔒 OWNER | Criar novo serviço |
| `/api/services/{id}` | `PUT` | 🔒 OWNER | Atualizar serviço |
| `/api/services/{id}` | `DELETE` | 🔒 OWNER | Desativar serviço (soft delete) |
| `/api/business-hours` | `GET` | 🌐 Público | Listar horários de funcionamento |
| `/api/business-hours` | `POST` | 🔒 OWNER | Adicionar horário |
| `/api/business-hours/{id}` | `PUT` | 🔒 OWNER | Atualizar horário |
| `/api/business-hours/{id}` | `DELETE` | 🔒 OWNER | Desativar horário (soft delete) |
| `/api/blocked-periods` | `GET` | 🌐 Público | Listar bloqueios futuros |
| `/api/blocked-periods` | `POST` | 🔒 OWNER | Bloquear período |
| `/api/blocked-periods/{id}` | `DELETE` | 🔒 OWNER | Remover bloqueio |
| `/api/appointments/available-slots` | `GET` | 🌐 Público | Consultar horários disponíveis |
| `/api/appointments` | `POST` | 🔑 CLIENT | Agendar horário |
| `/api/appointments/my-history` | `GET` | 🔑 CLIENT | Meu histórico |
| `/api/appointments` | `GET` | 🔒 OWNER | Listar todos agendamentos |
| `/api/appointments/today` | `GET` | 🔒 OWNER | Agenda do dia |
| `/api/appointments/revenue` | `GET` | 🔒 OWNER | Receita mensal |
| `/api/appointments/{id}/status` | `PATCH` | 🔒 OWNER | Atualizar status |
| `/api/appointments/{id}/cancel` | `PATCH` | 🔑🔒 Ambos | Cancelar agendamento |

---

## 📅 Motor de Agendamento

### Cálculo de Slots Disponíveis

O endpoint `GET /api/appointments/available-slots?date=2026-04-25&serviceId=<uuid>` calcula os horários disponíveis em tempo real cruzando três fontes:

```
Horários de funcionamento do dia (BusinessHours)
    ↓ Gera slots de 30 em 30 minutos
    ↓ Remove slots no passado
    ↓ Remove slots com conflito de agendamentos existentes (query nativa com make_interval)
    ↓ Remove slots em períodos bloqueados pelo owner
    = Slots disponíveis
```

### Máquina de Estados

O agendamento segue um ciclo de vida controlado com transições validadas:

```
    ┌──────────┐     ┌───────────┐     ┌──────────────┐     ┌───────────┐
    │ PENDING  │ ──► │ CONFIRMED │ ──► │ IN_PROGRESS  │ ──► │ COMPLETED │
    └────┬─────┘     └─────┬─────┘     └──────┬───────┘     └───────────┘
         │                 │                   │              (estado final)
         │                 │                   │
         ▼                 ▼                   ▼
    ┌───────────┐     ┌───────────┐     ┌───────────┐
    │ CANCELLED │     │ CANCELLED │     │ CANCELLED │
    └───────────┘     └───────────┘     └───────────┘
```

- ✅ Transições válidas: `PENDING→CONFIRMED`, `CONFIRMED→IN_PROGRESS`, `IN_PROGRESS→COMPLETED`
- ✅ Cancelamento: permitido a partir de qualquer status ativo
- ❌ Bloqueados: retrocessos (`CONFIRMED→PENDING`), pular etapas (`PENDING→COMPLETED`), reativar finalizados

### Validações de Negócio (`AppointmentValidator`)

Antes de criar um agendamento, o sistema valida:

1. ⏰ Antecedência mínima de **30 minutos**
2. 📋 Serviço deve estar **ativo**
3. 🕐 Horário deve caber **inteiramente** dentro do período de funcionamento
4. 🚫 Não pode conflitar com **períodos bloqueados** pelo owner
5. 📆 Não pode conflitar com **agendamentos existentes** (query nativa)

---

## 🗄️ Modelo de Dados

### Diagrama ER

```
┌──────────────────┐       ┌──────────────────┐
│     clients      │       │     services     │
├──────────────────┤       ├──────────────────┤
│ id (UUID, PK)    │       │ id (UUID, PK)    │
│ usernames        │       │ service_name (UQ)│
│ email (UQ)       │       │ description      │
│ phone_number     │       │ price            │
│ created_at       │       │ duration_min     │
│ updated_at       │       │ active           │
└────────┬─────────┘       │ created_at       │
         │                 │ updated_at       │
         │ 1:N             └────────┬─────────┘
         │                          │ 1:N
         ▼                          ▼
┌───────────────────────────────────────────┐
│              appointments                 │
├───────────────────────────────────────────┤
│ id (UUID, PK)                             │
│ client_id (FK → clients)                  │
│ service_id (FK → services)                │
│ date_time                                 │
│ status (PENDING/CONFIRMED/IN_PROGRESS/    │
│         COMPLETED/CANCELLED)              │
│ observation                               │
│ price                                     │
│ created_at / updated_at                   │
└───────────────────────────────────────────┘

┌──────────────────┐       ┌──────────────────┐
│  business_hours  │       │ blocked_periods  │
├──────────────────┤       ├──────────────────┤
│ id (UUID, PK)    │       │ id (UUID, PK)    │
│ day_of_week      │       │ start_date_time  │
│ open_time        │       │ end_date_time    │
│ close_time       │       │ reason           │
│ active           │       │ created_at       │
│ (UQ: day+open)   │       └──────────────────┘
└──────────────────┘
```

### Flyway Migrations

| Migration | Descrição |
|-----------|-----------|
| `V1` | Tabela `clients` + índice em `email` |
| `V2` | Tabela `services` (catálogo) |
| `V3` | Tabela `appointments` + FKs + índices (`client_id`, `date_time`, `status`) |
| `V4` | Tabela `business_hours` + constraint unique (`day_of_week` + `open_time`) |
| `V5` | Tabela `blocked_periods` + índice composto no range de datas |

---

## 🔄 Exemplos de Requisição

### Criar um serviço (OWNER)
```bash
curl -X POST http://localhost:8080/api/services \
  -H "Authorization: Bearer <token_owner>" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "Corte Masculino",
    "description": "Corte na máquina e tesoura",
    "price": 35.00,
    "durationMin": 30
  }'
```

**Response (201):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "serviceName": "Corte Masculino",
  "description": "Corte na máquina e tesoura",
  "price": 35.00,
  "durationMin": 30
}
```

### Agendar horário (CLIENT)
```bash
curl -X POST http://localhost:8080/api/appointments \
  -H "Authorization: Bearer <token_client>" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceId": "550e8400-e29b-41d4-a716-446655440000",
    "dateTime": "2026-04-25T10:00:00",
    "observation": "Corte degradê"
  }'
```

**Response (201):**
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "dateTime": "2026-04-25T10:00:00",
  "observation": "Corte degradê",
  "status": "PENDING",
  "clientId": "...",
  "clientName": "Luiz Otávio",
  "clientPhone": "44999999999",
  "serviceId": "550e8400-...",
  "serviceName": "Corte Masculino",
  "price": 35.00
}
```

### Consultar slots disponíveis (Público)
```bash
curl "http://localhost:8080/api/appointments/available-slots?date=2026-04-25&serviceId=550e8400-..."
```

**Response (200):**
```json
["08:00", "08:30", "09:00", "09:30", "10:00", "13:00", "13:30", "14:00"]
```

### Receita mensal (OWNER)
```bash
curl "http://localhost:8080/api/appointments/revenue?month=4&year=2026" \
  -H "Authorization: Bearer <token_owner>"
```

**Response (200):**
```json
{
  "month": 4,
  "year": 2026,
  "totalRevenue": 1050.00,
  "totalAppointments": 30
}
```

## ⚙️ Como Executar

### Pré-requisitos
- Java 21+
- Maven 3.9+
- Docker e Docker Compose
- Auth API rodando na porta `8081` ([repositório](https://github.com/Luiz12-dev/Autentica-o-projeto-TCC))

### 1. Subir o banco de dados
```bash
docker compose up -d
```

### 2. Subir a Auth API (em outro terminal)
```bash
# No repositório da Auth API:
docker compose up -d
./mvnw spring-boot:run
```

### 3. Executar a Core API
```bash
./mvnw spring-boot:run
```
> O Flyway executa as 5 migrations automaticamente na primeira inicialização.

### 4. Acessar o Swagger UI
```
http://localhost:8080/swagger-ui/index.html
```

Para testar endpoints protegidos:
1. Faça login na Auth API (`POST http://localhost:8081/api/auth/login`)
2. Copie o `accessToken` da resposta
3. No Swagger da Core API, clique em **Authorize** e cole o token

### 5. Executar os testes
```bash
./mvnw test
```

---

## 🔒 Fluxo de Segurança

```
Request com Bearer Token
    │
    ▼
SecurityFilter (OncePerRequestFilter)
    ├── Rota pública? → Prossegue sem validação
    ├── Token presente?
    │   ├── Não → 401 Unauthorized
    │   └── Sim → TokenService.validateToken()
    │       ├── Inválido/expirado → 401
    │       └── Válido → Extrai email + roles
    │           → SecurityContextHolder.setAuthentication()
    │           → @PreAuthorize verifica role
    │               ├── Role insuficiente → 403 Forbidden
    │               └── Role válida → Controller → Service → Repository
    └── Resposta JSON padronizada (GlobalExceptionHandler)
```

---

## 🛠️ Melhorias Futuras

- [ ] Endpoint de refresh token na Core API
- [ ] Notificações por e-mail/SMS para confirmação de agendamento
- [ ] Painel de métricas com Spring Actuator
- [ ] Rate limiting para proteção contra abuso
- [ ] CI/CD com GitHub Actions para rodar os 67 testes automaticamente
- [ ] Paginação com `Pageable` nos endpoints de listagem

---

## 👨‍💻 Autor

Desenvolvido por **Luiz Otávio** como parte do TCC (Trabalho de Conclusão de Curso).
