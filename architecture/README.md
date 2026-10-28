# Arquitetura - BMG QA Automation

Esta pasta concentra a documentacao tecnica da plataforma de automacao e validacao financeira.

O objetivo e permitir que qualquer QA, desenvolvedor ou pessoa tecnica consiga entender:

- como o framework esta organizado;
- como os principais fluxos financeiros funcionam;
- quais responsabilidades pertencem a cada camada;
- por que determinadas decisoes arquiteturais foram tomadas;
- como operacoes com efeito sao protegidas;
- como os testes sao organizados;
- quais pontos ainda fazem parte da evolucao da plataforma.

---

## Visao da plataforma

O framework foi estruturado para evitar que regras financeiras, acesso a banco, integracoes SOAP e logica operacional fiquem concentrados diretamente dentro dos testes.

A arquitetura busca separar responsabilidades e permitir evolucao gradual sem perder funcionalidade.

| Camada | Responsabilidade |
|---|---|
| **core** | Configuracao, database, seguranca de execucao, network, wait e XML |
| **domain** | Modelos, value objects e regras do dominio financeiro |
| **application** | Casos de uso, coordenacao de fluxos, validacoes e contratos |
| **infrastructure** | Implementacoes SQL Server, repositories, gateways e executores |
| **features** | Integracoes especificas de negocio, como quitacao SOAP |
| **src/test** | Testes, integracoes, runners e suporte operacional |

---

## Principio de dependencia

A direcao conceitual principal e:

**Tests / Runners -> Application -> Domain**

A infraestrutura fornece implementacoes concretas para contratos utilizados pela aplicacao.

O dominio nao deve depender diretamente de SQL Server, SOAP, Selenium, Allure, arquivos locais ou runners de teste.

Essa separacao reduz acoplamento e facilita a evolucao dos fluxos financeiros.

---

## Fluxo financeiro principal

O principal fluxo financeiro pode ser entendido como:

**Cenario -> Snapshot inicial -> Preparacao -> Execucao -> Batch -> Snapshot final -> Validacao**

| Componente | Responsabilidade |
|---|---|
| `FinancialScenarioRunner` | Entrada dos cenarios financeiros |
| `PaymentScenarioExecution` | Orquestra o ciclo completo do cenario |
| `FinancialSnapshotService` | Captura e recupera snapshots |
| `PaymentPreparation` | Prepara a instrucao de pagamento |
| `PaymentExecutionCoordinator` | Coordena a execucao |
| `PaymentProcessingStateService` | Recupera o estado do processamento |
| `PaymentProcessingReadinessResolver` | Determina se o pos-batch pode prosseguir |
| `FinancialValidationEngine` | Executa as validacoes financeiras |

### Etapas

1. selecionar o cenario financeiro;
2. capturar o estado anterior;
3. preparar a operacao;
4. criar o plano de validacao;
5. executar a operacao;
6. consultar o estado de processamento;
7. aguardar o momento correto do pos-batch;
8. capturar o novo estado;
9. validar as regras financeiras.

---

## Cenarios de pagamento

O fluxo mensal atualmente trabalha com:

- pagamento abaixo do minimo;
- pagamento minimo;
- pagamento parcial de 20%;
- pagamento parcial de 60%;
- pagamento total da fatura.

A quitacao antecipada permanece separada porque representa uma jornada financeira diferente do pagamento mensal.

---

## Snapshots financeiros

Snapshots representam o estado financeiro de uma conta em determinado momento.

| Componente | Papel |
|---|---|
| `FinancialSnapshot` | Modelo do estado financeiro |
| `FinancialSnapshotRepository` | Contrato de persistencia e leitura |
| `FinancialSnapshotService` | Servico de aplicacao |
| `FinancialSnapshotFileStore` | Implementacao local baseada em arquivo |

A estrategia permite comparar o estado pre-batch e pos-batch preservando o estado original utilizado pela validacao.

Mais detalhes:

[ADR-002 - Financial Snapshot Strategy](adr/ADR-002-financial-snapshot-strategy.md)

---

## Pagamentos

O fluxo de pagamento utiliza componentes separados para preparacao, execucao e acompanhamento.

| Componente | Papel |
|---|---|
| `PaymentPreparation` | Prepara o pagamento |
| `PaymentInstruction` | Representa a instrucao |
| `PaymentExecutionCoordinator` | Coordena a execucao |
| `PaymentExecutor` | Contrato de executor |
| `PaymentExecutionResult` | Resultado da operacao |
| `PaymentProcessingState` | Representa o estado do processamento |
| `PaymentProcessingReadinessResolver` | Traduz estado em prontidao de pos-batch |

