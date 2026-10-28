package com.automation.integration.quitacao;

import com.automation.core.database.DatabaseAccessor;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Optional;

public final class DatabaseConnectionProbeIT {

    @Test
    public void probeDatabaseConnection() throws Exception {

        DatabaseAccessor db = new DatabaseAccessor();

        Optional<Map<String, Object>> contexto = db.executeQuery(
                "SELECT @@SERVERNAME AS SERVER_NAME, DB_NAME() AS DATABASE_NAME"
        );

        Optional<Map<String, Object>> porBoleto = db.executeQuery(
                "SELECT TOP (1) Id_Conta AS ID_CONTA, Id_Boleto AS ID_BOLETO "
                        + "FROM BoletosEmitidos WHERE Id_Boleto = ?",
                571717682L
        );

        Optional<Map<String, Object>> exato = db.executeQuery(
                "SELECT TOP (1) Id_Conta AS ID_CONTA, Id_Boleto AS ID_BOLETO "
                        + "FROM BoletosEmitidos WHERE Id_Conta = ? AND Id_Boleto = ?",
                2227197L,
                571717682L
        );

        System.out.println();
        System.out.println("===== DATABASE PROBE =====");
        System.out.println("CONEXAO.......: " + contexto.orElse(null));
        System.out.println("POR BOLETO....: " + porBoleto.orElse(null));
        System.out.println("CONTA+BOLETO..: " + exato.orElse(null));
        System.out.println("==========================");
    }
}
