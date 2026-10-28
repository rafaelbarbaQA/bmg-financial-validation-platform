# BMG QA Automation

Framework Java de automação e validação de fluxos financeiros, criado para suportar cenários de pagamento, compra, quitação, processamento batch, snapshots financeiros e integrações com SQL Server e serviços SOAP.

O projeto foi estruturado para separar **regra de negócio**, **orquestração**, **infraestrutura** e **testes**, permitindo evoluir o framework de forma segura, reutilizável e escalável.

---

## Visão geral

O framework suporta jornadas financeiras que podem envolver:

- seleção e preparação de massa;
- captura do estado financeiro antes do processamento;
- geração de instruções de pagamento ou compra;
- execução em modo seguro ou controlado;
- acompanhamento do processamento batch;
- captura do estado após o processamento;
- validação de saldo, status, conta e parcelamento;
- integração com SQL Server;
- integração SOAP para quitação;
- evidências e relatórios de teste.

Fluxo conceitual:

```text
Preparar
   ↓
Executar
   ↓
Processar
   ↓
Capturar
   ↓
Validar
```

---

## Principais capacidades

Atualmente o framework contempla:

- pagamentos abaixo do mínimo;
- pagamento mínimo;
- pagamentos parciais;
- pagamento total da fatura;
- validações pré-batch e pós-batch;
- snapshots financeiros;
- acompanhamento do status de processamento;
- validação de saldo;
- compras;
- quitação antecipada via SOAP;
- geração controlada de boleto;
- consultas e comandos SQL Server;
- execução segura de operações com efeito;
- execução por profiles Maven.

---

## Arquitetura

O código principal está organizado em camadas com responsabilidades claras:

```text
src/main/java/com/automation

├── core
├── domain
├── application
├── infrastructure
└── features
```

### Responsabilidade de cada camada

| Camada | Responsabilidade |
|---|---|
| `core` | Configuração, database, segurança de execução, network, wait e XML |
| `domain` | Modelos, value objects e regras do domínio financeiro |
| `application` | Casos de uso, coordenação de fluxos, validações e contratos |
| `infrastructure` | Implementações SQL Server, repositories, gateways e executores |
| `features` | Integrações específicas, como quitação SOAP |
| `src/test` | Testes, integrações, runners e suporte de execução |

Fluxo de dependência esperado:

```text
Tests / Runners
      ↓
Application
      ↓
Domain
      ↓
Infrastructure
      ↓
SQL / SOAP / UI
```

O domínio permanece independente de detalhes de infraestrutura.

Para uma visão técnica mais aprofundada, consulte:

[Documentação de arquitetura](architecture/README.md)

---

## Fluxo financeiro principal

Um dos principais fluxos do framework é a validação de cenários financeiros antes e depois do batch.

```text
FinancialScenario
      ↓
PaymentScenarioExecution
      ↓
captureBefore()
      ↓
preparePayment()
      ↓
createValidationPlan()
      ↓
executePayment()
      ↓
loadPaymentProcessingState()
      ↓
captureAfter()
      ↓
validatePostBatch()
```

### Cenários modelados

- pagamento abaixo do mínimo;
- pagamento mínimo;
- pagamento parcial de 20%;
- pagamento parcial de 60%;
- pagamento total da fatura.

A quitação antecipada é tratada separadamente do fluxo de pagamento mensal, pois representa uma operação de negócio diferente.

---

## Snapshots financeiros

O framework utiliza snapshots para comparar o estado financeiro antes e depois de uma operação.

```text
FinancialSnapshot
      ↓
FinancialSnapshotRepository
      ↓
FinancialSnapshotService
      ↓
FinancialSnapshotFileStore
```

Essa estratégia permite preservar o estado anterior e validar alterações depois do processamento batch sem espalhar consultas e comparações pelo código de teste.

---

## Pagamentos

O fluxo de pagamento é dividido entre preparação, execução, processamento e validação.

```text
PaymentPreparation
      ↓
PaymentInstruction
      ↓
PaymentExecutionCoordinator
      ↓
PaymentExecutor
      ↓
PaymentExecutionResult
```

O cálculo do valor de pagamento possui uma fonte central de regra, evitando duplicação entre diferentes cenários.

O estado do processamento é representado por `PaymentProcessingState`, que centraliza os códigos conhecidos e expõe comportamento semântico, como:

```java
isWaitingForBatch()
isProcessed()
```

Assim, outras partes do framework não precisam trabalhar diretamente com números mágicos.

