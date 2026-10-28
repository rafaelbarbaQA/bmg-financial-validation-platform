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
public final class PurchaseProcedureMetadataIT {

    @Test(groups = { "integration", "database" })
    @Story("Inspecionar persistencia da procedure de compra")
    public void inspectPurchaseProcedureMetadata()
            throws SQLException {

        String procedure =
                System.getProperty(
                        "purchase.procedure",
                        "dbo.SPR_REALIZA_COMPRA"
                ).trim();

        DatabaseAccessor database =
                new DatabaseAccessor();

        Allure.parameter(
                "Procedure",
                procedure
        );

        String objectId = readValue(
                database,
                "SELECT CAST(OBJECT_ID(?) AS varchar(30)) AS VALUE",
                procedure
        );

        String parameters = readValue(
                database,
                "SELECT STRING_AGG("
                        + "CAST(CONCAT("
                        + "p.parameter_id, ' | ', "
                        + "p.name, ' | ', "
                        + "TYPE_NAME(p.user_type_id), "
                        + "' | output=', p.is_output"
                        + ") AS nvarchar(max)), CHAR(10)) AS VALUE "
                        + "FROM sys.parameters p "
                        + "WHERE p.object_id = OBJECT_ID(?)",
                procedure
        );

        String dependencies = readValue(
                database,
                "SELECT STRING_AGG("
                        + "CAST(CONCAT("
                        + "COALESCE(referenced_schema_name, ''), "
                        + "CASE WHEN referenced_schema_name IS NULL "
                        + "THEN '' ELSE '.' END, "
                        + "referenced_entity_name"
                        + ") AS nvarchar(max)), CHAR(10)) AS VALUE "
                        + "FROM sys.sql_expression_dependencies "
                        + "WHERE referencing_id = OBJECT_ID(?) "
                        + "AND referenced_entity_name IS NOT NULL",
                procedure
        );

        String definition = readValue(
                database,
                "SELECT OBJECT_DEFINITION(OBJECT_ID(?)) AS VALUE",
                procedure
        );

        printSection(
                "OBJECT ID",
                objectId
        );

        printSection(
                "PARAMETROS",
                parameters
        );

        printSection(
                "OBJETOS REFERENCIADOS",
                dependencies
        );

        printSection(
                "DEFINICAO DA PROCEDURE",
                definition
        );

        Allure.addAttachment(
                "Parametros - SPR_REALIZA_COMPRA",
                "text/plain",
                parameters
        );

        Allure.addAttachment(
                "Objetos referenciados - SPR_REALIZA_COMPRA",
                "text/plain",
                dependencies
        );

        Allure.addAttachment(
                "Definicao - SPR_REALIZA_COMPRA",
                "text/sql",
                definition
        );

        String normalized =
                definition.toUpperCase();

        boolean identityHint =
                normalized.contains("SCOPE_IDENTITY")
                        || normalized.contains("@@IDENTITY")
                        || normalized.contains("IDCOMPRA")
                        || normalized.contains("ID_COMPRA")
                        || normalized.contains(" OUTPUT");

        Allure.parameter(
                "Possivel retorno de identificador",
                identityHint ? "SIM" : "NAO IDENTIFICADO"
        );

        System.out.println();
        System.out.println(
                "============================================================"
        );
        System.out.println(
                " PURCHASE PROCEDURE INSPECTION"
        );
        System.out.println(
                "============================================================"
        );
        System.out.println(
                "Procedure...............: " + procedure
        );
        System.out.println(
                "Object ID...............: " + objectId
        );
        System.out.println(
                "Possivel retorno ID.....: "
                        + (identityHint ? "SIM" : "NAO IDENTIFICADO")
        );
        System.out.println(
                "============================================================"
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

        if (value == null) {
            return "NAO DISPONIVEL";
        }

        return String.valueOf(value);
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
