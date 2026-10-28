# Regras de Compra

Este documento descreve o modelo utilizado pelo framework para gerar e executar compras.

---

## Instrucao

Uma compra e representada por `PurchaseInstruction`.

A instrucao deve validar seus dados antes da execucao.

Exemplos de invariantes:

- conta obrigatoria;
- operacao obrigatoria;
- valores positivos;
- quantidade de parcelas valida;
- consistencia entre parcelas e valor de contrato quando aplicavel.

---

## Operacoes

O dominio utiliza `PurchaseOperation` para representar os tipos conhecidos.

Mapeamentos atualmente presentes no framework:

| Operacao | Codigo |
|---|---:|
| CASH | 270 |
| INSTALLMENT | 10557 |
| REFINANCING | 10376 |
| INSTALLMENT_CASH_WITHDRAWAL | 10436 |

Esses codigos devem permanecer centralizados no dominio enquanto representarem codigos estaveis do negocio.

---

## Geracao

`PurchaseInstructionGenerator` e responsavel por produzir instrucoes validas para os cenarios suportados.

A geracao deve permanecer separada da execucao real.

Isso permite:

- preview;
- testes deterministicos;
- validacao da instrucao antes da mutacao.

---

## Execucao

O fluxo principal utiliza:

`PurchaseExecutionCoordinator -> PurchaseExecutor -> PurchaseProcedureGateway`

A implementacao SQL Server executa a procedure financeira correspondente.

---

## Seguranca

Compra real e considerada uma operacao effectful.

A execucao deve exigir autorizacao explicita atraves do `EffectfulExecutionGuard`.

A protecao pode existir em mais de um ponto quando isso reduzir risco de bypass.

Mais detalhes:

[Operacoes Effectful](../security/EFFECTFUL-OPERATIONS.md)