---

## Validação pós-batch

As validações são executadas por componentes independentes e podem ser compostas em uma pipeline.

Exemplos:

- status de processamento;
- status da conta;
- saldo;
- oferta de parcelamento;
- adesão ao parcelamento.

No pagamento total, a expectativa de saldo segue a regra:

```text
saldo esperado = saldo atual antes do batch - valor pago da fatura
```

O framework não assume incorretamente que o saldo atual precisa obrigatoriamente terminar em zero.

---

## Fluxo de compra

O fluxo de compra separa geração, execução e acesso ao procedimento SQL.

```text
PurchaseInstructionGenerator
        ↓
PurchaseInstruction
        ↓
PurchaseExecutionCoordinator
        ↓
PurchaseExecutor
        ↓
SqlServerPurchaseExecutor
        ↓
PurchaseProcedureGateway
        ↓
SqlServerPurchaseProcedureGateway
        ↓
SQL Server
```

Operações que alteram dados externos passam por proteção explícita antes da execução.

---

## Quitação SOAP

A integração de quitação possui contratos separados para configuração, request e response.

```text
Simular Quitação
      ↓
validação da resposta
      ↓
Gerar Boleto de Quitação
      ↓
validação da resposta
```

A simulação é considerada uma operação de leitura.

A geração de boleto é considerada uma operação com efeito e exige autorização explícita antes da chamada ao serviço externo.

---

## Segurança de operações com efeito

O framework possui uma barreira explícita:

```text
EffectfulExecutionGuard
```

Operações capazes de alterar sistemas externos devem exigir:

```text
-DpermitirOperacoesComEfeito=true
```

Sem essa autorização, a operação deve ser bloqueada.

Alguns fluxos financeiros legados também podem exigir:

```text
-Dfinancial.mode=REAL
```

Princípio utilizado:

> Profiles Maven controlam o que será selecionado para executar. Guards controlam se uma operação perigosa pode realmente acontecer.

Mais detalhes:

[Segurança de operações effectful](architecture/security/EFFECTFUL-OPERATIONS.md)

---

## Banco de dados

O acesso principal ao SQL Server é centralizado em:

```text
DatabaseAccessor
```

Responsabilidades:

- abrir conexões;
- executar queries parametrizadas;
- executar comandos;
- utilizar `PreparedStatement`;
- aplicar timeout;
- fechar recursos;
- controlar commit e rollback;
- executar várias operações na mesma conexão com `inTransaction()`.

Exemplo conceitual:

```java
databaseAccessor.inTransaction(connection -> {
    // operações usando a mesma conexão
    return resultado;
});
```

O `DatabaseAccessor` permanece genérico e não contém regras financeiras nem bloqueio effectful.

Runners operacionais antigos em `src/test/tools` ainda podem possuir acessos JDBC legados e devem ser modernizados gradualmente.

---

## Configuração

Use como referência:

```text
src/main/resources/config.example.properties
```

Crie localmente um arquivo de ambiente, por exemplo:

```text
src/main/resources/config-128.properties
```

Arquivos reais `config-*.properties` são ignorados pelo Git.

### Credenciais

Credenciais não devem ser armazenadas em arquivos versionados.

Variáveis de ambiente utilizadas:

```text
DB_USER
DB_PASSWORD
CSLIGHT_USERNAME
CSLIGHT_PASSWORD
```

Exemplo no PowerShell:

```powershell
$env:DB_USER="usuario"
$env:DB_PASSWORD="senha"
$env:CSLIGHT_USERNAME="usuario"
$env:CSLIGHT_PASSWORD="senha"
```

Configurações não sensíveis podem permanecer nos arquivos locais de ambiente.

---

## Execução

### Testes padrão

```bash
mvn test
```

A execução padrão é a opção segura para desenvolvimento e validação local.

Ela exclui grupos classificados como:

```text
integration
effectful
database
soap
ui
e2e
```

### Testes de integração

```bash
mvn test -Pintegration-tests -Denv=128
```

Esse profile pode acessar recursos externos como banco, SOAP e UI, mas continua excluindo operações classificadas como `effectful`.

### Testes com efeito

```bash
mvn test -Peffectful-tests -Denv=128 -DpermitirOperacoesComEfeito=true
```

Use somente quando houver intenção explícita de executar operações que alteram sistemas externos.

---

## Stack

