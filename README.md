# Plataforma de Feedback de Aulas - Tech Challenge (Fase 4)

Aplicação Serverless em nuvem (AWS) desenvolvida com **Java 21**, **Spring Boot 4.1.1**, **Spring Cloud Function** e **PostgreSQL**, aplicando **Clean Architecture (Ports and Adapters)** e o **Princípio da Responsabilidade Única (SRP)**.

---

## 1. Problema e Objetivo

Em cursos on-line, é essencial que estudantes enviem feedbacks sobre as aulas e que administradores acompanhem rapidamente a satisfação dos alunos. Esta plataforma automatiza:
1. O recebimento e persistência de avaliações de aulas via API REST (`POST /avaliacao`).
2. A detecção de avaliações críticas e o envio automático de notificações de urgência aos administradores.
3. A consolidação e o envio periódico de relatórios semanais com métricas e médias de avaliações.

---

## 2. Arquitetura da Solução e Modelo de Cloud (AWS)

Foi adotado o modelo **Serverless (FaaS + BaaS)** na **Amazon Web Services (AWS)** para eliminar o custo de servidores ociosos 24/7, garantir escalabilidade automática sob demanda e isolar responsabilidades.

### Componentes Envolvidos:
* **Amazon API Gateway:** Expõe o endpoint HTTP `POST /avaliacao` e atua como porta de entrada para a função de ingestão.
* **AWS Lambda (Java 21 + AWS SnapStart):** Executa as funções serverless sob demanda com inicialização otimizada (redução de *cold start*).
* **Amazon EventBridge (CloudWatch Events):** Aciona de forma agendada (`cron(0 8 ? * MON *)`) a função de geração do relatório semanal.
* **Amazon RDS (PostgreSQL):** Banco de dados relacional em nuvem responsável pela persistência segura das avaliações.
* **Amazon SQS (`fila-notas-criticas`):** Fila de mensagens para desacoplamento de eventos de avaliações críticas.
* **Amazon SES (Simple Email Service):** Serviço de disparo de e-mails para alertas imediatos de urgência e envio do relatório semanal consolidado.
* **AWS Systems Manager (Parameter Store - SSM):** Armazenamento seguro de credenciais e parâmetros de conexão com o banco (`DB_URL`, `DB_USER`, `DB_PASS`).
* **Amazon CloudWatch (Logs & Alarms):** Monitoramento contínuo, coleta de logs estruturados e alarmes automáticos de erros de execução.

### Fluxo Arquitetural:
```text
[Estudante] 
   │ (POST /avaliacao)
   ▼
[Amazon API Gateway] ──► [Lambda 1: ProcessarAvaliacaoFunction]
                                ├──► Salva no [Amazon RDS PostgreSQL]
                                └──► Se crítica (nota <= 5):
                                        ├──► Publica evento no [Amazon SQS]
                                        └──► Envia Alerta via [Amazon SES]

[Amazon EventBridge] ──► [Lambda 2: GerarRelatorioSemanalFunction]
 (CRON: Segundas 8h)            ├──► Consulta dados no [Amazon RDS PostgreSQL]
                                └──► Consolida métricas e envia Relatório via [Amazon SES]
```

---

## 3. Separação de Responsabilidades (Funções Serverless)

Respeitando o **Single Responsibility Principle (SRP)**, a aplicação divide o processamento em duas funções serverless independentes:

1. **`processarAvaliacao` (`ProcessarAvaliacaoFunction`):**
    * **Gatilho:** Requisição HTTP `POST /avaliacao` via API Gateway.
    * **Responsabilidade:** Validar a nota recebida (0 a 10), classificar automaticamente o nível de urgência (`ALTA`, `MEDIA`, `BAIXA`, `NENHUMA`), persistir a avaliação no PostgreSQL e, caso seja crítica (`ALTA` ou `MEDIA`), acionar o disparo de alerta imediato contendo Descrição, Urgência e Data de envio.
2. **`gerarRelatorioSemanal` (`GerarRelatorioSemanalFunction`):**
    * **Gatilho:** Evento agendado semanalmente via Amazon EventBridge.
    * **Responsabilidade:** Consultar as avaliações registradas, calcular a média geral das notas, agrupar o volume de avaliações por dia e por urgência, listar os itens (Descrição, Urgência e Data de envio) e disparar o relatório consolidado aos administradores via Amazon SES.

---

## 4. Clean Architecture (Estrutura de Pacotes)

O código-fonte está organizado para isolar as regras de negócio de frameworks externos:

