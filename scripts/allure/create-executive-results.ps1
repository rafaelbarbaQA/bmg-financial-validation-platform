$ErrorActionPreference = "Stop"

$projectRoot =
    Split-Path `
        -Parent `
        (Split-Path -Parent $PSScriptRoot)

Set-Location $projectRoot

$sourceDir =
    Join-Path `
        $projectRoot `
        "target\allure-results"

$targetDir =
    Join-Path `
        $projectRoot `
        "target\allure-executive-results"

if (-not (Test-Path $sourceDir)) {
    throw "Diretorio Allure nao encontrado: $sourceDir"
}

# ============================================================
# TESTES QUE QUEREMOS NA VISAO EXECUTIVA
# ============================================================

$executiveClasses = @(
    "FinancialPlatformRunnerIT",
    "PaymentEventDetailsDatabaseIT",
    "PaymentProcessingStateDatabaseIT",
    "CapturePreBatchFinancialSnapshotIT",
    "ValidateFinancialSnapshotAfterIT",
    "FluxoQuitacaoAntecipadaTest"
)

# ============================================================
# LIMPAR SOMENTE O DIRETORIO EXECUTIVO
# O ALLURE COMPLETO NAO E ALTERADO
# ============================================================

if (Test-Path $targetDir) {
    Remove-Item `
        $targetDir `
        -Recurse `
        -Force
}

New-Item `
    -ItemType Directory `
    -Force `
    -Path $targetDir |
    Out-Null

# ============================================================
# FUNCAO PARA ENCONTRAR ATTACHMENTS REFERENCIADOS NO JSON
# ============================================================

function Get-AllSources {

    param(
        [Parameter(Mandatory = $true)]
        $Value
    )

    $sources = New-Object System.Collections.Generic.List[string]

    function Visit {

        param(
            $Node
        )

        if ($null -eq $Node) {
            return
        }

        if ($Node -is [string]) {
            return
        }

        if ($Node -is [System.Collections.IDictionary]) {

            foreach ($key in $Node.Keys) {

                if (
                    $key -eq "source" `
                    -and $Node[$key] -is [string]
                ) {
                    $sources.Add($Node[$key])
                }

                Visit $Node[$key]
            }

            return
        }

        if (
            $Node -is [System.Collections.IEnumerable] `
            -and -not ($Node -is [PSCustomObject])
        ) {

            foreach ($item in $Node) {
                Visit $item
            }

            return
        }

        foreach ($property in $Node.PSObject.Properties) {

            if (
                $property.Name -eq "source" `
                -and $property.Value -is [string]
            ) {
                $sources.Add($property.Value)
            }

            Visit $property.Value
        }
    }

    Visit $Value

    return $sources | Select-Object -Unique
}

# ============================================================
# IDENTIFICAR RESULTADOS EXECUTIVOS
# ============================================================

$selectedResults =
    Get-ChildItem `
        $sourceDir `
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

            $simpleClass =
                if ($testClass) {
                    ($testClass -split "\.")[-1]
                }
                else {
                    ""
                }

            if ($executiveClasses -contains $simpleClass) {

                [PSCustomObject]@{
                    File        = $_
                    Json        = $json
                    TestClass   = $simpleClass
                    TestName    = $json.name
                    Status      = $json.status
                    Uuid        = $json.uuid
                    Modified    = $_.LastWriteTime
                }
            }
        }
        catch {
            # Ignorar arquivo que nao seja result JSON valido.
        }
    }

if (-not $selectedResults) {

    throw "Nenhum resultado executivo foi encontrado."
}

# ============================================================
# REDUZIR PARA AS EVIDENCIAS EXECUTIVAS MAIS RELEVANTES
# ============================================================

function Test-ParameterValue {

    param(
        $Result,
        [string]$ExpectedValue
    )

    foreach ($parameter in @($Result.Json.parameters)) {

        if (
            [string]$parameter.value `
                -eq $ExpectedValue
        ) {
            return $true
        }
    }

    return $false
}

$executiveSelection =
    New-Object System.Collections.Generic.List[object]


# ------------------------------------------------------------
# 1. PAGAMENTO REAL
# ------------------------------------------------------------

$financialReal =
    $selectedResults |
    Where-Object {
        $_.TestClass -eq "FinancialPlatformRunnerIT" `
        -and $_.Status -eq "passed" `
        -and (
            Test-ParameterValue `
                $_ `
                "REAL"
        )
    } |
    Sort-Object Modified -Descending |
    Select-Object -First 1

if ($financialReal) {
    $executiveSelection.Add(
        $financialReal
    )
}


# ------------------------------------------------------------
# 2. PERSISTENCIA DO PAGAMENTO
# ------------------------------------------------------------

$paymentDatabase =
    $selectedResults |
    Where-Object {
        $_.TestClass -eq "PaymentEventDetailsDatabaseIT" `
        -and $_.Status -eq "passed"
    } |
    Sort-Object Modified -Descending |
    Select-Object -First 1

if ($paymentDatabase) {
    $executiveSelection.Add(
        $paymentDatabase
    )
}


# ------------------------------------------------------------
# 3. STATUS DO PAGAMENTO
# ------------------------------------------------------------

$paymentStatus =
    $selectedResults |
    Where-Object {
        $_.TestClass -eq "PaymentProcessingStateDatabaseIT" `
        -and $_.Status -eq "passed"
    } |
    Sort-Object Modified -Descending |
    Select-Object -First 1

if ($paymentStatus) {
    $executiveSelection.Add(
        $paymentStatus
    )
}


# ------------------------------------------------------------
# 4. BASELINE PRE-BATCH
# ------------------------------------------------------------

$preBatch =
    $selectedResults |
    Where-Object {
        $_.TestClass -eq "CapturePreBatchFinancialSnapshotIT" `
        -and $_.Status -eq "passed"
    } |
    Sort-Object Modified -Descending |
    Select-Object -First 1

if ($preBatch) {
    $executiveSelection.Add(
        $preBatch
    )
}


# ------------------------------------------------------------
# 5. POS-BATCH / AGUARDANDO PROCESSAMENTO
# ------------------------------------------------------------

$postBatch =
    $selectedResults |
    Where-Object {
        $_.TestClass -eq "ValidateFinancialSnapshotAfterIT"
    } |
    Sort-Object Modified -Descending |
    Select-Object -First 1

if ($postBatch) {
    $executiveSelection.Add(
        $postBatch
    )
}


# ------------------------------------------------------------
# 6. QUITACAO - ULTIMO PREFLIGHT
# ------------------------------------------------------------

$quitacao =
    $selectedResults |
    Where-Object {
        $_.TestClass -eq "FluxoQuitacaoAntecipadaTest" `
        -and $_.Status -eq "skipped" `
        -and (
            Test-ParameterValue `
                $_ `
                "UNAVAILABLE"
        )
    } |
    Sort-Object Modified -Descending |
    Select-Object -First 1

if (-not $quitacao) {

    $quitacao =
        $selectedResults |
        Where-Object {
            $_.TestClass -eq "FluxoQuitacaoAntecipadaTest"
        } |
        Sort-Object Modified -Descending |
        Select-Object -First 1
}

if ($quitacao) {
    $executiveSelection.Add(
        $quitacao
    )
}


$selectedResults =
    @(
        $executiveSelection |
        Sort-Object Modified
    )

Write-Host ""
Write-Host "Selecao executiva aplicada." -ForegroundColor Green
Write-Host "Resultados finais: $($selectedResults.Count)"

# ============================================================
# COPIAR RESULT + ATTACHMENTS
# ============================================================

$selectedUuids =
    @(
        $selectedResults |
        ForEach-Object {
            $_.Uuid
        }
    )

$copiedAttachments =
    New-Object System.Collections.Generic.HashSet[string]

foreach ($result in $selectedResults) {

    Copy-Item `
        $result.File.FullName `
        (Join-Path $targetDir $result.File.Name) `
        -Force

    $sources =
        Get-AllSources `
            $result.Json

    foreach ($source in $sources) {

        $sourcePath =
            Join-Path `
                $sourceDir `
                $source

        if (Test-Path $sourcePath) {

            Copy-Item `
                $sourcePath `
                (Join-Path $targetDir $source) `
                -Force

            [void] $copiedAttachments.Add(
                $source
            )
        }
    }
}

# ============================================================
# COPIAR CONTAINERS RELACIONADOS AOS TESTES
# ============================================================

$containersCopied = 0

Get-ChildItem `
    $sourceDir `
    -Filter "*-container.json" `
    -File |
ForEach-Object {

    try {

        $container =
            Get-Content `
                $_.FullName `
                -Raw |
            ConvertFrom-Json

        $children =
            @(
                $container.children
            )

        $belongsToExecutive =
            $false

        foreach ($child in $children) {

            if ($selectedUuids -contains $child) {
                $belongsToExecutive = $true
                break
            }
        }

        if ($belongsToExecutive) {

            Copy-Item `
                $_.FullName `
                (Join-Path $targetDir $_.Name) `
                -Force

            $containersCopied++

            $sources =
                Get-AllSources `
                    $container

            foreach ($source in $sources) {

                $sourcePath =
                    Join-Path `
                        $sourceDir `
                        $source

                if (Test-Path $sourcePath) {

                    Copy-Item `
                        $sourcePath `
                        (Join-Path $targetDir $source) `
                        -Force

                    [void] $copiedAttachments.Add(
                        $source
                    )
                }
            }
        }
    }
    catch {
        # Ignorar container invalido.
    }
}

