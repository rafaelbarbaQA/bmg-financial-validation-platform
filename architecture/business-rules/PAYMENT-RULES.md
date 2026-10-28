# Regras de Pagamento

Este documento descreve os principais cenarios de pagamento representados atualmente pelo framework.

---

## Cenarios suportados

### Abaixo do minimo

Representa um pagamento inferior ao valor minimo esperado para a fatura.

Esse cenario e utilizado principalmente para validar comportamento negativo ou regras especificas do processamento.

### Pagamento minimo

O valor executado corresponde ao valor minimo calculado para a fatura.

### Pagamento parcial

O framework possui cenarios parciais como:

- 20% da referencia utilizada pelo calculo;
- 60% da referencia utilizada pelo calculo.

### Pagamento total

O pagamento total corresponde ao valor total da fatura.

Importante:

**pagamento total da fatura nao significa obrigatoriamente saldo atual igual a zero.**

O saldo atual pode possuir movimentos posteriores ao fechamento da fatura.

---

## Fonte unica de calculo

O valor do pagamento deve ser calculado por uma fonte central.

O framework utiliza `PaymentAmountCalculator` para evitar que cada cenario implemente sua propria regra monetaria.

Isso reduz:

- duplicacao;
- divergencia entre cenarios;
- risco de regras diferentes para o mesmo conceito.

---

## Instrucao de pagamento

O resultado da preparacao e representado por `PaymentInstruction`.

A instrucao deve conter somente dados validos para execucao.

Validacoes de dominio devem acontecer antes da chamada ao executor.

---

## Execucao

A execucao utiliza o contrato `PaymentExecutor`.

Isso permite diferentes implementacoes, como:

- preview;
- execucao real;
- futuras integracoes.

O coordenador responsavel e `PaymentExecutionCoordinator`.

---

## Quitacao nao e pagamento mensal

A quitacao antecipada nao deve ser tratada como um `PaymentScenario` comum.

Ela possui:

- fluxo proprio;
- integracao SOAP;
- regras especificas;
- geracao de boleto.

Por isso, permanece separada do fluxo mensal de pagamento.