```text
com.pos.tech.challenge.feedback
├── domain
│   ├── entities       # Entidades de domínio puras (Avaliacao) e regras de corte
│   └── enums          # Classificação de urgência (UrgenciaEnum)
├── application
│   ├── ports          # Contratos de entrada/saída (AvaliacaoRepository, NotificacaoGateway)
│   └── usecases       # Regras de negócio (ProcessarAvaliacaoUseCase, GerarRelatorioSemanalUseCase)
├── adapters
│   ├── dtos           # Objetos de transferência (AvaliacaoRequestDTO, AvaliacaoResponseDTO, RelatorioSemanalDTO)
│   └── repositories   # Implementações dos adaptadores (AvaliacaoRepositoryImpl, NotificacaoGatewayImpl)
└── infrastructure
    ├── config         # Definição dos Beans Serverless (AvaliacaoFunctions, UseCaseConfig)
    └── persistence    # Mapeamento JPA/Hibernate (AvaliacaoEntity, SpringDataAvaliacaoRepository)
```

---

## 5. Segurança e Governança de Acesso

* **Princípio do Menor Privilégio (IAM Roles por Função):**
    * A função `ProcessarAvaliacaoFunction` possui permissão restrita para publicar na fila `FilaNotasCriticas` (`SQSSendMessagePolicy`) e disparar alertas via SES (`SESSendEmailPolicy`).
    * A função `GerarRelatorioSemanalFunction` possui apenas permissão de leitura no banco e envio de e-mail (`SESSendEmailPolicy`), sem acesso de escrita na fila SQS.
* **Proteção de Dados e Segredos:** Nenhuma senha ou URL sensível fica exposta no código-fonte. As variáveis `DB_URL`, `DB_USER` e `DB_PASS` são resolvidas dinamicamente via **AWS Systems Manager Parameter Store (`{{resolve:ssm:...}}`)**.

---

## 6. Monitoramento e Observabilidade

* **Logs Estruturados (SLF4J / CloudWatch Logs):** Todas as emissões de alertas críticos (`[ALERTA CRITICO]`) e consolidações de relatórios (`[RELATORIO SEMANAL]`) geram registros padronizados coletados automaticamente nos grupos de log `/aws/lambda/...` do Amazon CloudWatch.
* **Métricas e Alarmes (`AWS::CloudWatch::Alarm`):** O template provisiona o alarme `AlarmeErroProcessarAvaliacao`, que monitora a métrica nativa `Errors` da função Lambda em janelas de 5 minutos e sinaliza qualquer falha de execução (`Threshold >= 1`).

---

## 7. Documentação da API e Regras de Negócio

### Endpoint: Receber Avaliação
* **Método e Rota:** `POST /avaliacao`
* **Headers:** `Content-Type: application/json`

#### Exemplo de Request Payload:
```json
{
  "descricao": "A aula sobre arquitetura serverless teve problemas no áudio.",
  "nota": 3
}
```

#### Exemplo de Response Payload (`200 OK`):
```json
{
  "id": "c4a760a8-dbcf-4254-a2d9-6a44e852762a",
  "mensagem": "Avaliação registrada com sucesso."
}
```

### Regra de Classificação de Urgência:
* **Nota 0 a 3:** Urgência `ALTA` (Crítica -> Dispara notificação automática)
* **Nota 4 a 5:** Urgência `MEDIA` (Crítica -> Dispara notificação automática)
* **Nota 6 a 7:** Urgência `BAIXA` (Normal)
* **Nota 8 a 10:** Urgência `NENHUMA` (Normal)

---

## 8. Instruções de Build, Testes e Deploy Automatizado

### Pré-requisitos
* Java 21
* Maven (ou utilização do wrapper `./mvnw` incluso)
* AWS CLI e AWS SAM CLI configurados

### 1. Executar os Testes Automatizados Localmente
```bash
./mvnw clean test
```

### 2. Gerar o Pacote Compilado para AWS Lambda
```bash
./mvnw clean package -DskipTests
```
*(Gera o artefato `target/api-feedback-0.0.1-SNAPSHOT-aws.jar` otimizado com o `maven-shade-plugin`).*

### 3. Realizar o Deploy Automatizado na AWS via SAM
```bash
sam build
sam deploy --guided
```
Durante o processo guiado, informe o nome da stack (ex: `api-feedback-stack`) e a região da AWS (ex: `us-east-1`). O AWS SAM provisionará automaticamente o API Gateway, as duas funções Lambda, a fila SQS, o agendamento no EventBridge, as roles IAM e o alarme no CloudWatch.