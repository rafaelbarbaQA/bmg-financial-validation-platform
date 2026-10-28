# Estrategia de Testes - BMG QA Automation

Este documento define como os testes da plataforma sao organizados e quando cada nivel deve ser utilizado.

O objetivo e manter uma suite rapida e segura para desenvolvimento sem perder a capacidade de validar integracoes reais e fluxos financeiros completos.

---

## Principio central

Nem todo teste precisa acessar um ambiente real.

A estrategia separa:

- testes locais e deterministicos;
- testes de aplicacao;
- testes de infraestrutura;
- banco de dados;
- SOAP;
- UI;
- integracao;
- E2E;
- operacoes effectful.

---

## Niveis de teste

| Tipo | Objetivo |
|---|---|
| Unitario | Validar regra ou componente isolado |
| Application | Validar coordenacao e casos de uso |
| Infrastructure | Validar implementacoes concretas |
| Database | Validar integracao com SQL Server |
| SOAP | Validar comunicacao com servicos |
| UI | Validar interface web |
| Integration | Validar varios componentes juntos |
| E2E | Validar uma jornada completa |
| Effectful | Validar operacao que pode alterar o ambiente |

---

## Execucao padrao

Comando:

`mvn test`

Essa deve ser a suite mais segura para desenvolvimento local e CI padrao.

Ela exclui:

- `integration`;
- `database`;
- `soap`;
- `ui`;
- `e2e`;
- `effectful`.

A expectativa e que essa suite seja:

- rapida;
- deterministica;
- segura;
- reproduzivel;
- independente de mutacoes externas.

---

## Testes de integracao

Comando:

`mvn test -Pintegration-tests -Denv=<ambiente>`

Esse profile pode acessar:

- banco de dados;
- SOAP;
- UI;
- outros recursos externos.

O grupo `effectful` continua excluido.

---

## Testes effectful

Comando:

`mvn test -Peffectful-tests -Denv=<ambiente> -DpermitirOperacoesComEfeito=true`

Esses testes podem:

- gerar boleto;
- inserir pagamento;
- executar procedures;
- realizar compras;
- alterar dados externos.

A execucao deve ser sempre intencional.

---

## Dry-run e preview

Sempre que possivel, fluxos operacionais devem possuir modo de dry-run ou preview.

Objetivos:

- validar preparacao;
- validar selecao de massa;
- verificar instrucoes geradas;
- reduzir risco;
- diagnosticar problemas antes da execucao real.

Um dry-run nao deve exigir autorizacao effectful quando nenhuma mutacao real ocorrer.

---

## Pre-batch, batch e pos-batch

### Pre-batch

Responsabilidades tipicas:

- selecionar conta;
- capturar snapshot inicial;
- validar pre-condicoes;
- preparar a operacao;
- executar pagamento ou compra.

### Batch

O framework nao deve assumir que toda mudanca financeira acontece imediatamente.

O estado de processamento deve ser acompanhado pelos componentes especificos.

### Pos-batch

Responsabilidades tipicas:

- confirmar readiness;
- capturar snapshot final;
- validar saldo;
- validar status;
- validar parcelamento;
- comparar o estado anterior e posterior.

---

## Testes de dominio

Testes de dominio devem validar regras sem depender de SQL Server, SOAP ou UI.

Exemplos:

- invariantes de `PaymentInstruction`;
- calculo de pagamento;
- cenarios financeiros;
- `PurchaseInstruction`;
- estados de processamento.

---

## Testes de aplicacao

Validam coordenacao entre componentes.

Exemplos:

- `PaymentPreparation`;
- `PaymentExecutionCoordinator`;
- `FinancialSnapshotService`;
- readiness de pos-batch;
- validation engine.

Quando fizer sentido, dependencias externas devem ser substituidas por fakes, stubs ou executores de preview.

---

## Massa de teste

Massas reais ou operacionais nao devem ser versionadas.

O framework deve diferenciar:

- massa ficticia versionavel;
- massa local;
- massa selecionada automaticamente;
- massa fornecida manualmente.

Mais detalhes:

[ADR-003 - Financial Test Mass Selection](../adr/ADR-003-financial-test-mass-selection.md)

---

## Regressao

A regressao deve priorizar:

1. regras financeiras criticas;
2. processamento de pagamento;
3. saldo;
4. status;
5. compra;
6. quitacao;
7. integracoes principais;
8. seguranca effectful.

Direcao futura de pipeline:

**Unit/Application -> Integration -> Database/SOAP -> E2E -> Effectful controlado**

---

## Regra para novos testes

Antes de criar um novo teste, responda:

1. qual regra esta sendo validada?
2. essa regra ja possui cobertura?
3. precisa realmente de ambiente?
4. pode ser testada localmente?
5. altera algum sistema externo?
6. qual group TestNG representa esse risco?
7. qual massa sera utilizada?

---

## Seguranca

Um teste classificado como `effectful` nao deve depender somente do profile Maven para seguranca.

O componente que realiza a mutacao deve possuir protecao apropriada.

Veja:

[Operacoes Effectful](../security/EFFECTFUL-OPERATIONS.md)
