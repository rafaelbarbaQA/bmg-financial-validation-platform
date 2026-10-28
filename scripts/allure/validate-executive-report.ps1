$ErrorActionPreference = "Stop"

$projectRoot =
    Split-Path `
        -Parent `
        (Split-Path -Parent $PSScriptRoot)

Set-Location -LiteralPath $projectRoot
[Environment]::CurrentDirectory = $projectRoot

$resultsDir =
    Join-Path `
        $projectRoot `
        "target\allure-executive-results"

if (-not (Test-Path -LiteralPath $resultsDir)) {
    throw "Diretorio executivo nao encontrado: $resultsDir"
}


function Get-TestClass {

    param($Json)

    $label =
        @(
            $Json.labels |
            Where-Object {
                $_.name -eq "testClass"
            } |
            Select-Object -First 1
        )

    if ($label.Count -eq 0) {
        return ""
    }

    return [string]$label[0].value
}


function Get-ParameterMap {

    param($Json)

    $map = @{}

    foreach ($parameter in @($Json.parameters)) {

        if (
            $null -ne $parameter `
            -and $null -ne $parameter.name
        ) {

            $map[[string]$parameter.name] =
                [string]$parameter.value
        }
    }

    return $map
}


function Get-StepAttachments {

    param($Steps)

    foreach ($step in @($Steps)) {

        if ($null -eq $step) {
            continue
        }

        foreach ($attachment in @($step.attachments)) {

            if ($null -ne $attachment) {
                Write-Output $attachment
            }
        }

        if ($step.steps) {

            Get-StepAttachments `
                -Steps $step.steps
        }
    }
}


function Get-AllAttachments {

    param($Json)

    foreach ($attachment in @($Json.attachments)) {

        if ($null -ne $attachment) {
            Write-Output $attachment
        }
    }

    Get-StepAttachments `
        -Steps $Json.steps
}


function Assert-Condition {

    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}


# ============================================================
# CARREGAR RESULTADOS
# ============================================================

$results = @(
    Get-ChildItem `
        -LiteralPath $resultsDir `
        -Filter "*-result.json" `
        -File |
    ForEach-Object {

        $json =
            Get-Content `
                -LiteralPath $_.FullName `
                -Raw |
            ConvertFrom-Json

        [PSCustomObject]@{
            File       = $_
            Json       = $json
            TestClass  = Get-TestClass $json
            Parameters = Get-ParameterMap $json
        }
    }
)


Write-Host ""
Write-Host "Resultados encontrados....: $($results.Count)" -ForegroundColor Cyan


# ============================================================
# TOTAL
# ============================================================

Assert-Condition `
    -Condition ($results.Count -eq 6) `
    -Message "Esperados 6 resultados executivos. Encontrados: $($results.Count)"


# ============================================================
# PAGAMENTO REAL
# ============================================================

$payment =
    $results |
    Where-Object {

        $_.TestClass -match "FinancialPlatformRunnerIT" `
        -and $_.Json.status -eq "passed" `
        -and (
            $_.Parameters["modoExecucao"] -eq "REAL" `
            -or
            $_.Parameters["Modo de execucao"] -eq "REAL"
        )
    } |
    Select-Object -First 1

Assert-Condition `
    -Condition ($null -ne $payment) `
    -Message "Execucao financeira REAL aprovada nao encontrada."

$paymentAttachments =
    @(
        Get-AllAttachments `
            -Json $payment.Json
    )

Assert-Condition `
    -Condition (
        $paymentAttachments.name `
        -contains "Resumo executivo - Pagamento"
    ) `
    -Message "Resumo executivo de pagamento nao encontrado."


# ============================================================
# PERSISTENCIA SQL
# ============================================================

$paymentDetails =
    $results |
    Where-Object {

        $_.TestClass -match "PaymentEventDetailsDatabaseIT" `
        -and
        $_.Json.status -eq "passed"
    } |
    Select-Object -First 1

Assert-Condition `
    -Condition ($null -ne $paymentDetails) `
    -Message "Validacao de persistencia SQL nao encontrada."


# ============================================================
# STATUS PAGAMENTO
# ============================================================