# ============================================================
# COPIAR METADADOS DO ALLURE
# ============================================================

$metadataFiles = @(
    "environment.properties",
    "categories.json",
    "executor.json"
)

foreach ($metadata in $metadataFiles) {

    $source =
        Join-Path `
            $sourceDir `
            $metadata

    if (Test-Path $source) {

        Copy-Item `
            $source `
            (Join-Path $targetDir $metadata) `
            -Force
    }
}

# ============================================================
# NORMALIZAR BEHAVIORS EXECUTIVOS
# ============================================================

$behaviorMap = @{

    "FinancialPlatformRunnerIT" = @{
        Feature = "Pagamentos"
        Story   = "Execucao financeira"
        Name    = "Execucao financeira real"
    }

    "PaymentEventDetailsDatabaseIT" = @{
        Feature = "Pagamentos"
        Story   = "Persistencia SQL do pagamento"
        Name    = "Persistencia SQL do pagamento"
    }

    "PaymentProcessingStateDatabaseIT" = @{
        Feature = "Pagamentos"
        Story   = "Processamento do pagamento"
        Name    = "Status do processamento do pagamento"
    }

    "CapturePreBatchFinancialSnapshotIT" = @{
        Feature = "Ciclo financeiro"
        Story   = "Snapshot pre-batch"
        Name    = "Snapshot financeiro pre-batch"
    }

    "ValidateFinancialSnapshotAfterIT" = @{
        Feature = "Ciclo financeiro"
        Story   = "Validacao pos-batch"
        Name    = "Validacao financeira pos-batch"
    }

    "FluxoQuitacaoAntecipadaTest" = @{
        Feature = "Quitacao antecipada"
        Story   = "Fluxo integrado SOAP e banco de dados"
        Name    = "Quitacao antecipada - ambiente 141"
    }
}


Get-ChildItem `
    $targetDir `
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

        if (-not $behaviorMap.ContainsKey($simpleClass)) {
            return
        }

        $behavior =
            $behaviorMap[$simpleClass]

        $labels =
            @(
                $json.labels |
                Where-Object {
                    $_.name -notin @(
                        "epic",
                        "feature",
                        "story",
                        "suite",
                        "subSuite"
                    )
                }
            )

        $labels +=
            [PSCustomObject]@{
                name  = "epic"
                value = "Financial Validation Platform"
            }

        $labels +=
            [PSCustomObject]@{
                name  = "feature"
                value = $behavior.Feature
            }

        $labels +=
            [PSCustomObject]@{
                name  = "story"
                value = $behavior.Story
            }

        $labels +=
            [PSCustomObject]@{
                name  = "suite"
                value = "Financial Validation Platform"
            }

        $labels +=
            [PSCustomObject]@{
                name  = "subSuite"
                value = $behavior.Feature
            }

        $json.labels =
            @(
                $labels
            )

        $json.name =
            $behavior.Name

        $normalized =
            $json |
            ConvertTo-Json `
                -Depth 50

        [System.IO.File]::WriteAllText(
            $_.FullName,
            $normalized,
            (New-Object System.Text.UTF8Encoding($false))
        )
    }
    catch {

        Write-Host `
            "Falha ao normalizar $($_.Name): $($_.Exception.Message)" `
            -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "Behaviors executivos normalizados." -ForegroundColor Green

# ============================================================
# RESUMO
# ============================================================

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " FINANCIAL VALIDATION PLATFORM - EXECUTIVE RESULTS" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

$selectedResults |
    Sort-Object Modified |
    Select-Object `
        Status,
        TestClass,
        TestName,
        Modified |
    Format-Table -AutoSize

Write-Host ""
Write-Host "Resultados selecionados..: $($selectedResults.Count)"
Write-Host "Attachments preservados.: $($copiedAttachments.Count)"
Write-Host "Containers preservados...: $containersCopied"

Write-Host ""
Write-Host "Origem completa..........: $sourceDir"
Write-Host "Visao executiva..........: $targetDir"

Write-Host ""
Write-Host "ALLURE EXECUTIVO PRONTO." -ForegroundColor Green