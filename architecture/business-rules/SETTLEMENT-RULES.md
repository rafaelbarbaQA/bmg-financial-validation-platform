# Regras de Quitacao Antecipada

Este documento descreve o fluxo de quitacao antecipada utilizado pelo framework.

Quitacao permanece separada do fluxo comum de pagamento mensal.

---

## Simulacao

A primeira etapa e a simulacao da quitacao.

A simulacao consulta o servico SOAP e retorna dados financeiros da operacao.

Por nao gerar alteracao externa, e tratada como operacao de leitura.

---

## Validacao da simulacao

A resposta deve ser interpretada e validada antes da etapa de geracao.

Falhas tecnicas, SOAP Faults ou respostas invalidas nao devem permitir continuidade silenciosa do fluxo.

---

## Geracao de boleto

A geracao do boleto de quitacao altera o estado externo e e considerada effectful.

Antes do envio da requisicao real, o framework exige autorizacao atraves de:

`EffectfulExecutionGuard`

---

## Fluxo

**Simular quitacao -> validar simulacao -> autorizar operacao -> gerar boleto -> validar resposta**

---

## Contratos SOAP

O framework separa:

- configuracao;
- request de simulacao;
- response de simulacao;
- request de geracao;
- response de geracao.

Essa separacao evita misturar montagem de XML, comunicacao e regra funcional no mesmo componente.

---

## Seguranca

A classe que monta o request nao precisa possuir Guard.

A response nao precisa possuir Guard.

O Guard deve estar no componente que efetivamente envia a operacao capaz de gerar o boleto.

Mais detalhes:

[Operacoes Effectful](../security/EFFECTFUL-OPERATIONS.md)

## Critério de sucesso da quitação

A simulação é considerada válida quando:

- `CodRetorno = 0`;
- `TotalAntecipacao > 0`.

A geração do boleto é considerada bem-sucedida quando:

- `CodRetorno = 0`;
- `IdBoleto > 0`.

A persistência pode ser confirmada posteriormente por consulta read-only ao SQL Server.

`FlagRegistrado` e o registro bancário externo não fazem parte do critério de sucesso adotado pelo framework.
