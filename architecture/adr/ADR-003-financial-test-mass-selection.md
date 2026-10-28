# ADR-003 — Seleção de massas financeiras de teste

- **Status:** Aceito
- **Data:** 2026-08-04
- **Responsáveis:** Equipe de Qualidade e Engenharia
- **Decisão:** Representar e selecionar massas financeiras elegíveis com critérios explícitos, permitindo seleção manual e automática.

---

## 1. Contexto

Os cenários financeiros não dependem apenas de uma conta específica.

Frequentemente, a execução necessita de conjuntos de contas com características determinadas.

Exemplos:

- cinco contas com parcelamento e status 0;
- cinco contas sem parcelamento e status 10;
- contas com boleto de fatura tipo 7;
- contas elegíveis para geração de boleto de quitação tipo 9;
- contas com parcelamento e incidência de IOF;
- contas somente com saldo de fatura anterior;
- contas sem compras no ciclo atual;
- contas de um produto específico;
- contas em determinada etapa de prévia, corte ou vencimento.

A massa poderá ser identificada manualmente pelo QA ou localizada automaticamente pela plataforma.

---

## 2. Problema

Como permitir que diferentes projetos encontrem massas financeiras adequadas sem:

- duplicar consultas SQL;
- colocar critérios de seleção dentro dos testes;
- acoplar cenários a uma tabela específica;
- criar uma implementação diferente para seleção manual e automática;
- reutilizar acidentalmente a mesma conta em cenários incompatíveis?

---

## 3. Decisão

A plataforma utilizará o conceito de:

# Financial Test Mass

Uma massa financeira representa um conjunto de contas elegíveis para um cenário de negócio.

A seleção será baseada em critérios explícitos.

Exemplos de critérios:

- quantidade necessária;
- produto;
- status da conta;
- status da fatura;
- existência de parcelamento;
- ausência de parcelamento;
- incidência de IOF;
- existência de saldo anterior;
- existência de compras no ciclo;
- tipo de boleto;
- ausência de boleto de quitação;
- quantidade de operações;
- datas de corte e vencimento;
- elegibilidade para determinado fluxo.

---

## 4. Modos de seleção

A plataforma suportará inicialmente:

### MANUAL

As contas são informadas pelo usuário.

Mesmo sendo selecionadas manualmente, deverão passar pela validação de elegibilidade.

### AUTOMATIC

A plataforma consulta a base aplicando os critérios solicitados e seleciona a quantidade necessária.

Uma evolução futura poderá suportar:

### MANUAL_WITH_AUTOMATIC_FALLBACK

As contas manuais são avaliadas e as contas inválidas são substituídas automaticamente.

---

## 5. Mesmo critério, origens diferentes

A regra do cenário não deverá mudar conforme a origem da massa.

Exemplo:

```text
Critério:
5 contas com parcelamento e status 0