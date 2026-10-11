$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$root = Join-Path $projectRoot 'resourcepack'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem
foreach ($file in (Get-ChildItem $root -Recurse -File | Where-Object { $_.Extension -eq '.json' -or $_.Extension -eq '.mcmeta' })) {
    $null = [IO.File]::ReadAllText($file.FullName) | ConvertFrom-Json
}
$font = [IO.File]::ReadAllText((Join-Path $root 'assets/tilkiinterface/font/ui.json')) | ConvertFrom-Json
$logoProviders=@($font.providers | Where-Object { $_.type -eq 'bitmap' -and ($_.chars -contains [string][char]0xea01 -or $_.chars -contains [string][char]0xea02) })
if($logoProviders.Count -ne 2) { throw 'HUD and TAB need separate logo glyphs' }
if(!($logoProviders | Where-Object { $_.file -eq 'tilkiinterface:font/mascot.png' -and $_.height -eq 64 }) -or
   !($logoProviders | Where-Object { $_.file -eq 'tilkiinterface:font/tab_logo.png' -and $_.height -eq 52 })) { throw 'Logo glyph placement invalid' }
$escProvider=@($font.providers | Where-Object { $_.type -eq 'bitmap' -and $_.file -eq 'tilkiinterface:font/esc_frame.png' -and $_.chars -contains [string][char]0xea03 })
$escBottom=@($font.providers | Where-Object { $_.type -eq 'bitmap' -and $_.file -eq 'tilkiinterface:font/esc_frame_bottom.png' -and $_.chars -contains [string][char]0xea06 })
if($escProvider.Count -ne 1 -or $escProvider[0].height -ne 256 -or $escProvider[0].ascent -ne 44 -or $escBottom.Count -ne 1 -or $escBottom[0].height -ne 22) { throw 'Responsive ESC frame glyphs missing or misplaced' }
$tabFrameProviders=@($font.providers | Where-Object { $_.file -match '/tab_frame/' })
if($tabFrameProviders.Count -lt 42) { throw 'Dynamic TAB frame providers missing' }
if($font.providers | Where-Object { $_.file -match '(/hd/|sidebar_frame|/tab_[0-9])' }) { throw 'Legacy frame provider remains' }
foreach ($provider in $font.providers) {
    if ($provider.type -ne 'bitmap') { continue }
    $texture = Join-Path $root ('assets/tilkiinterface/textures/' + $provider.file.Substring('tilkiinterface:'.Length))
    $bitmap = [Drawing.Bitmap]::new($texture)
    try {
        if ($provider.chars.Count -ne 1 -or ($provider.chars[0].Length -ne 1 -and ![char]::IsSurrogatePair($provider.chars[0],0))) { throw 'Expected single-cell bitmap' }
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
    if ($definition.'menu.game' -ne ([string][char]0xea03+[string][char]0xea0a+[string][char]0xea06)) { throw 'Missing ESC frame translation' }
}
foreach ($state in @('button','button_highlighted','button_disabled')) {
    $imagePath = Join-Path $root ('assets/minecraft/textures/gui/sprites/widget/'+$state+'.png')
    $bitmap = [Drawing.Bitmap]::new($imagePath)
    try { if ($bitmap.Width -ne 200 -or $bitmap.Height -ne 20) { throw 'Invalid GUI button dimensions' } } finally { $bitmap.Dispose() }
    $metadata = [IO.File]::ReadAllText($imagePath+'.mcmeta') | ConvertFrom-Json
    if ($metadata.gui.scaling.type -ne 'nine_slice' -or $metadata.gui.scaling.border -ne 2) { throw 'GUI button scaling invalid' }
}
foreach($state in @('slider','slider_highlighted','text_field','text_field_highlighted')) {
    $imagePath=Join-Path $root ('assets/minecraft/textures/gui/sprites/widget/'+$state+'.png')
    $bitmap=[Drawing.Bitmap]::new($imagePath);try { if($bitmap.Width -ne 200 -or $bitmap.Height -ne 20){throw "Invalid themed control: $state"} } finally {$bitmap.Dispose()}
}
foreach($state in @('checkbox','checkbox_highlighted','checkbox_selected','checkbox_selected_highlighted')) {
    $imagePath=Join-Path $root ('assets/minecraft/textures/gui/sprites/widget/'+$state+'.png')
    $bitmap=[Drawing.Bitmap]::new($imagePath);try { if($bitmap.Width -ne 20 -or $bitmap.Height -ne 20){throw "Invalid themed checkbox: $state"} } finally {$bitmap.Dispose()}
}
$zipPath = Join-Path $projectRoot 'dist/TilkiInterface-ResourcePack.zip'
$zip = [IO.Compression.ZipFile]::OpenRead($zipPath)
try {
    if (!$zip.GetEntry('pack.mcmeta') -or !$zip.GetEntry('assets/minecraft/lang/tr_tr.json') -or !$zip.GetEntry('assets/tilkiinterface/textures/font/esc_frame.png') -or !$zip.GetEntry('assets/tilkiinterface/textures/font/esc_frame_bottom.png')) { throw 'ZIP layout invalid' }
    if($zip.Entries.FullName | Where-Object { $_ -like 'assets/tilkiinterface/textures/font/hd/*' -or $_ -like 'assets/tilkiinterface/textures/font/sidebar_frame*' -or $_ -match 'assets/tilkiinterface/textures/font/tab_[0-9]' }) { throw 'Legacy frame texture remains' }
} finally { $zip.Dispose() }
$hash = (Get-FileHash $zipPath -Algorithm SHA1).Hash.ToLowerInvariant()
if ($hash -ne ([IO.File]::ReadAllText((Join-Path $projectRoot 'dist/resourcepack-sha1.txt'))).Trim()) { throw 'Pack SHA1 mismatch' }
$config = [IO.File]::ReadAllText((Join-Path $projectRoot 'src/main/resources/config.yml'))
if (!$config.Contains($hash)) { throw 'Default config SHA1 mismatch' }
Write-Output 'VALID: HUD logo, dynamic TAB frame, responsive ESC frame, themed options controls, fonts, ZIP and SHA1.'
