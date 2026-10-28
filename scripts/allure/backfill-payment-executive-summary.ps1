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

if (-not (Test-Path $resultsDir)) {
    throw "Diretorio executivo nao encontrado: $resultsDir"
}


# ============================================================
# AUXILIARES
# ============================================================

function Get-ParameterMap {

    param(
        $Json
    )

    $map = @{}

    foreach ($parameter in @($Json.parameters)) {

        if (
            $null -ne $parameter.name `
            -and $null -ne $parameter.value
        ) {

            $map[[string]$parameter.name] =
                [string]$parameter.value
        }
    }

    return $map
}


function Get-FirstValue {

    param(
        [hashtable]$Map,
        [string[]]$Keys,
        [string]$Default = "NAO INFORMADO"
    )

    foreach ($key in $Keys) {

        if (
            $Map.ContainsKey($key) `
            -and -not [string]::IsNullOrWhiteSpace(
                $Map[$key]
            )
        ) {
            return $Map[$key]
        }
    }

    return $Default
}


function Read-Properties {

    param(
        [string]$Path
    )

    $map = @{}

    if (-not (Test-Path $Path)) {
        return $map
    }

    Get-Content $Path |
    ForEach-Object {

        $line = $_.Trim()

        if (
            $line.Length -eq 0 `
            -or $line.StartsWith("#") `
            -or -not $line.Contains("=")
        ) {
            return
        }

        $parts =
            $line.Split(
                '=',
                2
            )

        $map[$parts[0].Trim()] =
            $parts[1].Trim()
    }

    return $map
}


# ============================================================
# LOCALIZAR EXECUCAO REAL
# ============================================================

$realResult =
    Get-ChildItem `
        $resultsDir `
        -Filter "*-result.json" `
        -File |
    ForEach-Object {

        try {

            $json =
                Get-Content `
                    $_.FullName `
                    -Raw |
                ConvertFrom-Json

            $testClass =
                @(
                    $json.labels |
                    Where-Object {
                        $_.name -eq "testClass"
                    } |
                    Select-Object -First 1
                ).value

            if (-not $testClass) {
                return
            }

            $simpleClass =
                ($testClass -split "\.")[-1]

            if ($simpleClass -ne "FinancialPlatformRunnerIT") {
                return
            }

            $parameters =
                Get-ParameterMap `
                    $json

            $mode =
                Get-FirstValue `
                    -Map $parameters `
                    -Keys @(
                        "modoExecucao",
                        "Modo de execucao"
                    )

            if (
                $json.status -eq "passed" `
                -and $mode -eq "REAL"
            ) {

                [PSCustomObject]@{
                    File       = $_
                    Json       = $json
                    Parameters = $parameters
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


if (-not $realResult) {
    throw "Execucao REAL aprovada nao encontrada no Allure executivo."
}


$json =
    $realResult.Json

$params =
    $realResult.Parameters


# ============================================================
# PARAMETROS DA EXECUCAO
# ============================================================

$account =
    Get-FirstValue `
        -Map $params `
        -Keys @(
            "idConta",
            "Conta"
        )

$scenario =
    Get-FirstValue `
        -Map $params `
        -Keys @(
            "cenario",
            "Cenario"
        )

$paymentAmount =
    Get-FirstValue `
        -Map $params `
        -Keys @(
            "valorPagamento"
        )

$paymentMode =
    Get-FirstValue `
        -Map $params `
        -Keys @(
            "modalidade"
        )

$ticketId =
    Get-FirstValue `
        -Map $params `
        -Keys @(
            "idBoleto",
            "IdBoleto"
        )

$nossoNumero =
    Get-FirstValue `
        -Map $params `
        -Keys @(
            "nossoNumero"
        )

$paymentEventId =
    Get-FirstValue `
        -Map $params `
        -Keys @(
            "idEventoPagamento"
        )

$paymentDate =
    Get-FirstValue `
        -Map $params `
        -Keys @(
            "dataPagamento",
            "Data pagamento"
        )

$processingStatus =
    Get-FirstValue `
        -Map $params `
        -Keys @(
            "Status inicial encontrado"
        )


# ============================================================
# FALLBACK: RECUPERAR STATUS DA EVIDENCIA SUREFIRE
# ============================================================

if ($processingStatus -eq "NAO INFORMADO") {

    $surefireReport =
        Join-Path `
            $projectRoot `
            "target\surefire-reports\TEST-com.automation.integration.financial.CapturePreBatchFinancialSnapshotIT.xml"

    if (Test-Path $surefireReport) {

        $surefireContent =
            [System.IO.File]::ReadAllText(
                $surefireReport
            )

        $eventPattern =
            "Evento pagamento\.*:\s*" `
            + [regex]::Escape(
                $paymentEventId
            )

        if (
            [regex]::IsMatch(
                $surefireContent,
                $eventPattern
            )
        ) {

            $statusMatch =
                [regex]::Match(
                    $surefireContent,
                    "Status pagamento\.*:\s*(\d+)"
                )

            if ($statusMatch.Success) {

                $processingStatus =
                    $statusMatch.Groups[1].Value
            }
        }
    }
}


