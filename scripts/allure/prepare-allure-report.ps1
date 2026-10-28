param(
    [string]$EnvironmentName = "141"
)

$ErrorActionPreference = "Stop"

# scripts/allure -> scripts -> project root
$projectRoot =
    Split-Path `
        -Parent `
        (Split-Path -Parent $PSScriptRoot)

Set-Location $projectRoot

$utf8 =
    New-Object System.Text.UTF8Encoding($false)

$resultsDir =
    Join-Path `
        $projectRoot `
        "target\allure-results"

$configPath =
    Join-Path `
        $projectRoot `
        "src\main\resources\config-$EnvironmentName.properties"

New-Item `
    -ItemType Directory `
    -Force `
    -Path $resultsDir |
    Out-Null


# ============================================================
# LER PROPRIEDADES DO AMBIENTE
# ============================================================

$properties = @{}

if (Test-Path $configPath) {

    Get-Content $configPath |
    ForEach-Object {

        $line = $_.Trim()

        if (
            $line.Length -gt 0 `
            -and -not $line.StartsWith("#") `
            -and $line.Contains("=")
        ) {

            $parts =
                $line.Split(
                    '=',
                    2
                )

            $key =
                $parts[0].Trim()

            $value =
                $parts[1].Trim()

            $properties[$key] =
                $value
        }
    }
}


# ============================================================
# GIT
# ============================================================

$gitBranch =
    (
        git branch --show-current
    ).Trim()

$gitCommit =
    (
        git rev-parse --short HEAD
    ).Trim()


# ============================================================
# ENVIRONMENT.PROPERTIES
# ============================================================

$environmentLines = @(
    "Framework=Financial Validation Platform"
    "Environment=$EnvironmentName"
    "WS_URL=$($properties['WS_URL'])"
    "DB_IP=$($properties['DB_IP'])"
    "DB_PORT=$($properties['DB_PORT'])"
    "DB_NAME=$($properties['DB_NAME'])"
    "Git_Branch=$gitBranch"
    "Git_Commit=$gitCommit"
    "Execution=Local Maven"
)

$environmentPath =
    Join-Path `
        $resultsDir `
        "environment.properties"

[System.IO.File]::WriteAllLines(
    $environmentPath,
    $environmentLines,
    $utf8
)


# ============================================================
# CATEGORIES.JSON
# ============================================================

$categories = @(

    [ordered]@{
        name =
            "ENVIRONMENT UNAVAILABLE"

        matchedStatuses =
            @(
                "skipped"
            )

        messageRegex =
            ".*ENVIRONMENT UNAVAILABLE.*"
    },

    [ordered]@{
        name =
            "SAFETY BLOCKED"

        matchedStatuses =
            @(
                "broken",
                "failed"
            )

        messageRegex =
            ".*permitirOperacoesComEfeito.*"
    },

    [ordered]@{
        name =
            "PROCESSING PENDING / WAITING FOR BATCH"

        matchedStatuses =
            @(
                "failed"
            )

        messageRegex =
            ".*(Payment must be processed before AFTER validation.*|Payment is waiting for batch processing.*)"
    },


    [ordered]@{
        name =
            "TECHNICAL / INFRASTRUCTURE FAILURE"

        matchedStatuses =
            @(
                "broken"
            )
    }
)

$categoriesJson =
    $categories |
    ConvertTo-Json `
        -Depth 5

$categoriesPath =
    Join-Path `
        $resultsDir `
        "categories.json"

[System.IO.File]::WriteAllText(
    $categoriesPath,
    $categoriesJson,
    $utf8
)


# ============================================================
# RESUMO
# ============================================================

Write-Host ""
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host " ALLURE EXECUTIVE METADATA" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan

Write-Host "Environment.....: $EnvironmentName"
Write-Host "WS..............: $($properties['WS_URL'])"
Write-Host "DB..............: $($properties['DB_IP'])"
Write-Host "Branch..........: $gitBranch"
Write-Host "Commit..........: $gitCommit"

Write-Host ""
Write-Host "Criado:" -ForegroundColor Green
Write-Host " - $environmentPath"
Write-Host " - $categoriesPath"

Write-Host ""
Write-Host "Categorias:" -ForegroundColor Yellow
Write-Host " - ENVIRONMENT UNAVAILABLE"
Write-Host " - SAFETY BLOCKED"
Write-Host " - BUSINESS VALIDATION FAILURE"
Write-Host " - TECHNICAL / INFRASTRUCTURE FAILURE"

Write-Host ""
Write-Host "Allure metadata preparado." -ForegroundColor Green