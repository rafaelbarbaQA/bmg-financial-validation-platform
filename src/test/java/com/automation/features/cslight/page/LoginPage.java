package com.automation.features.cslight.page;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class LoginPage {

    @FindBy(id = "lgnTCY_UserName")
    private WebElement campoUsuario;

    @FindBy(id = "lgnTCY_Password")
    private WebElement campoSenha;

    @FindBy(id = "lgnTCY_LoginButton")
    private WebElement botaoLogin;

    public LoginPage(
            WebDriver driver
    ) {

        if (driver == null) {
            throw new IllegalArgumentException(
                    "O WebDriver não pode ser nulo."
            );
        }

        PageFactory.initElements(
                driver,
                this
        );
    }

    public void realizarLogin(
            String usuario,
            String senha
    ) {

        validarCredenciais(
                usuario,
                senha
        );

        campoUsuario.clear();
        campoUsuario.sendKeys(
                usuario
        );

        campoSenha.clear();
        campoSenha.sendKeys(
                senha
        );

        botaoLogin.click();
    }

    private void validarCredenciais(
            String usuario,
            String senha
    ) {

        if (usuario == null
                || usuario.isBlank()) {

            throw new IllegalArgumentException(
                    "O usuário do CSLight não foi informado."
            );
        }

        if (senha == null
                || senha.isBlank()) {

            throw new IllegalArgumentException(
                    "A senha do CSLight não foi informada."
            );
        }
    }
}
