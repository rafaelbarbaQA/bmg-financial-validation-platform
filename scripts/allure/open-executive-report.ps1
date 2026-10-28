param(
    [string]$EnvironmentName = "141",
    [string]$ReportVersion = "2.39.0",
    [switch]$NoServe
)

$ErrorActionPreference = "Stop"

$projectRoot =
    Split-Path `
        -Parent `
        (Split-Path -Parent $PSScriptRoot)

Set-Location $projectRoot

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " FINANCIAL VALIDATION PLATFORM" -ForegroundColor Cyan
Write-Host " EXECUTIVE REPORT PIPELINE" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan


# ============================================================
# 1. METADATA
# ============================================================

Write-Host ""
Write-Host "[1/5] Preparando metadata..." -ForegroundColor Yellow

$prepare =
    Join-Path `
        $PSScriptRoot `
        "prepare-allure-report.ps1"

if (-not (Test-Path $prepare)) {
    throw "Script nao encontrado: $prepare"
}

& $prepare `
    -EnvironmentName $EnvironmentName


# ============================================================
# 2. RESULTADOS EXECUTIVOS
# ============================================================

Write-Host ""
Write-Host "[2/5] Selecionando evidencias executivas..." -ForegroundColor Yellow

$createExecutive =
    Join-Path `
        $PSScriptRoot `
        "create-executive-results.ps1"

if (-not (Test-Path $createExecutive)) {
    throw "Script nao encontrado: $createExecutive"
}

& $createExecutive


# ============================================================
# 3. BACKFILLS
# ============================================================

Write-Host ""
Write-Host "[3/5] Preparando resumos executivos..." -ForegroundColor Yellow

$paymentBackfill =
    Join-Path `
        $PSScriptRoot `
        "backfill-payment-executive-summary.ps1"

if (Test-Path $paymentBackfill) {
    & $paymentBackfill
}
else {
    Write-Host "Backfill pagamento nao encontrado." -ForegroundColor DarkYellow
}


$postBatchBackfill =
    Join-Path `
        $PSScriptRoot `
        "backfill-post-batch-executive-summary.ps1"

if (Test-Path $postBatchBackfill) {
    & $postBatchBackfill
}
else {
    Write-Host "Backfill pos-batch nao encontrado." -ForegroundColor DarkYellow
}

$quitacaoBackfill =
    Join-Path `
        $PSScriptRoot `
        "backfill-quitacao-executive-summary.ps1"

if (Test-Path $quitacaoBackfill) {
    & $quitacaoBackfill
}
else {
    Write-Host "Backfill quitacao nao encontrado." -ForegroundColor DarkYellow
}


# ============================================================
# 4. VALIDACAO
# ============================================================

Write-Host ""
Write-Host "[4/5] Validando integridade do relatorio..." -ForegroundColor Yellow

$validator =
    Join-Path `
        $PSScriptRoot `
        "validate-executive-report.ps1"

if (-not (Test-Path $validator)) {
    throw "Validador executivo nao encontrado: $validator"
}

& $validator

$resultsDir =
    Join-Path `
        $projectRoot `
        "target\allure-executive-results"

$resultCount =
    @(
        Get-ChildItem `
            $resultsDir `
            -Filter "*-result.json" `
            -File `
            -ErrorAction SilentlyContinue
    ).Count

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host " EXECUTIVE REPORT READY" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green

Write-Host "Environment..............: $EnvironmentName"
Write-Host "Results..................: $resultCount"
Write-Host "Directory................: $resultsDir"

Write-Host ""
Write-Host "Nenhum teste foi executado." -ForegroundColor Green
Write-Host "Nenhuma operacao financeira foi executada." -ForegroundColor Green
Write-Host "Nenhum SOAP foi executado." -ForegroundColor Green


if ($NoServe) {

    Write-Host ""
    Write-Host "NoServe ativo - navegador nao aberto." -ForegroundColor Yellow
    return
}


# ============================================================
# ABRIR ALLURE
# ============================================================

Write-Host ""
Write-Host "Abrindo Allure executivo..." -ForegroundColor Cyan

mvn `
    "-Dreport.version=$ReportVersion" `
    "-Dallure.results.directory=allure-executive-results" `
    io.qameta.allure:allure-maven:3.0.2:serve