# ============================================================
# SNAPSHOT BEFORE EXISTENTE
# ============================================================

$snapshotPath =
    Join-Path `
        $projectRoot `
        "target\financial-snapshots\$account-before.properties"

$snapshot =
    Read-Properties `
        $snapshotPath


$currentBalance =
    Get-FirstValue `
        -Map $snapshot `
        -Keys @(
            "currentBalance",
            "saldoAtual",
            "saldoAtualFinal"
        )

$invoiceAmount =
    Get-FirstValue `
        -Map $snapshot `
        -Keys @(
            "invoiceAmount",
            "saldoFatura",
            "valorFatura"
        )

$minimumAmount =
    Get-FirstValue `
        -Map $snapshot `
        -Keys @(
            "minimumAmount",
            "valorMinimo",
            "valorMinimoExtrato"
        )


# ============================================================
# RESUMO EXECUTIVO
# ============================================================

$summary = @"
FINANCIAL VALIDATION PLATFORM
========================================
RESUMO EXECUTIVO - PAGAMENTO
========================================

Resultado................: PASS
Modo.....................: REAL

Conta....................: $account
Cenario..................: $scenario
Modalidade...............: $paymentMode

Saldo atual..............: $currentBalance
Saldo da fatura..........: $invoiceAmount
Valor minimo.............: $minimumAmount
Valor do pagamento.......: $paymentAmount

IdBoleto.................: $ticketId
NossoNumero..............: $nossoNumero
IdEventoPagamento........: $paymentEventId
Data pagamento...........: $paymentDate

Persistencia SQL.........: VALIDADA
Status processamento.....: $processingStatus
Proxima etapa............: PROCESSAMENTO BATCH / VALIDACAO POS-BATCH
"@


# ============================================================
# CRIAR ATTACHMENT
# ============================================================

$attachmentName =
    "$($json.uuid)-executive-payment-summary.txt"

$attachmentPath =
    Join-Path `
        $resultsDir `
        $attachmentName

[System.IO.File]::WriteAllText(
    $attachmentPath,
    $summary,
    (New-Object System.Text.UTF8Encoding($false))
)


# ============================================================
# REGISTRAR ATTACHMENT NO RESULTADO ALLURE
# ============================================================

$attachments =
    @(
        $json.attachments |
        Where-Object {
            $_.name -ne "Resumo executivo - Pagamento"
        }
    )

$attachments +=
    [PSCustomObject]@{
        name   = "Resumo executivo - Pagamento"
        source = $attachmentName
        type   = "text/plain"
    }

$json.attachments =
    @(
        $attachments
    )


$normalizedJson =
    $json |
    ConvertTo-Json `
        -Depth 50

[System.IO.File]::WriteAllText(
    $realResult.File.FullName,
    $normalizedJson,
    (New-Object System.Text.UTF8Encoding($false))
)


# ============================================================
# RESULTADO
# ============================================================

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " RESUMO EXECUTIVO ADICIONADO" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

Write-Host "Conta................: $account"
Write-Host "Cenario..............: $scenario"
Write-Host "Valor................: $paymentAmount"
Write-Host "IdBoleto.............: $ticketId"
Write-Host "NossoNumero..........: $nossoNumero"
Write-Host "IdEventoPagamento....: $paymentEventId"
Write-Host "Status...............: $processingStatus"
Write-Host ""
Write-Host "Arquivo..............: $attachmentName"
Write-Host ""
Write-Host "Nenhuma operacao financeira foi executada." -ForegroundColor Green