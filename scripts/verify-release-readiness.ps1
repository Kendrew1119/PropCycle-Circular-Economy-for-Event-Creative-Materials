[CmdletBinding()]
param(
    [switch]$SkipBuild,
    [switch]$SkipRules
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$failures = [System.Collections.Generic.List[string]]::new()
$pending = [System.Collections.Generic.List[string]]::new()

function Write-Pass {
    param([string]$Message)
    Write-Host "PASS    $Message" -ForegroundColor Green
}

function Write-Pending {
    param([string]$Message)
    $pending.Add($Message)
    Write-Host "PENDING $Message" -ForegroundColor Yellow
}

function Write-Failure {
    param([string]$Message)
    $failures.Add($Message)
    Write-Host "FAIL    $Message" -ForegroundColor Red
}

function Invoke-CheckedCommand {
    param(
        [string]$Label,
        [scriptblock]$Command
    )

    Write-Host "`nRunning: $Label" -ForegroundColor Cyan
    & $Command
    if ($LASTEXITCODE -ne 0) {
        Write-Failure "$Label exited with code $LASTEXITCODE."
        return $false
    }

    Write-Pass $Label
    return $true
}

function Test-IgnoredLocalFile {
    param(
        [string]$RelativePath,
        [bool]$RequiredForLiveUse
    )

    $fullPath = Join-Path $repoRoot $RelativePath
    if (-not (Test-Path -LiteralPath $fullPath)) {
        if ($RequiredForLiveUse) {
            Write-Pending "$RelativePath is absent. The project can build, but its related live service cannot be tested."
        } else {
            Write-Pass "$RelativePath is not required on this machine."
        }
        return
    }

    & git -C $repoRoot check-ignore -q -- $RelativePath
    if ($LASTEXITCODE -eq 0) {
        Write-Pass "$RelativePath exists locally and is ignored by Git."
    } else {
        Write-Failure "$RelativePath exists but is not ignored by Git."
    }
}

Push-Location $repoRoot
try {
    Write-Host 'PropCycle Phase 2F/2G release-readiness preflight' -ForegroundColor Cyan
    Write-Host 'This script never deploys Firebase, creates a signing key, or starts an Android emulator.'

    & git rev-parse --is-inside-work-tree *> $null
    if ($LASTEXITCODE -ne 0) {
        throw 'Run this script from a Git clone of PropCycle.'
    }
    Write-Pass 'Repository root is valid.'

    $trackedFiles = @(& git ls-files)
    $forbiddenPathPattern = '(^|/)(google-services\.json|secrets\.properties|local\.properties|keystore\.properties|\.firebaserc|\.env(?:\..*)?|[^/]+\.(?:jks|keystore))$'
    $trackedSecrets = @($trackedFiles | Where-Object { $_ -match $forbiddenPathPattern })
    if ($trackedSecrets.Count -eq 0) {
        Write-Pass 'No forbidden local configuration or signing files are tracked.'
    } else {
        Write-Failure ("Forbidden tracked files: " + ($trackedSecrets -join ', '))
    }

    $credentialMatches = @(& git grep -I -n -E 'AIza[0-9A-Za-z_-]{30,}|-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----' -- . 2>$null)
    if ($LASTEXITCODE -gt 1) {
        Write-Failure 'The tracked-file credential scan could not run.'
    } elseif ($credentialMatches.Count -eq 0) {
        Write-Pass 'No Google API-key or private-key shape was found in tracked files.'
    } else {
        Write-Failure ("Possible credentials found in tracked files:`n" + ($credentialMatches -join "`n"))
    }

    Test-IgnoredLocalFile -RelativePath 'app/google-services.json' -RequiredForLiveUse $true
    Test-IgnoredLocalFile -RelativePath 'secrets.properties' -RequiredForLiveUse $true
    Test-IgnoredLocalFile -RelativePath 'local.properties' -RequiredForLiveUse $false

    $legacyFiles = @($trackedFiles | Where-Object {
        $_ -match '\.(kt|kts|tsx|ts)$' -or
        $_ -match '(^|/)(app\.json|expo-env\.d\.ts|yarn\.lock)$'
    })
    if ($legacyFiles.Count -eq 0) {
        Write-Pass 'No team-authored Kotlin, React Native, Expo, or TypeScript source is tracked.'
    } else {
        Write-Failure ("Unexpected Kotlin or legacy mobile files: " + ($legacyFiles -join ', '))
    }

    $resourceFiles = @(Get-ChildItem -LiteralPath (Join-Path $repoRoot 'app/src/main/res') -Filter '*.xml' -File -Recurse)
    $invalidXml = [System.Collections.Generic.List[string]]::new()
    foreach ($resourceFile in $resourceFiles) {
        try {
            [void][xml](Get-Content -LiteralPath $resourceFile.FullName -Raw)
        } catch {
            $invalidXml.Add($resourceFile.FullName.Substring($repoRoot.Length + 1))
        }
    }
    if ($invalidXml.Count -eq 0) {
        Write-Pass "$($resourceFiles.Count) Android resource XML files parse successfully."
    } else {
        Write-Failure ("Invalid resource XML: " + ($invalidXml -join ', '))
    }

    $navigationPath = Join-Path $repoRoot 'app/src/main/res/navigation/nav_graph.xml'
    [xml]$navigationXml = Get-Content -LiteralPath $navigationPath -Raw
    $destinationCount = @($navigationXml.navigation.fragment).Count
    if ($destinationCount -eq 20) {
        Write-Pass 'All 20 proposal navigation destinations remain present.'
    } else {
        Write-Failure "Expected 20 navigation destinations but found $destinationCount."
    }

    $javaCandidates = @(
        @(
            $env:JAVA_HOME,
            'C:\Program Files\Android\Android Studio\jbr',
            'C:\Program Files\Android\Android Studio\jre'
        ) | Where-Object { $_ -and (Test-Path -LiteralPath (Join-Path $_ 'bin/java.exe')) }
    )

    if ($javaCandidates.Count -gt 0) {
        $env:JAVA_HOME = $javaCandidates[0]
        $env:Path = "$($env:JAVA_HOME)\bin;$env:Path"
        Write-Pass "Java runtime selected from $($env:JAVA_HOME)."
    } elseif (Get-Command java -ErrorAction SilentlyContinue) {
        Write-Pass 'Java runtime found on PATH.'
    } else {
        Write-Failure 'Java was not found. Install Android Studio or set JAVA_HOME.'
    }

    if (-not $SkipBuild -and $failures.Count -eq 0) {
        [void](Invoke-CheckedCommand -Label 'JVM tests, debug/release builds, and Android lint' -Command {
            & .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:lintDebug --stacktrace
        })
    } elseif ($SkipBuild) {
        Write-Pending 'Gradle verification was skipped by request.'
    } else {
        Write-Pending 'Gradle verification was skipped because an earlier required check failed.'
    }

    $rulesModules = Join-Path $repoRoot 'firebase-tests/node_modules'
    if ($SkipRules) {
        Write-Pending 'Firebase Emulator Security Rules tests were skipped by request.'
    } elseif (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
        Write-Pending 'npm is unavailable, so Firebase Emulator Security Rules tests were not run.'
    } elseif (-not (Test-Path -LiteralPath $rulesModules)) {
        Write-Pending 'firebase-tests dependencies are not installed. Run npm ci inside firebase-tests, then rerun this preflight.'
    } elseif ($failures.Count -eq 0) {
        Push-Location (Join-Path $repoRoot 'firebase-tests')
        try {
            [void](Invoke-CheckedCommand -Label 'Firebase Emulator Security Rules tests' -Command {
                & npm run test:rules
            })
        } finally {
            Pop-Location
        }
    } else {
        Write-Pending 'Firebase Emulator Security Rules tests were skipped because an earlier required check failed.'
    }

    Write-Host "`nSummary" -ForegroundColor Cyan
    Write-Host "Required failures: $($failures.Count)"
    Write-Host "Owner/setup items still pending: $($pending.Count)"
    foreach ($item in $pending) {
        Write-Host "- $item"
    }

    if ($failures.Count -gt 0) {
        Write-Host 'Release-readiness preflight failed. Fix every FAIL item before sharing a candidate.' -ForegroundColor Red
        exit 1
    }

    Write-Host 'Local preflight passed. PENDING items still require setup or live evidence; this is not production approval.' -ForegroundColor Green
    exit 0
} finally {
    Pop-Location
}
