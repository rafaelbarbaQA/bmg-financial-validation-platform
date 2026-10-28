$ErrorActionPreference = "Stop"

$projectRoot =
    Split-Path `
        -Parent `
        (Split-Path -Parent $PSScriptRoot)

Set-Location $projectRoot

$resultsDir =
    Join-Path `
        $projectRoot `
        "target\allure-executive-results"

$utf8 =
    New-Object System.Text.UTF8Encoding($false)


function Get-ParameterMap {

    param($Json)

    $map = @{}

    foreach ($parameter in @($Json.parameters)) {

        if ($null -ne $parameter.name) {
            $map[[string]$parameter.name] =
                [string]$parameter.value
        }
    }

    return $map
}


# ============================================================
# LOCALIZAR VALIDACAO POS-BATCH
# ============================================================

$result =
    Get-ChildItem `
        $resultsDir `
        -Filter "*-result.json" `
        -File |
    ForEach-Object {

        try {

            $json =
                Get-Content $_.FullName -Raw |
                ConvertFrom-Json

            $testClass =
                @(
                    $json.labels |
                    Where-Object {
                        $_.name -eq "testClass"
                    } |
                    Select-Object -First 1
                ).value

            if (
                $testClass -match "ValidateFinancialSnapshotAfterIT"
            ) {

                [PSCustomObject]@{
                    File = $_
                    Json = $json
                }
            }
        }
        catch {
        }
    } |
    Sort-Object {
        $_.File.LastWriteTime
    } -Descending |
    Select-Object -First 1


if (-not $result) {
    throw "Resultado ValidateFinancialSnapshotAfterIT nao encontrado."
}


$json =
    $result.Json

$params =
    Get-ParameterMap $json


# ============================================================
# PROPRIEDADES DO SUREFIRE
# ============================================================

$surefirePath =
    Join-Path `
        $projectRoot `
        "target\surefire-reports\TEST-com.automation.integration.financial.ValidateFinancialSnapshotAfterIT.xml"

$surefireContent = ""

if (Test-Path $surefirePath) {

    $surefireContent =
        [System.IO.File]::ReadAllText(
            $surefirePath
        )
}


function Get-SurefireProperty {

    param(
        [string]$Name,
        [string]$Default = "NAO INFORMADO"
    )

    if ([string]::IsNullOrWhiteSpace($surefireContent)) {
        return $Default
    }

    $pattern =
        '<property\s+name="' `
        + [regex]::Escape($Name) `
        + '"\s+value="([^"]*)"'

    $match =
        [regex]::Match(
            $surefireContent,
            $pattern
        )

    if ($match.Success) {
        return $match.Groups[1].Value
    }

    return $Default
}


$paymentEventId =
    Get-SurefireProperty `
        "payment.event.id"

$paymentAmount =
    Get-SurefireProperty `
        "payment.amount"

$account =
    Get-SurefireProperty `
        "validation.account.id"


# ============================================================
# FALLBACK DA CONTA PELO SNAPSHOT
# ============================================================

if ($account -eq "NAO INFORMADO") {

    $snapshot =
        Get-ChildItem `
            (Join-Path $projectRoot "target\financial-snapshots") `
            -Filter "*-before.properties" `
            -File `
            -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if ($snapshot) {

        if (
            $snapshot.BaseName `
                -match '^(\d+)-before$'
        ) {
            $account =
                $Matches[1]
        }
    }
}


# ============================================================
# STATUS ESPERADO / ENCONTRADO
# ============================================================

$message = ""

if ($json.statusDetails) {
    $message =
        [string]$json.statusDetails.message
}

$expectedStatus = "2"
$actualStatus   = "NAO INFORMADO"

$statusMatch =
    [regex]::Match(
        $message,
        'expected\s*\[(\d+)\].*?(?:found|but found)\s*\[(\d+)\]',
        [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
    )

if ($statusMatch.Success) {

    $expectedStatus =
        $statusMatch.Groups[1].Value

    $actualStatus =
        $statusMatch.Groups[2].Value
}


# Fallback na evidencia PRE-BATCH
if ($actualStatus -eq "NAO INFORMADO") {

    $captureReport =
        Join-Path `
            $projectRoot `
            "target\surefire-reports\TEST-com.automation.integration.financial.CapturePreBatchFinancialSnapshotIT.xml"

    if (Test-Path $captureReport) {

        $captureContent =
            [System.IO.File]::ReadAllText(
                $captureReport
            )

        if (
            $captureContent -match
            'Status pagamento\.*:\s*(\d+)'
        ) {
            $actualStatus =
                $Matches[1]
        }
    }
}


# ============================================================
# CLASSIFICACAO
# ============================================================

if (
    $actualStatus -ne "NAO INFORMADO" `
    -and $actualStatus -ne $expectedStatus
) {

    $classification =
        "PROCESSING PENDING / WAITING FOR BATCH"

    $functionalResult =
        "NAO AVALIADO"

    $postBatchResult =
        "NAO CONCLUIDA"

}
else {

    $classification =
        "VALIDACAO POS-BATCH"

    $functionalResult =
        $json.status.ToUpper()

    $postBatchResult =
        $json.status.ToUpper()
}


# ============================================================
# RESUMO
# ============================================================

$summary = @"
FINANCIAL VALIDATION PLATFORM
========================================
RESUMO EXECUTIVO - POS-BATCH
========================================

Conta.....................: $account
IdEventoPagamento.........: $paymentEventId
Valor do pagamento........: $paymentAmount

Status esperado...........: $expectedStatus
Status encontrado.........: $actualStatus

Processamento batch.......: PENDENTE
Validacao pos-batch.......: $postBatchResult
Resultado funcional.......: $functionalResult

Classificacao.............: $classification

Observacao:
A validacao financeira posterior ao batch somente pode ser
concluida depois que o evento de pagamento atingir o status
de processamento esperado.
"@


$attachmentName =
    "$($json.uuid)-executive-post-batch-summary.txt"

$attachmentPath =
    Join-Path `
        $resultsDir `
        $attachmentName

[System.IO.File]::WriteAllText(
    $attachmentPath,
    $summary,
    $utf8
)


$current =
    @(
        $json.attachments |
        Where-Object {
            $_.name -ne "Resumo executivo - Pos-batch"
        }
    )

$current +=
    [PSCustomObject]@{
        name   = "Resumo executivo - Pos-batch"
        source = $attachmentName
        type   = "text/plain"
    }

$json.attachments =
    @($current)


[System.IO.File]::WriteAllText(
    $result.File.FullName,
    (
        $json |
        ConvertTo-Json -Depth 50
    ),
    $utf8
)


Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " RESUMO EXECUTIVO - POS-BATCH" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

Write-Host "Conta...............: $account"
Write-Host "Evento..............: $paymentEventId"
Write-Host "Pagamento...........: $paymentAmount"
Write-Host "Status esperado.....: $expectedStatus"
Write-Host "Status encontrado...: $actualStatus"
Write-Host "Classificacao.......: $classification"
Write-Host ""
Write-Host "Nenhuma alteracao financeira foi executada." -ForegroundColor Green