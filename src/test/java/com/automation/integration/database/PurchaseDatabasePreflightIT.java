package com.automation.integration.database;

import com.automation.core.config.ConfigReader;
import com.automation.core.database.DatabaseAccessor;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public final class PurchaseDatabasePreflightIT {

    @Test(groups = {"integration", "database"})
    public void shouldConnectToConfiguredPurchaseDatabase()
            throws SQLException {

        String expectedIp =
                requireProperty(
                        "expected.db.ip"
                );

        String expectedDatabase =
                System.getProperty(
                        "expected.db.name",
                        "bmg"
                );

        String configuredIp =
                ConfigReader.getProperty(
                        "DB_IP"
                );

        String configuredPort =
                ConfigReader.getProperty(
                        "DB_PORT"
                );

        String configuredDatabase =
                ConfigReader.getProperty(
                        "DB_NAME"
                );

        System.out.println();
        System.out.println(
                "================ DATABASE PREFLIGHT ================"
        );

        System.out.println(
                "Ambiente.................: "
                        + System.getProperty(
                                "env",
                                "NAO INFORMADO"
                        )
        );

        System.out.println(
                "DB IP....................: "
                        + configuredIp
        );

        System.out.println(
                "DB PORT..................: "
                        + configuredPort
        );

        System.out.println(
                "DB NAME..................: "
                        + configuredDatabase
        );

        assertEquals(
                configuredIp,
                expectedIp
        );

        assertEquals(
                configuredPort,
                "1433"
        );

        assertEquals(
                configuredDatabase,
                expectedDatabase
        );

        DatabaseAccessor databaseAccessor =
                new DatabaseAccessor();

        Optional<Map<String, Object>> result =
                databaseAccessor.executeQuery(
                        "SELECT "
                                + "DB_NAME() AS DATABASE_NAME, "
                                + "1 AS CONNECTION_OK"
                );

        assertTrue(
                result.isPresent(),
                "Database preflight returned no result."
        );

        Object databaseName =
                result.get().get(
                        "DATABASE_NAME"
                );

        Object connectionOk =
                result.get().get(
                        "CONNECTION_OK"
                );

        System.out.println(
                "Banco conectado..........: "
                        + databaseName
        );

        System.out.println(
                "Conexao..................: "
                        + connectionOk
        );

        System.out.println(
                "Operacao com efeito......: NAO"
        );

        System.out.println(
                "===================================================="
        );

        assertTrue(
                String.valueOf(databaseName)
                        .equalsIgnoreCase(
                                expectedDatabase
                        ),
                "Connected database differs from expected database. "
                        + "Expected: "
                        + expectedDatabase
                        + ", actual: "
                        + databaseName
        );

        assertEquals(
                ((Number) connectionOk).intValue(),
                1
        );
    }

    private static String requireProperty(
            String propertyName
    ) {

        String value =
                System.getProperty(
                        propertyName
                );

        if (value == null
                || value.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Informe -D"
                            + propertyName
                            + "=..."
            );
        }

        return value.trim();
    }
}