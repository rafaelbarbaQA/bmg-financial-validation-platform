# Operacoes Effectful - Seguranca de Execucao

Este documento descreve a estrategia utilizada para proteger testes e ferramentas capazes de alterar sistemas externos.

---

## O que e uma operacao effectful

Uma operacao e considerada effectful quando pode produzir efeito real no ambiente.

Exemplos:

- gerar boleto;
- inserir pagamento;
- executar procedure de compra;
- alterar registros financeiros;
- executar runners de massa com persistencia;
- chamar servicos externos que geram artefatos reais.

---

## Objetivo

Evitar que uma execucao comum de testes realize mutacoes acidentais.

Principio:

**Operacoes reais precisam de intencao explicita.**

---

## EffectfulExecutionGuard

A principal barreira utilizada pelo framework e:

`EffectfulExecutionGuard`

A autorizacao utiliza a propriedade:

`permitirOperacoesComEfeito`

Uma execucao autorizada exige:

`-DpermitirOperacoesComEfeito=true`

Na ausencia dessa autorizacao, o comportamento deve ser **fail-closed**.

---

## Duas camadas de protecao

### Selecao da suite

O Maven separa testes atraves de groups e profiles.

Execucao padrao:

`mvn test`

Testes effectful permanecem excluidos.

Execucao explicita:

`mvn test -Peffectful-tests -Denv=128 -DpermitirOperacoesComEfeito=true`

### Protecao no codigo

Mesmo quando um teste e selecionado, o componente responsavel pela mutacao deve validar a autorizacao.

Isso impede que a seguranca dependa apenas da configuracao do Maven.

---

## Onde o Guard deve ficar

O Guard deve ficar proximo ao ponto real de mutacao.

Locais adequados:

- executor;
- gateway;
- repository de escrita;
- service que dispara uma operacao externa.

Exemplos atuais:

- `SqlServerPurchaseExecutor`;
- `SqlServerPurchaseProcedureGateway`;
- `EventoExternoPagamentoRepository`;
- `GerarBoletoQuitacaoAntecipadaService`.

---

## Onde o Guard nao deve ficar

Nem todo componente precisa de Guard.

Exemplos:

- modelos;
- requests;
- responses;
- validators;
- repositories somente leitura;
- utilitarios;
- `DatabaseAccessor`.

### DatabaseAccessor

`DatabaseAccessor` e propositalmente generico.

Ele e utilizado tanto para leitura quanto para comandos.

Por isso, a protecao effectful deve existir no componente que conhece a intencao de negocio da operacao.

---

## Defesa em profundidade

Quando fizer sentido, uma operacao pode possuir protecao em mais de um nivel.

Exemplo:

**SqlServerPurchaseExecutor -> Guard -> PurchaseProcedureGateway -> Guard -> SQL Server**

Isso reduz a possibilidade de bypass acidental.

---

## financial.mode

Alguns fluxos legados tambem utilizam:

`-Dfinancial.mode=REAL`

Essa propriedade representa uma barreira adicional de intencao.

Ela nao substitui o `EffectfulExecutionGuard`.

---

## Dry-run

Um dry-run deve ser executavel sem autorizacao effectful quando nenhuma mutacao externa ocorrer.

O Guard deve proteger apenas o caminho que realmente altera o ambiente.

---

## CI/CD

Por padrao, pipelines compartilhados nao devem executar operacoes effectful.

| Stage | Effectful |
|---|---|
| Build | Nao |
| Unit/Test | Nao |
| Integration | Nao |
| Regressao segura | Nao |
| Effectful controlado | Somente com autorizacao explicita |

---

## Credenciais

O Guard nao substitui secret management.

Credenciais devem vir de:

- variaveis de ambiente;
- secret store da pipeline;
- mecanismo corporativo equivalente.

Nunca devem ficar:

- no codigo;
- em properties versionado;
- no README;
- em logs;
- em massa de teste.

---

## Runners legados

Alguns runners em `src/test/tools` ainda possuem fluxos transacionais proprios.

Dividas conhecidas incluem:

- JDBC direto;
- commit e rollback manual;
- caminhos historicos de massa;
- combinacao de dry-run e execucao real.

Esses runners devem ser modernizados gradualmente.

Novos fluxos nao devem copiar esse padrao.

---

## Checklist para novas operacoes

Antes de implementar uma operacao que altera ambiente:

1. identificar o ponto real de mutacao;
2. classificar o teste como effectful;
3. aplicar `EffectfulExecutionGuard`;
4. evitar logging de dados sensiveis;
5. utilizar credenciais externas ao codigo;
6. testar o comportamento bloqueado;
7. testar a execucao autorizada quando aplicavel.

---

## Principio final

**Pipeline define quando executar.**

**Suite define o que executar.**

**Guard define se a mutacao esta autorizada.**
