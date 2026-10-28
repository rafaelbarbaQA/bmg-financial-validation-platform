package com.automation.tests.ui.cslight;

import com.automation.features.cslight.page.LoginPage;

import com.automation.core.ui.UiAssertions;
import com.automation.core.ui.BaseUiTest;
import com.automation.core.config.ConfigReader;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;

import org.testng.annotations.Test;

@Epic("CSLight")
@Feature("Autenticação")
@Story("Login")
@Owner("QA Automation")
@Severity(SeverityLevel.CRITICAL)
public class LoginTest extends BaseUiTest {

    @Test(
            groups = {
                    "ui",
                    "smoke",
                    "cslight"
            }
    )
    @Description(
            "Realiza o login no CSLight utilizando "
                    + "credenciais configuradas localmente."
    )
    public void deveRealizarLoginComSucesso() {

        String usuario =
                ConfigReader.getProperty(
                        "CSLIGHT_USERNAME"
                );

        String senha =
                ConfigReader.getProperty(
                        "CSLIGHT_PASSWORD"
                );

        LoginPage loginPage =
                new LoginPage(driver);

        loginPage.realizarLogin(
                usuario,
                senha
        );

        UiAssertions.assertPageTitle(
                "dock",
                "Não apresentou dock"
        );
    }
}
