# Regras de Negocio

Esta pasta documenta as principais regras financeiras atualmente representadas pelo framework.

O objetivo nao e duplicar o codigo, mas registrar o comportamento esperado de cada jornada para facilitar manutencao, revisao e evolucao.

## Documentos

| Documento | Conteudo |
|---|---|
| [Payment Rules](PAYMENT-RULES.md) | Cenarios e regras de pagamento |
| [Post-Batch Rules](POST-BATCH-RULES.md) | Processamento, snapshots e saldo |
| [Purchase Rules](PURCHASE-RULES.md) | Regras relacionadas a compra |
| [Settlement Rules](SETTLEMENT-RULES.md) | Simulacao e quitacao antecipada |

## Principio

Regra de negocio deve possuir uma fonte principal no codigo.

Esta documentacao descreve o comportamento esperado, enquanto a implementacao permanece no dominio e na camada de aplicacao.
