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


function Get-AllAttachments {

    param($Steps)

    $result =
        New-Object System.Collections.Generic.List[object]

    foreach ($step in @($Steps)) {

        foreach ($attachment in @($step.attachments)) {
            $result.Add($attachment)
        }

        if ($step.steps) {

            foreach ($child in @(Get-AllAttachments $step.steps)) {
                $result.Add($child)
            }
        }
    }

    return $result
}


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

            $params =
                Get-ParameterMap $json

            if (
                $testClass -match "FluxoQuitacaoAntecipadaTest" `
                -and $json.status -eq "skipped" `
                -and $params["environmentStatus"] -eq "UNAVAILABLE"
            ) {

                [PSCustomObject]@{
                    File   = $_
                    Json   = $json
                    Params = $params
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
    throw "Resultado de quitacao UNAVAILABLE nao encontrado."
}


$json =
    $result.Json

$params =
    $result.Params


$environment =
    if ($params.ContainsKey("ambiente")) {
        $params["ambiente"]
    }
    else {
        "NAO INFORMADO"
    }

$account =
    if ($params.ContainsKey("idConta")) {
        $params["idConta"]
    }
    else {
        "NAO INFORMADO"
    }

$endpoint =
    if ($params.ContainsKey("soapEndpoint")) {
        $params["soapEndpoint"]
    }
    else {
        "NAO INFORMADO"
    }


# ============================================================
# RECUPERAR DETALHES DO PREFLIGHT EXISTENTE
# ============================================================

$resolvedAddress = "NAO INFORMADO"
$port            = "NAO INFORMADO"
$detail          = "NAO INFORMADO"

$attachments =
    @()

$attachments +=
    @($json.attachments)

$attachments +=
    @(Get-AllAttachments $json.steps)

$preflight =
    $attachments |
    Where-Object {
        $_.name -eq "Environment preflight"
    } |
    Select-Object -First 1


if ($preflight) {

    $preflightPath =
        Join-Path `
            $resultsDir `
            $preflight.source

    if (Test-Path $preflightPath) {

        $text =
            [System.IO.File]::ReadAllText(
                $preflightPath
            )

        $match =
            [regex]::Match(
                $text,
                "(?m)^ResolvedAddress:\s*(.+)$"
            )

        if ($match.Success) {
            $resolvedAddress =
                $match.Groups[1].Value.Trim()
        }

        $match =
            [regex]::Match(
                $text,
                "(?m)^Port:\s*(.+)$"
            )

        if ($match.Success) {
            $port =
                $match.Groups[1].Value.Trim()
        }

        $match =
            [regex]::Match(
                $text,
                "(?m)^Detail:\s*(.+)$"
            )

        if ($match.Success) {
            $detail =
                $match.Groups[1].Value.Trim()
        }
    }
}


# ============================================================
# CRIAR RESUMO
# ============================================================

$summary = @"
FINANCIAL VALIDATION PLATFORM
========================================
RESUMO EXECUTIVO - QUITACAO ANTECIPADA
========================================

Ambiente..................: $environment
Conta.....................: $account

Preflight.................: UNAVAILABLE
Endpoint..................: $endpoint
IP resolvido..............: $resolvedAddress
Porta.....................: $port
Motivo....................: $detail

Simulacao SOAP............: NAO EXECUTADA
Geracao do boleto.........: NAO EXECUTADA
Persistencia SQL..........: NAO EXECUTADA
Validacao SOAP x SQL......: NAO EXECUTADA

Resultado funcional.......: NAO AVALIADO
Classificacao.............: ENVIRONMENT UNAVAILABLE
"@


$attachmentName =
    "$($json.uuid)-executive-settlement-summary.txt"

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
            $_.name -ne "Resumo executivo - Quitacao antecipada"
        }
    )

$current +=
    [PSCustomObject]@{
        name =
            "Resumo executivo - Quitacao antecipada"

        source =
            $attachmentName

        type =
            "text/plain"
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
Write-Host " RESUMO EXECUTIVO - QUITACAO" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

Write-Host "Ambiente........: $environment"
Write-Host "Conta...........: $account"
Write-Host "Preflight.......: UNAVAILABLE"
Write-Host "Endpoint........: $endpoint"
Write-Host "IP..............: $resolvedAddress"
Write-Host "Porta...........: $port"
Write-Host "Motivo..........: $detail"

Write-Host ""
Write-Host "Nenhum SOAP ou operacao financeira foi executado." -ForegroundColor Green