| Tecnologia | Uso |
|---|---|
| Java 11 | Compatibilidade de compilação do projeto |
| Maven | Build, dependências e profiles |
| TestNG | Framework de testes |
| Selenium WebDriver | Automação web |
| Apache HttpClient 5 | Comunicação HTTP |
| Microsoft SQL Server JDBC | Integração com banco |
| Apache Commons CSV | Leitura de massas de teste |
| Allure | Evidências e relatórios |
| AspectJ | Integração com reporting |
| WebDriverManager | Gerenciamento de drivers |

O projeto pode ser executado com JDKs mais recentes, mas atualmente é compilado com compatibilidade Java 11.

---

## Estrutura de testes

```text
src/test/java/com/automation

├── application
├── core
├── domain
├── features
├── infrastructure
├── integration
├── tests
└── tools
```

`src/test/tools` contém runners operacionais e utilitários de massa.

Parte desses runners é legado e será modernizada gradualmente sem bloquear a evolução do núcleo.

---

## Massas, evidências e arquivos locais

Dados operacionais não devem ser versionados.

Exemplos:

- contas;
- pagamentos;
- migrações;
- lotes;
- retries;
- evidências;
- snapshots locais;
- logs;
- resultados Allure.

A pasta abaixo é ignorada pelo Git:

```text
src/test/resources/massas/
```

Também são ignorados:

```text
target/
allure-results/
allure-report/
evidencias/
*.log
*.backup
*.bak
.financial-snapshots/
```

---

## Documentação técnica

A documentação aprofundada do framework fica em `architecture/`.

### Arquitetura

[Visão técnica da arquitetura](architecture/README.md)

### ADRs

As principais decisões arquiteturais são registradas como Architecture Decision Records.

- [ADR-001 - Financial Domain-Oriented Platform](architecture/adr/ADR-001-financial-domain-oriented-platform.md)
- [ADR-002 - Financial Snapshot Strategy](architecture/adr/ADR-002-financial-snapshot-strategy.md)
- [ADR-003 - Financial Test Mass Selection](architecture/adr/ADR-003-financial-test-mass-selection.md)
- [Índice dos ADRs](architecture/adr/README.md)

### Diagramas

[Diagramas da arquitetura](architecture/diagrams/)

### Estratégia de testes

[Estratégia de testes](architecture/testing/TEST-STRATEGY.md)

### Segurança

[Operações effectful](architecture/security/EFFECTFUL-OPERATIONS.md)

### Roadmap

[Roadmap técnico](architecture/roadmap/ROADMAP.md)

---

## Princípios do projeto

### Regra de negócio não pertence ao teste

Testes devem orquestrar e validar comportamento, não concentrar cálculo financeiro e acesso a infraestrutura.

### Uma regra deve possuir uma fonte principal

Cálculos e regras financeiras não devem ser duplicados entre vários cenários.

### Infraestrutura deve ficar atrás de contratos

Banco, SOAP e operações externas devem ser acessados por repositories, gateways e executores.

### Operações perigosas precisam de proteção explícita

Profiles Maven não substituem Guards no ponto real de mutação.

### Evolução gradual

Código legado não precisa ser reescrito inteiro de uma vez.

A estratégia é modernizar progressivamente sem perder funcionalidade.

---

## Antes de um commit

Execute:

```bash
mvn test
```

Resultado esperado:

```text
BUILD SUCCESS
```

Também é recomendado:

```bash
git diff --check
```

Confirme que não existem no commit:

- credenciais;
- massas reais;
- evidências;
- logs;
- backups;
- arquivos gerados.

---

## Estado atual

A base principal está preparada para evolução via Git, com:

- arquitetura financeira reorganizada;
- snapshots separados entre `application` e `infrastructure`;
- fluxo de pagamento consolidado;
- fluxo de compra protegido;
- quitação SOAP estruturada;
- estados de processamento centralizados;
- acesso transacional centralizado no `DatabaseAccessor`;
- secrets removidos dos arquivos versionados;
- massas operacionais removidas;
- build padrão validado com `BUILD SUCCESS`.

Ainda existem runners operacionais em `src/test/tools` que serão modernizados gradualmente.

---

## Evolução

Direção desejada:

```text
Framework funcional
      ↓
Git
      ↓
CI/CD
      ↓
regressão automatizada
      ↓
observabilidade
      ↓
execução controlada por ambiente
      ↓
expansão para novos fluxos financeiros
```

O objetivo é manter uma **plataforma de validação financeira reutilizável, segura e evolutiva**.
