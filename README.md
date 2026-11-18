# Plataforma Serverless de Feedback de Aulas — Tech Challenge (Fase 4)

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.1-brightgreen?style=for-the-badge&logo=springboot)
![AWS Serverless](https://img.shields.io/badge/AWS-Serverless_SAM-FF9900?style=for-the-badge&logo=amazonaws)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?style=for-the-badge&logo=postgresql)
![Clean Architecture](https://img.shields.io/badge/Architecture-Clean_Architecture-blue?style=for-the-badge)

Aplicação Serverless desenvolvida para a Fase 4 da Pós-Tech FIAP (Arquitetura e Desenvolvimento em Java), utilizando **Java 21**, **Spring Boot 4.1.1**, **Spring Cloud Function**, **PostgreSQL** e **AWS SAM**, projetada sob os pilares da **Clean Architecture (Ports and Adapters)** e do **Princípio da Responsabilidade Única (SRP)**.

---

## 📌 Artefatos de Entrega

- **Repositório Oficial (GitHub):** [https://github.com/abraaoylgner/api-feedback](https://github.com/abraaoylgner/api-feedback)
- **Vídeo de Demonstração da Solução:** `[INSERIR_LINK_DO_VIDEO_AQUI]`
- **Coleção de Testes (Insomnia):** Arquivo [`insomnia-collection-api-feedback.json`](./insomnia-collection-api-feedback.json) disponível na raiz do repositório com todos os cenários de teste pré-configurados para importação.

---

## 1. Problema e Objetivo

Para garantir a qualidade dos cursos on-line, é essencial que os estudantes possam fornecer feedbacks sobre as aulas e que os administradores acompanhem rapidamente a satisfação dos alunos.

Esta plataforma serverless automatiza:
1. **Recebimento de Feedbacks:** Ingestão e validação de avaliações via endpoint REST (`POST /avaliacao`) com notas de `0` a `10`.
2. **Alertas Críticos em Tempo Real:** Identificação imediata de avaliações críticas (`ALTA` ou `MEDIA` urgência), publicação em fila de mensageria e disparo automático de e-mail de alerta aos administradores.
3. **Relatórios Periódicos Automatizados:** Geração semanal agendada contendo a média geral das notas, estatísticas por dia e por nível de urgência, e o detalhamento dos feedbacks recebidos.

---

## 2. Modelo de Cloud Escolhido e Arquitetura da Solução

### Justificativa do Modelo de Cloud (Serverless: FaaS + BaaS)
A solução adota o modelo **Serverless (Function as a Service + Backend as a Service)** na **Amazon Web Services (AWS)** pelos seguintes benefícios arquiteturais e financeiros:
- **Eficiência de Custos (Pay-per-use):** Elimina o custo de servidores EC2 ociosos 24/7. A cobrança ocorre estritamente pelos milissegundos de execução quando um aluno envia uma avaliação ou quando o relatório semanal é gerado.
- **Escalabilidade Automática:** O AWS Lambda escala horizontalmente de forma transparente durante picos de acesso (ex.: término simultâneo de aulas ao vivo).
- **Baixa Latência em Java 21:** Uso do **AWS Lambda SnapStart** (`ApplyOn: PublishedVersions`), que cria snapshots pré-inicializados da JVM e reduz drasticamente o *cold start*.

### Componentes da Solução na AWS
| Componente AWS | Papel na Arquitetura |
| :--- | :--- |
| **Amazon API Gateway** | Porta de entrada HTTP que expõe a rota `POST /avaliacao` e roteia requisições para a Lambda de ingestão. |
| **AWS Lambda (Java 21)** | Executa de forma isolada as funções serverless (`ProcessarAvaliacaoFunction` e `GerarRelatorioSemanalFunction`). |
| **Amazon RDS (PostgreSQL)** | Banco de dados relacional gerenciado para persistência transacional segura das avaliações. |
| **Amazon SQS** | Fila de mensagens (`fila-notas-criticas`) que desacopla e armazena eventos de avaliações críticas para resiliência. |
| **Amazon SES** | Serviço de disparo de e-mails para envio de notificações imediatas de urgência e do relatório semanal consolidado. |
| **Amazon EventBridge** | Agendador serverless (`cron(0 8 ? * MON *)`) que aciona automaticamente a geração do relatório toda segunda-feira às 08h00. |
| **AWS Systems Manager (SSM)** | Parameter Store utilizado para injeção segura de credenciais e URL do banco (`DB_URL`, `DB_USER`, `DB_PASS`). |
| **Amazon CloudWatch** | Coleta centralizada de logs estruturados e monitoramento ativo com alarme de falhas (`AWS::CloudWatch::Alarm`). |

### Diagrama de Fluxo Arquitetural
```text
[Estudante] 
   │ (POST /avaliacao)
   ▼
[Amazon API Gateway] ──► [Lambda 1: ProcessarAvaliacaoFunction]
                                ├──► 1. Valida nota (0-10) e classifica urgência
                                ├──► 2. Persiste no [Amazon RDS PostgreSQL]
                                └──► 3. Se crítica (nota <= 5):
                                        ├──► Publica evento na fila [Amazon SQS]
                                        └──► Dispara e-mail de urgência via [Amazon SES]

[Amazon EventBridge] ──► [Lambda 2: GerarRelatorioSemanalFunction]
 (CRON: Segundas 8h)            ├──► 1. Consulta avaliações no [Amazon RDS PostgreSQL]
                                ├──► 2. Calcula média, qtd/dia e qtd/urgência
                                └──► 3. Envia relatório consolidado via [Amazon SES]
```

---

## 3. Funções Serverless e Responsabilidade Única (SRP)

Em conformidade com as regras do desafio, o sistema divide as responsabilidades em **duas funções serverless independentes**, garantindo isolamento de falhas e escalabilidade individualizada:

### 1. Função de Ingestão e Triagem (`processarAvaliacao`)
- **Recurso SAM:** `ProcessarAvaliacaoFunction` (`java.util.function.Function<AvaliacaoRequestDTO, AvaliacaoResponseDTO>`)
- **Gatilho:** Evento HTTP `POST /avaliacao` via Amazon API Gateway.
- **Responsabilidade Única:** Receber o payload da avaliação, validar regras de domínio (nota entre 0 e 10 e descrição obrigatória), calcular a urgência, persistir no banco PostgreSQL e, exclusivamente quando a avaliação for crítica (`nota <= 5`), acionar a notificação imediata para os administradores.

### 2. Função de Relatório Analítico (`gerarRelatorioSemanal`)
- **Recurso SAM:** `GerarRelatorioSemanalFunction` (`java.util.function.Consumer<Object>`)
- **Gatilho:** Evento agendado (`Schedule`) via Amazon EventBridge (`cron(0 8 ? * MON *)`).
- **Responsabilidade Única:** Extrair os feedbacks armazenados, processar a média aritmética das notas, consolidar os agrupamentos de volume por dia e por urgência, e disparar o relatório gerencial por e-mail aos administradores.

---

## 4. Qualidade de Código e Clean Architecture

O projeto foi estruturado seguindo a **Clean Architecture (Ports and Adapters)** para manter o núcleo de negócio 100% desacoplado de frameworks externos, banco de dados e SDKs da nuvem:

```text
com.pos.tech.challenge.feedback
├── domain
│   ├── entities       # Entidade pura (Avaliacao) com validações e cálculo de criticidade
│   ├── enums          # Enum de classificação (UrgenciaEnum: ALTA, MEDIA, BAIXA, NENHUMA)
│   └── exceptions     # Exceções de negócio (DominioException)
├── application
│   ├── ports          # Interfaces/Portas (AvaliacaoRepository, NotificacaoGateway)
│   └── usecases       # Orquestração de negócio (ProcessarAvaliacaoUseCase, GerarRelatorioSemanalUseCase)
├── adapters
│   ├── dtos           # Contratos imutáveis (AvaliacaoRequestDTO, AvaliacaoResponseDTO, RelatorioSemanalDTO, ErroResponseDTO)
│   └── repositories   # Adaptadores concretos (AvaliacaoRepositoryImpl, NotificacaoGatewayImpl)
└── infrastructure
    ├── config         # Beans Serverless e Tratamento Global de Erros (AvaliacaoFunctions, UseCaseConfig, GlobalExceptionHandler)
    └── persistence    # Entidade JPA e Repositório Spring Data (AvaliacaoEntity, SpringDataAvaliacaoRepository)
```

---

## 5. Segurança e Governança de Acesso

A governança de acesso e a proteção de dados de clientes foram implementadas diretamente na Infraestrutura como Código (`template.yaml`):

1. **Princípio do Menor Privilégio (IAM Roles segregadas por função):**
   - `ProcessarAvaliacaoFunction`: Possui permissão estrita para enviar mensagens apenas para a fila `FilaNotasCriticas` (`SQSSendMessagePolicy`) e disparar e-mails pelo SES (`SESSendEmailPolicy`).
   - `GerarRelatorioSemanalFunction`: Possui permissão restrita ao envio de e-mails (`SESSendEmailPolicy`), sem permissão de publicação na fila SQS.
2. **Gestão Segura de Segredos (Zero Hardcoded Credentials):**
   - Nenhuma senha de banco de dados fica exposta no repositório. As credenciais são injetadas em tempo de execução via **AWS Systems Manager Parameter Store (`{{resolve:ssm:/feedback/...}}`)**.
3. **Tratamento Seguro de Exceções (`GlobalExceptionHandler`):**
   - Erros internos ou *stack traces* nunca são expostos ao cliente externo. Falhas de validação retornam `400 Bad Request` padronizado e erros inesperados são registrados internamente com retorno genérico `500`.

---

## 6. Configuração do Monitoramento e Observabilidade

1. **Logs Estruturados no Amazon CloudWatch Logs:**
   - A aplicação emite logs padronizados com marcadores semânticos (`[ALERTA CRITICO]`, `[RELATORIO SEMANAL]`, `[ERRO DE VALIDACAO]`, `[ERRO INTERNO]`), facilitando filtros e buscas no CloudWatch Logs Insights.
2. **Alarme Automatizado de Falhas (`AWS::CloudWatch::Alarm`):**
   - O recurso `AlarmeErroProcessarAvaliacao` monitora continuamente a métrica nativa `Errors` do namespace `AWS/Lambda` em períodos de 5 minutos (`Period: 300`), disparando estado de alarme imediatamente caso ocorra qualquer falha não tratada (`Threshold >= 1`).

---

## 7. Contratos da API, Regras de Negócio e Modelos de Notificação

### Endpoint de Entrada: Registrar Avaliação
- **Rota em Produção (API Gateway):** `POST /avaliacao`
- **Rota em Ambiente Local (Spring Cloud Function Web):** `POST http://localhost:8080/processarAvaliacao`
- **Headers:** `Content-Type: application/json`

#### Exemplo de Request (`JSON`):
```json
{
  "descricao": "Aula excelente, mas o áudio da transmissão estava um pouco baixo.",
  "nota": 4
}
```

#### Exemplo de Response — Sucesso (`200 OK`):
```json
{
  "id": "c4a760a8-dbcf-4254-a2d9-6a44e852762a",
  "mensagem": "Avaliação registrada com sucesso."
}
```

#### Exemplo de Response — Erro de Validação (`400 Bad Request` para nota fora de 0 a 10):
```json
{
  "timestamp": "2026-11-16T21:15:30.125",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Nota deve ser entre 0 e 10",
  "path": "/processarAvaliacao"
}
```

### Regra de Classificação de Urgência
| Faixa de Nota | Nível de Urgência | Status de Criticidade | Ação Automática |
| :---: | :---: | :---: | :--- |
| **0 a 3** | `ALTA` | Crítica | Salva no RDS + Publica no SQS + Envia E-mail de Urgência (SES) |
| **4 a 5** | `MEDIA` | Crítica | Salva no RDS + Publica no SQS + Envia E-mail de Urgência (SES) |
| **6 a 7** | `BAIXA` | Normal | Salva no RDS (Contabilizada no Relatório Semanal) |
| **8 a 10** | `NENHUMA` | Normal | Salva no RDS (Contabilizada no Relatório Semanal) |

### Formato das Notificações Enviadas aos Administradores

#### 1. E-mail de Aviso de Urgência (Disparo Imediato)
Contém exatamente os campos exigidos na especificação:
```text
Assunto: [URGENTE] Feedback Crítico Recebido - Urgência: ALTA

Alerta de Avaliação Crítica detectada na plataforma:

- Descrição: A aula sobre arquitetura serverless teve problemas no áudio.
- Urgência: ALTA
- Data de envio: 2026-11-16T20:30:15.123456
```

#### 2. E-mail do Relatório Semanal (Disparo Agendado via Cron)
Contém a média geral, agrupamentos por dia e por urgência, e a listagem detalhada:
```text
Assunto: Relatório Semanal de Feedbacks - Pós-Tech

Relatório Semanal de Feedbacks
=========================================
Média de Avaliações: 6.50

Quantidade de avaliações por dia:
- 2026-11-16: 4

Quantidade de avaliações por urgência:
- ALTA: 1
- MEDIA: 1
- NENHUMA: 2

Detalhamento das Avaliações:
- Data: 2026-11-16T20:30:15 | Urgência: ALTA | Descrição: Problemas no áudio.
- Data: 2026-11-16T20:35:10 | Urgência: MEDIA | Descrição: Exemplos rápidos demais.
- Data: 2026-11-16T20:40:00 | Urgência: NENHUMA | Descrição: Excelente didática!
- Data: 2026-11-16T20:42:11 | Urgência: NENHUMA | Descrição: Conteúdo muito prático.
```

---

## 8. Instruções de Execução Local e Deploy Automatizado

### Pré-requisitos
- **Java 21** e **Maven** (ou uso do wrapper `./mvnw` incluso no projeto)
- **Docker** (para subir o banco PostgreSQL local de testes)
- **AWS CLI** e **AWS SAM CLI** (para deploy automatizado na nuvem)

---

### A. Executando e Testando Localmente (Docker + Insomnia)

1. **Suba o container do PostgreSQL localmente:**
   - **Primeira execução (cria e inicia o container com reinício automático):**
     ```bash
     docker run --name postgres-feedback \
       --restart unless-stopped \
       -e POSTGRES_USER=postgres \
       -e POSTGRES_PASSWORD=postgres \
       -e POSTGRES_DB=feedback_db \
       -p 5432:5432 -d postgres:16-alpine
     ```
   - **Execuções seguintes (caso o container já exista e esteja parado):**
     ```bash
     docker start postgres-feedback
     ```

2. **Execute os testes automatizados:**
   ```bash
   ./mvnw clean test
   ```

3. **Inicie a aplicação com o perfil `local`:**
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

4. **Teste as funções no Insomnia (Coleção Inclusa):**
   Foi disponibilizado na raiz do projeto o arquivo **`insomnia-collection-api-feedback.json`** contendo os 4 cenários de teste pré-configurados. Para utilizá-lo, basta abrir o Insomnia, clicar em **Import** e selecionar o arquivo:
   - **Cenário 1 (`[400] Avaliação Inválida`):** `POST http://localhost:8080/processarAvaliacao` com `"nota": 11` (valida o tratamento de erro `400 Bad Request`).
   - **Cenário 2 (`[200] Avaliação Normal`):** `POST http://localhost:8080/processarAvaliacao` com `"nota": 9` (salva no PostgreSQL sem disparar alerta crítico).
   - **Cenário 3 (`[200] Avaliação Crítica`):** `POST http://localhost:8080/processarAvaliacao` com `"nota": 2` (salva no PostgreSQL, publica na fila SQS e dispara e-mail imediato via SES).
   - **Cenário 4 (`[200] Gerar Relatório Semanal`):** `POST http://localhost:8080/gerarRelatorioSemanal` com Body JSON `{}` (consolida média, agrupamentos por dia/urgência e envia o relatório semanal via SES).

---

### B. Deploy Automatizado na AWS (via AWS SAM)

1. **Empacote a aplicação gerando o artefato otimizado para AWS Lambda (`-aws.jar`):**
   ```bash
   ./mvnw clean package -DskipTests
   ```
   *(O plugin `maven-shade-plugin` gerará o arquivo `target/api-feedback-0.0.1-SNAPSHOT-aws.jar`).*

2. **Crie os parâmetros seguros no AWS Systems Manager (Parameter Store):**
   ```bash
   aws ssm put-parameter --name "/feedback/db_url" --value "jdbc:postgresql://<ENDPOINT_RDS>:5432/feedback_db" --type "String"
   aws ssm put-parameter --name "/feedback/db_user" --value "postgres" --type "String"
   aws ssm put-parameter --name "/feedback/db_pass" --value "<SENHA_RDS>" --type "SecureString"
   ```

3. **Realize o Build e o Deploy Automatizado com o AWS SAM:**
   ```bash
   sam build
   sam deploy --guided
   ```
   Durante o assistente interativo, informe o nome da Stack (ex.: `api-feedback-stack`) e a região desejada (ex.: `sa-east-1` ou `us-east-1`). O AWS CloudFormation/SAM provisionará automaticamente o API Gateway, as 2 funções Lambda, a fila SQS, o agendamento no EventBridge, as políticas de acesso IAM e o alarme do CloudWatch.