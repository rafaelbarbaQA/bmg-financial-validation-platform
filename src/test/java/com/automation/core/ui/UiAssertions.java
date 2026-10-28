package com.automation.core.ui;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;

public class UiAssertions {

    private static WebDriver driver;

    private UiAssertions() {
        // Construtor privado para evitar instanciação
    }

    public static void setDriver(WebDriver webDriver) {
        driver = webDriver;
    }

    /**
     * Verifica se dois valores String são iguais.
     * @param actual O valor real obtido.
     * @param expected O valor esperado.
     * @param message A mensagem a ser exibida se a asserção falhar.
     */
    public static void assertEquality(String actual, String expected, String message) {
        System.out.println("Asserting equality: Actual='" + actual + "', Expected='" + expected + "'");
        Assert.assertEquals(actual, expected, message);
    }

    /**
     * Verifica se um WebElement está visível na tela.
     * @param element O WebElement a ser verificado.
     * @param message A mensagem a ser exibida se a asserção falhar.
     */
    public static void assertElementIsDisplayed(WebElement element, String message) {
        // Adicionando uma verificação para null para evitar NullPointerException
        if (element == null) {
            Assert.fail("O WebElement é nulo. " + message);
        }
        System.out.println("Asserting element display: " + message);
        // Usa o método isDisplayed() do próprio WebElement
        Assert.assertTrue(element.isDisplayed(), message);
    }

    /**
     * Verifica se o título da página é o esperado.
     * Requer que o driver tenha sido setado no UiAssertions.
     * @param expectedTitle O título esperado da página.
     * @param message A mensagem a ser exibida se a asserção falhar.
     */
    public static void assertPageTitle(String expectedTitle, String message) {
        if (driver == null) {
            throw new IllegalStateException("WebDriver not set for UiAssertions. Call setDriver() first.");
        }
        String actualTitle = driver.getTitle();
        System.out.println("Asserting page title: Actual='" + actualTitle + "', Expected='" + expectedTitle + "'");
        Assert.assertEquals(actualTitle, expectedTitle, message);
    }

    /**
     * Verifica se uma condição booleana é verdadeira.
     * @param condition A condição booleana a ser verificada.
     * @param message A mensagem a ser exibida se a asserção falhar.
     */
    public static void assertTrueCondition(boolean condition, String message) {
        System.out.println("Asserting true condition: " + message);
        Assert.assertTrue(condition, message);
    }
}