Os codigos conhecidos de processamento ficam centralizados em `PaymentProcessingState`, evitando numeros magicos espalhados pelo codigo.

---

## Validacao financeira

As regras de validacao sao divididas em componentes independentes.

Exemplos:

- saldo;
- status de processamento;
- status da conta;
- oferta de parcelamento;
- adesao ao parcelamento.

No pagamento total:

**saldo esperado = saldo anterior - valor pago da fatura**

Isso permite tratar corretamente movimentacoes posteriores ao fechamento da fatura.

---

## Fluxo de compra

O fluxo de compra separa geracao, coordenacao, execucao e acesso SQL.

| Componente | Papel |
|---|---|
| `PurchaseInstructionGenerator` | Gera a instrucao de compra |
| `PurchaseInstruction` | Modelo da operacao |
| `PurchaseExecutionCoordinator` | Coordena a execucao |
| `PurchaseExecutor` | Contrato de execucao |
| `SqlServerPurchaseExecutor` | Executor concreto |
| `PurchaseProcedureGateway` | Contrato da procedure |
| `SqlServerPurchaseProcedureGateway` | Implementacao SQL Server |

Operacoes reais devem passar pelo `EffectfulExecutionGuard` antes da mutacao.

---

## Quitacao SOAP

A integracao de quitacao possui separacao entre configuracao, request, response, services e validators.

### Simulacao

A simulacao de quitacao e considerada operacao de leitura.

### Geracao de boleto

A geracao de boleto e uma operacao com efeito.

O `EffectfulExecutionGuard` deve ser aplicado antes da chamada que realmente gera o boleto.

---

## Banco de dados

O acesso generico ao SQL Server e centralizado em `DatabaseAccessor`.

Responsabilidades:

- abrir conexao;
- utilizar `PreparedStatement`;
- aplicar timeout;
- executar queries;
- executar comandos;
- controlar commit;
- controlar rollback;
- fechar recursos.

Fluxos que precisam compartilhar a mesma conexao podem utilizar `DatabaseAccessor.inTransaction(...)`.

O `DatabaseAccessor` nao possui Guard propositalmente, pois e infraestrutura generica utilizada tanto para leitura quanto para escrita.

A protecao effectful deve existir no componente que conhece a intencao da operacao.

---

## Estrutura de testes

| Area | Uso |
|---|---|
| `application` | Testes de casos de uso |
| `core` | Testes dos recursos compartilhados |
| `domain` | Testes de regras e modelos |
| `features` | Testes de integracoes especificas |
| `infrastructure` | Testes das implementacoes concretas |
| `integration` | Banco, SOAP e fluxos integrados |
| `tests` | Cenarios funcionais |
| `tools` | Runners operacionais e utilitarios |

`src/test/tools` possui parte da divida tecnica atual e sera modernizado gradualmente.

---

## Documentacao complementar

### ADRs

- [ADR-001 - Financial Domain-Oriented Platform](adr/ADR-001-financial-domain-oriented-platform.md)
- [ADR-002 - Financial Snapshot Strategy](adr/ADR-002-financial-snapshot-strategy.md)
- [ADR-003 - Financial Test Mass Selection](adr/ADR-003-financial-test-mass-selection.md)

### Regras de negocio

A pasta `business-rules/` concentra documentacao das regras financeiras importantes.

### Diagramas

A pasta `diagrams/` concentra as representacoes visuais dos principais fluxos.

Ordem recomendada:

1. visao geral da arquitetura;
2. pagamento, batch e validacao;
3. fluxo de compra;
4. quitacao SOAP;
5. evolucao da plataforma.

### Estrategia de testes

[TEST-STRATEGY.md](testing/TEST-STRATEGY.md)

### Seguranca

[EFFECTFUL-OPERATIONS.md](security/EFFECTFUL-OPERATIONS.md)

### Roadmap

[ROADMAP.md](roadmap/ROADMAP.md)

---

## Principios arquiteturais

### Regra financeira deve possuir uma fonte principal

Evitar duplicacao de calculos e regras entre testes e cenarios.

### Teste nao deve concentrar regra de negocio

Testes devem orquestrar e validar.

### Infraestrutura deve ficar atras de contratos

Banco, SOAP e integracoes externas devem ser encapsulados.

### Operacoes perigosas devem possuir protecao explicita

Profiles Maven nao substituem Guards no ponto real de mutacao.

### Evolucao incremental

Codigo legado pode ser modernizado gradualmente sem bloquear o uso da plataforma.

---

## Direcao de evolucao

**Framework funcional -> Git -> CI/CD -> Regressao -> Observabilidade -> Expansao de dominios**

O roadmap detalhado esta disponivel em:

[ROADMAP.md](roadmap/ROADMAP.md)
