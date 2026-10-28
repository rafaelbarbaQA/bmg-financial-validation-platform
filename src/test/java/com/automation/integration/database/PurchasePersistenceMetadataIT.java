package com.automation.integration.database;

import com.automation.core.database.DatabaseAccessor;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

@Epic("Financial Validation Platform")
@Feature("Purchase")
public final class PurchasePersistenceMetadataIT {

    private static final String TABLE =
            "dbo.EventosExternosComprasNaoProcessados";

    @Test(groups = { "integration", "database" })
    @Story("Inspecionar identificador da compra persistida")
    public void inspectPurchasePersistenceMetadata()
            throws SQLException {

        DatabaseAccessor database =
                new DatabaseAccessor();

        String columns = readValue(
                database,
                "SELECT STRING_AGG("
                        + "CAST(CONCAT("
                        + "c.column_id, ' | ', "
                        + "c.name, ' | ', "
                        + "TYPE_NAME(c.user_type_id), "
                        + "' | identity=', c.is_identity, "
                        + "' | nullable=', c.is_nullable"
                        + ") AS nvarchar(max)), CHAR(10)) AS VALUE "
                        + "FROM sys.columns c "
                        + "WHERE c.object_id = OBJECT_ID(?)",
                TABLE
        );

        String primaryKey = readValue(
                database,
                "SELECT STRING_AGG("
                        + "CAST(CONCAT("
                        + "i.name, ' | coluna=', c.name, "
                        + "' | key_ordinal=', ic.key_ordinal"
                        + ") AS nvarchar(max)), CHAR(10)) AS VALUE "
                        + "FROM sys.indexes i "
                        + "INNER JOIN sys.index_columns ic "
                        + "ON i.object_id = ic.object_id "
                        + "AND i.index_id = ic.index_id "
                        + "INNER JOIN sys.columns c "
                        + "ON ic.object_id = c.object_id "
                        + "AND ic.column_id = c.column_id "
                        + "WHERE i.object_id = OBJECT_ID(?) "
                        + "AND i.is_primary_key = 1",
                TABLE
        );

        String indexes = readValue(
                database,
                "SELECT STRING_AGG("
                        + "CAST(CONCAT("
                        + "i.name, ' | unique=', i.is_unique, "
                        + "' | coluna=', c.name, "
                        + "' | ordinal=', ic.key_ordinal"
                        + ") AS nvarchar(max)), CHAR(10)) AS VALUE "
                        + "FROM sys.indexes i "
                        + "INNER JOIN sys.index_columns ic "
                        + "ON i.object_id = ic.object_id "
                        + "AND i.index_id = ic.index_id "
                        + "INNER JOIN sys.columns c "
                        + "ON ic.object_id = c.object_id "
                        + "AND ic.column_id = c.column_id "
                        + "WHERE i.object_id = OBJECT_ID(?) "
                        + "AND i.index_id > 0",
                TABLE
        );

        String identityColumn = readValue(
                database,
                "SELECT TOP 1 c.name AS VALUE "
                        + "FROM sys.columns c "
                        + "WHERE c.object_id = OBJECT_ID(?) "
                        + "AND c.is_identity = 1",
                TABLE
        );

        printSection(
                "COLUNAS",
                columns
        );

        printSection(
                "IDENTITY",
                identityColumn
        );

        printSection(
                "PRIMARY KEY",
                primaryKey
        );

        printSection(
                "INDICES",
                indexes
        );

        Allure.parameter(
                "Tabela de persistencia",
                TABLE
        );

        Allure.parameter(
                "Coluna identity",
                identityColumn
        );

        Allure.addAttachment(
                "Colunas da compra",
                "text/plain",
                columns
        );

        Allure.addAttachment(
                "Primary Key da compra",
                "text/plain",
                primaryKey
        );

        Allure.addAttachment(
                "Indices da compra",
                "text/plain",
                indexes
        );
    }

    private static String readValue(
            DatabaseAccessor database,
            String sql,
            Object... parameters
    ) throws SQLException {

        Optional<Map<String, Object>> result =
                database.executeQuery(
                        sql,
                        parameters
                );

        if (result.isEmpty()) {
            return "SEM RESULTADO";
        }

        Object value =
                result.get().get(
                        "VALUE"
                );

        return value == null
                ? "NAO DISPONIVEL"
                : String.valueOf(value);
    }

    private static void printSection(
            String title,
            String content
    ) {

        System.out.println();
        System.out.println(
                "===== " + title + " ====="
        );
        System.out.println(content);
    }
}
