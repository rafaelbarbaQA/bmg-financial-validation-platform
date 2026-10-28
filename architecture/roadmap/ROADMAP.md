# Roadmap - BMG QA Automation

Este roadmap descreve a direcao de evolucao da plataforma.

O objetivo nao e transformar toda divida tecnica em bloqueador da primeira versao.

A evolucao deve acontecer de forma incremental, com commits pequenos e validacao continua.

---

## Estado atual

A base principal ja possui:

- arquitetura orientada a dominio;
- camada de aplicacao;
- infraestrutura SQL Server;
- fluxo financeiro;
- snapshots;
- pagamentos;
- compra;
- quitacao SOAP;
- validacao pre e pos-batch;
- `EffectfulExecutionGuard`;
- profiles Maven;
- configuracao segura de secrets;
- suite padrao validada com `BUILD SUCCESS`.

---

# NOW - Consolidar a V1

## Git corporativo

- publicar a base principal;
- manter branch e historico organizados;
- garantir documentacao;
- evitar arquivos operacionais no repositorio.

## Documentacao

- README principal;
- architecture README;
- diagramas;
- ADRs;
- regras de negocio;
- estrategia de testes;
- documentacao effectful;
- roadmap.

## Seguranca basica

- manter secrets fora do codigo;
- manter massas reais fora do Git;
- manter logs e evidencias fora do Git;
- proteger operacoes de mutacao.

## Build

- manter `mvn test` verde;
- manter Java 11 ate confirmacao do runtime corporativo;
- evitar mudancas desnecessarias de dependencias.

---

# NEXT - Reduzir divida tecnica

## Runners operacionais

Modernizar `src/test/tools`.

Prioridades:

- remover `DriverManager.getConnection` direto;
- utilizar `DatabaseAccessor`;
- utilizar `inTransaction()` quando aplicavel;
- separar claramente dry-run e execucao real;
- adicionar Guard onde houver mutacao;
- remover caminhos historicos hardcoded.

## Massa externa

Substituir caminhos fixos por configuracao.

Direcao desejada:

`-Dmass.file=<arquivo>`

Objetivos:

- framework sem dados reais;
- runners reutilizaveis;
- massa externa por ambiente;
- execucao reproduzivel.

## Reporting

Reduzir acoplamento entre infraestrutura e Allure.

Direcao:

**infraestrutura executa -> reporting observa**

## SOAP

Melhorias previstas:

- centralizar tratamento de SOAP Fault;
- padronizar validacao de responses;
- reforcar validacao dos builders;
- manter geracao de boleto protegida.

## Payment

Evolucoes previstas:

- padronizar erros de externalPaymentId;
- avaliar value object para IDs de evento;
- confirmar semantica de zeros a esquerda em NossoNumero;
- revisar concorrencia na prevencao de pagamento duplicado.

---

# NEXT - CI/CD

Depois da V1 estabilizada no Git:

## Pipeline segura

Stages sugeridos:

1. compile;
2. unit/application;
3. integracao segura;
4. database/SOAP;
5. regressao;
6. effectful controlado.

## Secrets

Utilizar secret store corporativo para:

- `DB_USER`;
- `DB_PASSWORD`;
- `CSLIGHT_USERNAME`;
- `CSLIGHT_PASSWORD`;
- tokens futuros.

## Evidencias

A V1 mantém um snapshot estático e controlado do Allure em `docs/allure-v1-report/`. Na evolução de CI/CD, o relatório deverá também ser publicado automaticamente como artefato da pipeline.

---

# LATER - Evolucao da plataforma

## Regressao automatizada

Criar suites por dominio e criticidade.

Exemplos:

- payment;
- purchase;
- settlement;
- batch;
- installment.

## Observabilidade

Evoluir diagnostico atraves de:

- logs estruturados;
- correlacao de execucao;
- metricas;
- dashboards;
- ferramentas corporativas.

## Paralelismo

Avaliar somente depois de garantir:

- independencia de massa;
- isolamento de dados;
- idempotencia;
- seguranca effectful.

## Novos fluxos

Expandir a plataforma para novas jornadas financeiras preservando a separacao arquitetural.

## Java

Avaliar migracao de Java 11 somente apos confirmacao do runtime corporativo suportado.

---

## Criterio de prioridade

Uma melhoria deve subir de prioridade quando:

- reduz risco operacional;
- elimina duplicacao importante;
- aumenta reutilizacao;
- desbloqueia CI/CD;
- melhora manutencao;
- reduz execucao manual;
- melhora seguranca.

---

## Principio de evolucao

Nao e necessario eliminar toda divida tecnica antes de utilizar a plataforma.

**Funcional -> Versionado -> Automatizado -> Observavel -> Evoluido**
