# ADR-001 — Plataforma orientada ao domínio financeiro

- **Status:** Aceito
- **Data:** 2026-08-03
- **Responsáveis:** Equipe de Qualidade e Engenharia
- **Decisão:** Organizar a automação como uma plataforma de validação financeira orientada a domínio.

---

## 1. Contexto

O projeto precisa validar fluxos financeiros complexos, incluindo:

- pagamentos de fatura;
- processamento por batch;
- geração e pagamento de boletos;
- quitação antecipada;
- parcelamento de fatura;
- créditos excedentes;
- juros;
- IOF;
- taxas;
- descontos;
- encargos;
- saldos;
- regras específicas por produto.

Uma automação tradicional, organizada somente por testes, páginas, utilitários e serviços genéricos, tende a apresentar os seguintes problemas:

- regras financeiras espalhadas pelos testes;
- consultas SQL misturadas com lógica de negócio;
- classes de serviço com responsabilidades excessivas;
- dificuldade para reutilizar regras;
- baixo poder de diagnóstico;
- alto acoplamento com Selenium, SOAP, REST e banco de dados;
- dificuldade para adicionar produtos e cenários;
- ausência de rastreabilidade antes e depois dos batches.

O sistema validado não é apenas uma aplicação com telas. Ele é um sistema financeiro orientado por regras, estados, eventos e processamentos assíncronos.

Por isso, a plataforma de validação deve representar o domínio financeiro, e não apenas os mecanismos técnicos usados para executar testes.

---

## 2. Problema

Como estruturar a automação para que ela:

1. represente conceitos financeiros reais;
2. mantenha as regras de negócio independentes da infraestrutura;
3. permita reconstruir cálculos financeiros;
4. registre o estado antes e depois dos batches;
5. execute diferentes cenários de forma configurável;
6. produza evidências auditáveis;
7. cresça para novos produtos sem duplicação;
8. permaneça sustentável no longo prazo?

---

## 3. Decisão

A solução será construída como uma:

# Financial Validation Platform

A plataforma será orientada aos domínios financeiros da empresa.

Os principais domínios previstos são:

- Payment;
- Invoice;
- Settlement;
- Installment;
- Credit;
- Batch;
- Account;
- Product;
- Operation;
- Financial Snapshot.

A arquitetura será dividida em responsabilidades explícitas.

### Domain

Representa conceitos, entidades, valores e regras do negócio financeiro.

Não poderá depender de:

- Selenium;
- Playwright;
- TestNG;
- JUnit;
- SQL;
- SOAP;
- REST;
- arquivos de configuração;
- detalhes de infraestrutura.

### Application

Orquestra casos de uso e cenários financeiros.

Responsabilidades:

- coordenar etapas;
- selecionar regras;
- chamar portas de entrada e saída;
- construir contextos;
- disparar validadores;
- retornar resultados.

A camada de aplicação não deverá conter SQL nem fórmulas financeiras.

### Infrastructure

Implementa os mecanismos técnicos necessários.

Exemplos:

- SQL Server;
- SOAP;
- REST;
- Selenium;
- Playwright;
- arquivos;
- relatórios;
- configurações;
- integrações externas.

### Validation

Comprova os resultados do sistema.

Os validadores serão independentes e especializados.

Exemplos:

- PaymentProcessingValidator;
- InvoiceBalanceValidator;
- InstallmentOfferValidator;
- CreditBalanceValidator;
- SettlementAmountValidator.

### Financial Rules

As regras financeiras existirão em componentes próprios.

Exemplos:

- PaymentCalculationPolicy;
- InterestCalculationRule;
- IofCalculationRule;
- DiscountCalculationRule;
- SettlementCompositionRule.

Uma regra financeira não deverá ser duplicada em testes, engines ou repositórios.

---

## 4. Princípios obrigatórios

### 4.1 Domínio independente

O domínio financeiro deverá permanecer independente de ferramentas e integrações.

### 4.2 Regra financeira em um único lugar

Toda fórmula, percentual, taxa, critério ou política deverá possuir uma única fonte de verdade dentro da plataforma.

### 4.3 Engine apenas orquestra

Engines coordenam o fluxo, mas não executam diretamente:

- SQL;
- cálculos;
- asserts;
- requisições SOAP;
- ações de interface.

### 4.4 Repository apenas acessa dados

Repositories não deverão tomar decisões de negócio.

Eles deverão:

- consultar;
- inserir;
- atualizar;
- mapear;
- retornar dados.

### 4.5 Validator apenas valida

Cada validador deverá possuir uma responsabilidade clara.

Um validador de crédito não deverá validar parcelamento ou quitação.

### 4.6 Contexto explícito

O estado de uma execução deverá ser representado por objetos de contexto.

O contexto poderá conter:

- cenário executado;
- conta selecionada;
- fatura;
- boleto;
- pagamento;
- valores esperados;
- identificadores gerados;
- snapshot anterior;
- snapshot posterior;
- resultado das validações.

### 4.7 Snapshot antes e depois

Processamentos por batch deverão ser validados por comparação de estado.

A plataforma deverá conseguir demonstrar:

- como a conta estava antes;
- qual ação foi preparada;
- qual batch foi executado;
- como a conta ficou depois;
- qual diferença era esperada;
- qual diferença foi encontrada.

### 4.8 Evidência auditável

Uma falha financeira deverá exibir, quando aplicável:

- valor esperado;
- valor encontrado;
- diferença;
- fórmula utilizada;
- componentes da fórmula;
- fonte dos dados;
- identificadores da conta, fatura e boleto;
- etapa em que ocorreu a divergência.

### 4.9 Nenhum SQL nos testes

Classes de teste não deverão conter instruções SQL.

### 4.10 Nenhuma regra em classes Utils

Não será utilizada uma classe genérica de utilitários para concentrar regras ou responsabilidades indefinidas.

Classes deverão possuir nomes que revelem sua intenção.

---

## 5. Fluxo arquitetural esperado

```text
Test / Runner / CLI
        |
        v
Financial Engine
        |
        v
Scenario Module
        |
        +--> Financial Rule
        |
        +--> Calculator
        |
        +--> Repository Port
        |
        +--> Integration Port
        |
        +--> Snapshot
        |
        +--> Validators
        |
        v
Financial Execution Result
        |
        v
Audit Report