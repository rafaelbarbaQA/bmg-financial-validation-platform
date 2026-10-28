# Regras de Pos-Batch

Este documento descreve as principais regras utilizadas para acompanhar e validar o processamento financeiro apos uma operacao.

---

## Snapshot inicial

Antes da operacao financeira, o framework captura um `FinancialSnapshot`.

Esse snapshot representa o estado utilizado como referencia para validacoes futuras.

---

## Processamento

O estado do pagamento e representado por `PaymentProcessingState`.

Os codigos atualmente reconhecidos sao centralizados no proprio modelo.

### Aguardando batch

Status:

`0`

Comportamento semantico:

`isWaitingForBatch()`

### Processado

Status:

`2`

Comportamento semantico:

`isProcessed()`

Qualquer outro valor e tratado como estado inesperado ate que exista uma regra explicita para ele.

---

## Readiness

O pos-batch so deve prosseguir quando o processamento estiver pronto.

Essa decisao e responsabilidade de:

- `PaymentProcessingReadinessResolver`;
- `PaymentPostBatchReadinessGuard`.

O objetivo e impedir que validacoes finais sejam executadas antes do processamento esperado.

---

## Snapshot final

A captura do estado final deve acontecer depois da validacao de readiness.

O snapshot inicial e o snapshot final devem pertencer a mesma conta.

---

## Regra de saldo para pagamento total

Para pagamento total da fatura:

**valor pago = valor total da fatura**

A expectativa de saldo e:

**saldo esperado = saldo atual anterior - valor pago**

Exemplo:

- saldo atual antes: 1.200,00;
- valor da fatura: 1.000,00;
- pagamento: 1.000,00;
- saldo esperado apos processamento: 200,00.

Essa regra evita assumir que o pagamento total da fatura deve zerar todo o saldo atual.

---

## Validacoes

O pos-batch pode combinar validacoes como:

- status do pagamento;
- status da conta;
- saldo;
- oferta de parcelamento;
- adesao ao parcelamento.

As validacoes devem retornar resultados estruturados e nao depender diretamente do mecanismo de teste.
