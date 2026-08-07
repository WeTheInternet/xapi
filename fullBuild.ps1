[CmdletBinding()]
param(
    [Alias('s')][switch] $Shadow,
    [Alias('f')][switch] $Fast,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $GradleArguments
)

$ErrorActionPreference = 'Stop'
$XapiRoot = $PSScriptRoot

if (-not $GradleArguments -or $GradleArguments.Count -eq 0) {
    $GradleArguments = @('build', 'xapiPublish', 'testClasses', '-x', 'test', '-x', 'check', '-x', 'javadoc')
}

function Invoke-XapiGradle {
    param(
        [Parameter(Mandatory = $true)][string] $Directory,
        [Parameter(Mandatory = $true)][string[]] $Arguments,
        [string] $JavaHome
    )

    Push-Location $Directory
    $PreviousJavaHome = $env:JAVA_HOME
    $PreviousPath = $env:PATH
    try {
        if ($JavaHome) {
            $env:JAVA_HOME = $JavaHome
            $env:PATH = (Join-Path $JavaHome 'bin') + [IO.Path]::PathSeparator + $PreviousPath
        }
        Write-Host "Invoking $Directory\gradlew.bat $($Arguments -join ' ')"
        & (Join-Path $Directory 'gradlew.bat') @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle failed in $Directory with exit code $LASTEXITCODE"
        }
    }
    finally {
        $env:JAVA_HOME = $PreviousJavaHome
        $env:PATH = $PreviousPath
        Pop-Location
    }
}

function Test-Java8Home {
    param([string] $Candidate)

    if (-not $Candidate) {
        return $false
    }
    $Java = Join-Path $Candidate 'bin/java.exe'
    if (-not (Test-Path -LiteralPath $Java -PathType Leaf)) {
        return $false
    }
    $Release = Join-Path $Candidate 'release'
    if (Test-Path -LiteralPath $Release -PathType Leaf) {
        $ReleaseText = Get-Content -LiteralPath $Release -Raw
        if ($ReleaseText -match '(?m)^JAVA_VERSION="1\.8') {
            return $true
        }
    }

    $PreviousPreference = $ErrorActionPreference
    try {
        # Windows PowerShell can promote java -version's stderr to a terminating error.
        $ErrorActionPreference = 'Continue'
        $VersionOutput = (& $Java -version 2>&1 | Select-Object -First 1) -join ''
        return $VersionOutput -match 'version "(1\.8|8)([\._"]|$)'
    }
    catch {
        return $false
    }
    finally {
        $ErrorActionPreference = $PreviousPreference
    }
}

function Find-XapiJava8Home {
    $Candidates = @(
        $env:XAPI_JAVA8_HOME,
        $env:JAVA8_HOME,
        $env:JDK8_HOME,
        ${env:JAVA_HOME_8_X64},
        $env:JAVA_HOME
    )
    foreach ($Candidate in $Candidates) {
        if (Test-Java8Home $Candidate) {
            return $Candidate
        }
    }
    throw 'A Java 8 JDK is required for the two legacy Gradle stages. Set XAPI_JAVA8_HOME to its installation directory and rerun.'
}

if (-not $Fast) {
    Invoke-XapiGradle (Join-Path $XapiRoot 'net.wti.core') $GradleArguments
    Invoke-XapiGradle (Join-Path $XapiRoot 'net.wti.gradle.modern') ($GradleArguments + @('-x', 'functionalTest'))
    $Java8Home = Find-XapiJava8Home
    Write-Host "Using Java 8 from $Java8Home for legacy Gradle stages"
    Invoke-XapiGradle (Join-Path $XapiRoot 'net.wti.gradle.tools') $GradleArguments $Java8Home
    Invoke-XapiGradle (Join-Path $XapiRoot 'net.wti.gradle') $GradleArguments $Java8Home
}
else {
    Write-Host 'Fast mode: skipping prerequisite tool builds'
}

$MainArguments = @(
    '-Dxapi.composite=true',
    '-Pxapi.changing=true',
    '--parallel',
    '--build-cache',
    '--stacktrace',
    '-Pxapi.debug=false'
)
if (-not $Shadow) {
    $MainArguments += @('-x', 'shadowJar')
}
else {
    Write-Host 'Allowing shadow jar'
}
$MainArguments += $GradleArguments
Invoke-XapiGradle $XapiRoot $MainArguments
