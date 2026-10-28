package com.automation.core.ui;

import com.automation.core.config.ConfigReader;

import io.github.bonigarcia.wdm.WebDriverManager;

import org.apache.commons.io.FileUtils;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;

import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.io.IOException;

import java.text.SimpleDateFormat;

import java.time.Duration;

import java.util.Date;
import java.util.Locale;

public class BaseUiTest {

    private static final String SCREENSHOT_FOLDER =
            System.getProperty("user.dir")
                    + "/test-output/reports/screenshots";

    protected static WebDriver driver;

    private final String browser =
            System.getProperty(
                    "browser",
                    "chrome"
            );

    @BeforeMethod
    public void setup() {

        String targetUrl =
                ConfigReader.getProperty(
                        "CSLIGHT_URL"
                );

        driver =
                criarDriver(browser);

        driver.manage()
                .window()
                .maximize();

        driver.manage()
                .timeouts()
                .implicitlyWait(
                        Duration.ofSeconds(10)
                );

        System.out.println(
                "Acessando URL: "
                        + targetUrl
        );

        driver.get(targetUrl);

        UiAssertions.setDriver(driver);
    }

    private WebDriver criarDriver(
            String browserName
    ) {

        String browserNormalizado =
                browserName
                        .trim()
                        .toLowerCase(Locale.ROOT);

        switch (browserNormalizado) {

            case "chrome":
                WebDriverManager
                        .chromedriver()
                        .setup();

                ChromeOptions chromeOptions =
                        new ChromeOptions();

                return new ChromeDriver(
                        chromeOptions
                );

            case "firefox":
                WebDriverManager
                        .firefoxdriver()
                        .setup();

                FirefoxOptions firefoxOptions =
                        new FirefoxOptions();

                return new FirefoxDriver(
                        firefoxOptions
                );

            case "edge":
                WebDriverManager
                        .edgedriver()
                        .setup();

                EdgeOptions edgeOptions =
                        new EdgeOptions();

                return new EdgeDriver(
                        edgeOptions
                );

            default:
                throw new IllegalArgumentException(
                        "Navegador não suportado: "
                                + browserName
                );
        }
    }

    public static String captureScreenshot(
            String testName
    ) {

        if (driver == null) {
            return null;
        }

        String timestamp =
                new SimpleDateFormat(
                        "yyyyMMdd_HHmmss"
                ).format(new Date());

        String fileName =
                SCREENSHOT_FOLDER
                        + File.separator
                        + testName
                        + "_"
                        + timestamp
                        + ".png";

        File screenshotDirectory =
                new File(
                        SCREENSHOT_FOLDER
                );

        if (!screenshotDirectory.exists()
                && !screenshotDirectory.mkdirs()) {

            throw new IllegalStateException(
                    "Não foi possível criar a pasta de screenshots: "
                            + SCREENSHOT_FOLDER
            );
        }

        try {
            File screenshotFile =
                    ((TakesScreenshot) driver)
                            .getScreenshotAs(
                                    OutputType.FILE
                            );

            FileUtils.copyFile(
                    screenshotFile,
                    new File(fileName)
            );

            return fileName;

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Erro ao salvar o screenshot.",
                    exception
            );
        }
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {

        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }
}