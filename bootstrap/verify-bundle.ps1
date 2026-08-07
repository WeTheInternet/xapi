[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$XapiRoot = Split-Path -Parent $PSScriptRoot
$Manifest = Join-Path $XapiRoot '.xapi-bootstrap/MANIFEST.sha256'

if (-not (Test-Path -LiteralPath $Manifest -PathType Leaf)) {
    throw "Missing installed bootstrap manifest: $Manifest. Extract the bootstrap ZIP into $XapiRoot first."
}

Push-Location $XapiRoot
try {
    foreach ($Line in Get-Content -LiteralPath $Manifest) {
        if ($Line -notmatch '^([0-9a-f]{64})  (.+)$') {
            throw "Malformed manifest line: $Line"
        }
        $Expected = $Matches[1]
        $RelativePath = $Matches[2].Replace('/', [IO.Path]::DirectorySeparatorChar)
        $Actual = (Get-FileHash -LiteralPath $RelativePath -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($Actual -ne $Expected) {
            throw "Checksum mismatch for ${RelativePath}: expected $Expected, found $Actual"
        }
        Write-Host "$RelativePath`: OK"
    }
}
finally {
    Pop-Location
}

Write-Host "Bootstrap seed verified for $XapiRoot"
