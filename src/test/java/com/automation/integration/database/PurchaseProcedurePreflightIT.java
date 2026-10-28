package com.automation.integration.database;

import com.automation.core.database.DatabaseAccessor;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

import static org.testng.Assert.assertTrue;

public final class PurchaseProcedurePreflightIT {

    @Test(groups = { "integration", "database" })
    public void shouldValidatePurchaseProcedureSource() throws SQLException {

        long accountId = Long.parseLong(
                requireProperty("purchase.preflight.account")
        );

        DatabaseAccessor database = new DatabaseAccessor();

        Optional<Map<String, Object>> environment =
                database.executeQuery(
                        "SELECT @@SERVERNAME AS SERVER_NAME, "
                                + "DB_NAME() AS DATABASE_NAME"
                );

        assertTrue(environment.isPresent());

        System.out.println();
        System.out.println("===== CONEXAO DA AUTOMACAO =====");
        System.out.println("Servidor: " + environment.get().get("SERVER_NAME"));
        System.out.println("Banco...: " + environment.get().get("DATABASE_NAME"));

        Optional<Map<String, Object>> source =
                database.executeQuery(
                        "SELECT TOP 1 "
                                + "EC.Id_Conta AS ID_CONTA, "
                                + "CARTAO.Id_Cartao AS ID_CARTAO, "
                                + "CARTAO.Cartao AS CARTAO, "
                                + "CARTAO.Portador AS PORTADOR, "
                                + "EC.ProximoVencimentoReal AS PROX_VENC_REAL, "
                                + "EC.ProximoVencimentoPadrao AS PROX_VENC_PADRAO "
                                + "FROM EstadosContas EC WITH (NOLOCK) "
                                + "CROSS APPLY ( "
                                + "SELECT TOP 1 "
                                + "Cart.Id_Cartao, Cart.Cartao, Cart.Portador "
                                + "FROM Cartoes Cart WITH (NOLOCK) "
                                + "WHERE Cart.Id_Conta = EC.Id_Conta "
                                + "AND Cart.Cartao IS NOT NULL "
                                + "AND Cart.Portador = 1 "
                                + "ORDER BY Cart.Id_Cartao DESC "
                                + ") CARTAO "
                                + "WHERE EC.Id_Conta = ?",
                        accountId
                );

        System.out.println();
        System.out.println("===== SOURCE DA SPR_REALIZA_COMPRA =====");

        if (source.isEmpty()) {
            System.out.println("RESULTADO: ZERO LINHAS");
            throw new IllegalStateException(
                    "A conta nao atende ao SELECT interno da SPR_REALIZA_COMPRA."
            );
        }

        Map<String, Object> row = source.get();

        System.out.println("Conta.............: " + row.get("ID_CONTA"));
        System.out.println("IdCartao..........: " + row.get("ID_CARTAO"));
        System.out.println("Cartao............: " + row.get("CARTAO"));
        System.out.println("Portador..........: " + row.get("PORTADOR"));
        System.out.println("Prox Venc Real....: " + row.get("PROX_VENC_REAL"));
        System.out.println("Prox Venc Padrao..: " + row.get("PROX_VENC_PADRAO"));
        System.out.println("RESULTADO.........: APTO PARA INSERT");
    }

    private static String requireProperty(String name) {
        String value = System.getProperty(name);

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Informe -D" + name + "=...");
        }

        return value.trim();
    }
}
