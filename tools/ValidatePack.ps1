$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$root = Join-Path $projectRoot 'resourcepack'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem
foreach ($file in (Get-ChildItem $root -Recurse -File | Where-Object { $_.Extension -eq '.json' -or $_.Extension -eq '.mcmeta' })) {
    $null = [IO.File]::ReadAllText($file.FullName) | ConvertFrom-Json
}
$font = [IO.File]::ReadAllText((Join-Path $root 'assets/tilkiinterface/font/ui.json')) | ConvertFrom-Json
foreach ($provider in $font.providers) {
    $texture = Join-Path $root ('assets/tilkiinterface/textures/' + $provider.file.Substring('tilkiinterface:'.Length))
    $bitmap = [Drawing.Bitmap]::new($texture)
    try {
        if ($provider.chars.Count -ne 1 -or $provider.chars[0].Length -ne 1) { throw 'Expected single-cell bitmap' }
        if ($bitmap.Width -gt 256 -or $bitmap.Height -gt 256) { throw 'Bitmap glyph exceeds client atlas limit' }
        if ($provider.ascent -gt $provider.height) { throw 'Invalid ascent' }
    } finally { $bitmap.Dispose() }
}
foreach ($fontName in @('default','uniform')) {
    $definition = [IO.File]::ReadAllText((Join-Path $root ('assets/minecraft/font/'+$fontName+'.json'))) | ConvertFrom-Json
    if ($definition.providers[0].id -ne 'tilkiinterface:ui') { throw 'Missing font reference' }
}
foreach ($language in @('tr_tr','en_us','en_gb')) {
    $definition = [IO.File]::ReadAllText((Join-Path $root ('assets/minecraft/lang/'+$language+'.json'))) | ConvertFrom-Json
    if ($definition.'menu.game' -ne [string][char]0xea00) { throw 'Missing ESC logo translation' }
}
foreach ($state in @('button','button_highlighted','button_disabled')) {
    $imagePath = Join-Path $root ('assets/minecraft/textures/gui/sprites/widget/'+$state+'.png')
    $bitmap = [Drawing.Bitmap]::new($imagePath)
    try { if ($bitmap.Width -ne 200 -or $bitmap.Height -ne 20) { throw 'Invalid GUI button dimensions' } } finally { $bitmap.Dispose() }
    $metadata = [IO.File]::ReadAllText($imagePath+'.mcmeta') | ConvertFrom-Json
    if ($metadata.gui.scaling.type -ne 'nine_slice' -or $metadata.gui.scaling.border -ne 2) { throw 'GUI button scaling invalid' }
}
$zipPath = Join-Path $projectRoot 'dist/TilkiInterface-ResourcePack.zip'
$zip = [IO.Compression.ZipFile]::OpenRead($zipPath)
try {
    if (!$zip.GetEntry('pack.mcmeta') -or !$zip.GetEntry('assets/minecraft/lang/tr_tr.json')) { throw 'ZIP layout invalid' }
} finally { $zip.Dispose() }
$hash = (Get-FileHash $zipPath -Algorithm SHA1).Hash.ToLowerInvariant()
if ($hash -ne ([IO.File]::ReadAllText((Join-Path $projectRoot 'dist/resourcepack-sha1.txt'))).Trim()) { throw 'Pack SHA1 mismatch' }
$config = [IO.File]::ReadAllText((Join-Path $projectRoot 'src/main/resources/config.yml'))
if (!$config.Contains($hash)) { throw 'Default config SHA1 mismatch' }
Write-Output 'VALID: logo glyph bounds, default/Unicode font references, ESC languages, button textures/scaling, JSON, ZIP and SHA1.'