$processing =
    $results |
    Where-Object {

        $_.TestClass -match "PaymentProcessingStateDatabaseIT" `
        -and
        $_.Json.status -eq "passed"
    } |
    Select-Object -First 1

Assert-Condition `
    -Condition ($null -ne $processing) `
    -Message "Validacao de status do pagamento nao encontrada."


# ============================================================
# PRE-BATCH
# ============================================================

$before =
    $results |
    Where-Object {

        $_.TestClass -match "CapturePreBatchFinancialSnapshotIT" `
        -and
        $_.Json.status -eq "passed"
    } |
    Select-Object -First 1

Assert-Condition `
    -Condition ($null -ne $before) `
    -Message "Snapshot pre-batch nao encontrado."


# ============================================================
# POS-BATCH
# ============================================================

$after =
    $results |
    Where-Object {

        $_.TestClass -match "ValidateFinancialSnapshotAfterIT"
    } |
    Select-Object -First 1

Assert-Condition `
    -Condition ($null -ne $after) `
    -Message "Validacao pos-batch nao encontrada."

$afterAttachments =
    @(
        Get-AllAttachments `
            -Json $after.Json
    )

Assert-Condition `
    -Condition (
        $afterAttachments.name `
        -contains "Resumo executivo - Pos-batch"
    ) `
    -Message "Resumo executivo pos-batch nao encontrado."


# ============================================================
# QUITACAO
# ============================================================

$settlement =
    $results |
    Where-Object {

        $_.TestClass -match "FluxoQuitacaoAntecipadaTest" `
        -and
        $_.Json.status -eq "skipped"
    } |
    Select-Object -First 1

Assert-Condition `
    -Condition ($null -ne $settlement) `
    -Message "Resultado de quitacao indisponivel nao encontrado."

$settlementAttachments =
    @(
        Get-AllAttachments `
            -Json $settlement.Json
    )

Assert-Condition `
    -Condition (
        $settlementAttachments.name `
        -contains "Resumo executivo - Quitacao antecipada"
    ) `
    -Message "Resumo executivo de quitacao nao encontrado."


# ============================================================
# METADATA
# ============================================================

$environmentFile =
    Join-Path `
        $resultsDir `
        "environment.properties"

$categoriesFile =
    Join-Path `
        $resultsDir `
        "categories.json"

Assert-Condition `
    -Condition (
        Test-Path -LiteralPath $environmentFile
    ) `
    -Message "environment.properties nao encontrado."

Assert-Condition `
    -Condition (
        Test-Path -LiteralPath $categoriesFile
    ) `
    -Message "categories.json nao encontrado."


# ============================================================
# INTEGRIDADE DOS ATTACHMENTS
# ============================================================

$missingAttachments = @()

foreach ($result in $results) {

    $attachments =
        @(
            Get-AllAttachments `
                -Json $result.Json
        )

    foreach ($attachment in $attachments) {

        if (
            $null -eq $attachment `
            -or
            [string]::IsNullOrWhiteSpace(
                [string]$attachment.source
            )
        ) {
            continue
        }

        $attachmentPath =
            Join-Path `
                $resultsDir `
                ([string]$attachment.source)

        if (
            -not (
                Test-Path `
                    -LiteralPath $attachmentPath
            )
        ) {

            $missingAttachments +=
                "$($result.Json.name) | $($attachment.name) | $($attachment.source)"
        }
    }
}


if ($missingAttachments.Count -gt 0) {

    Write-Host ""
    Write-Host "ATTACHMENTS AUSENTES:" -ForegroundColor Red

    foreach ($missing in $missingAttachments) {
        Write-Host $missing -ForegroundColor Red
    }

    throw "Foram encontrados attachments ausentes."
}


# ============================================================
# RESULTADO
# ============================================================

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host " EXECUTIVE REPORT VALIDATED" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green

Write-Host ""
Write-Host "[PASS] Execucao financeira REAL"
Write-Host "[PASS] Persistencia SQL"
Write-Host "[PASS] Status do pagamento"
Write-Host "[PASS] Snapshot pre-batch"
Write-Host "[PASS] Pos-batch classificado"
Write-Host "[PASS] Quitacao / environment unavailable"
Write-Host "[PASS] Attachments"
Write-Host "[PASS] Metadata Allure"

Write-Host ""
Write-Host "Resultados executivos....: $($results.Count)"
Write-Host "Integridade...............: OK"