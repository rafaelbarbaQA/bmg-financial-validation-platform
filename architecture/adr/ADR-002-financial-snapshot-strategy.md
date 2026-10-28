# ADR-002 — Estratégia de snapshots financeiros

- **Status:** Aceito
- **Data:** 2026-08-03
- **Responsáveis:** Equipe de Qualidade e Engenharia
- **Decisão:** Registrar e comparar o estado financeiro antes e depois dos processamentos por batch.

---

## 1. Contexto

Os principais fluxos da plataforma dependem de batches que alteram o estado financeiro das contas.

Entre as alterações possíveis estão:

- processamento de pagamentos;
- liquidação de faturas;
- criação de créditos;
- geração de ofertas de parcelamento;
- alteração de saldos;
- geração e liquidação de boletos;
- encerramento de operações;
- atualização de status;
- criação de lançamentos financeiros.

Validar apenas o estado final não é suficiente.

Para comprovar corretamente o comportamento do sistema, é necessário conhecer:

- o estado antes do batch;
- a ação preparada;
- o resultado esperado;
- o estado depois do batch;
- as diferenças efetivamente encontradas.

---

## 2. Problema

Como validar processamentos financeiros assíncronos sem perder os valores e estados existentes antes da execução do batch?

Como produzir uma evidência que permita identificar:

- o que mudou;
- o que deveria ter mudado;
- o que permaneceu incorretamente;
- em qual componente surgiu a divergência?

---

## 3. Decisão

A plataforma utilizará snapshots financeiros imutáveis para representar o estado de negócio em momentos específicos.

Inicialmente, existirão dois momentos principais:

```text
BEFORE_BATCH
AFTER_BATCH