$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$root = Join-Path $projectRoot 'resourcepack'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem
foreach ($file in (Get-ChildItem $root -Recurse -File | Where-Object { $_.Extension -eq '.json' -or $_.Extension -eq '.mcmeta' })) {
    $null = [IO.File]::ReadAllText($file.FullName) | ConvertFrom-Json
}
$font = [IO.File]::ReadAllText((Join-Path $root 'assets/tilkiinterface/font/ui.json')) | ConvertFrom-Json
# Forest HUD: eight 9 px stat icons, one sidebar frame glyph, TAB frame pieces with
# one provider per row position; the old mascot/TAB logo glyphs are gone.
$icons=@($font.providers | Where-Object { $_.type -eq 'bitmap' -and $_.file -like 'tilkiinterface:font/icon_*' })
if($icons.Count -ne 8 -or ($icons | Where-Object { $_.height -ne 9 -or $_.ascent -ne 8 })) { throw 'Stat icons must be eight one-line glyphs' }
$sidebar=@($font.providers | Where-Object { $_.file -eq 'tilkiinterface:font/sidebar_logo.png' })
if($sidebar.Count -ne 1 -or $sidebar[0].chars[0] -ne [string][char]0xea28) { throw 'Sidebar logo glyph missing' }
$rowPieces=@($font.providers | Where-Object { $_.file -like 'tilkiinterface:font/hud/row_*' })
if($rowPieces.Count -ne 60) { throw 'TAB rows need 20 positions of three pieces' }
if($font.providers | Where-Object { $_.file -match '(mascot|tab_logo)\.png$' }) { throw 'Old HUD/TAB logo glyph remains' }
if(!(Test-Path (Join-Path $projectRoot 'src/main/resources/hud-layout.properties'))) { throw 'Plugin HUD layout missing' }
if($font.providers | Where-Object { $_.file -eq 'tilkiinterface:font/esc_frame.png' }) { throw 'Title-anchored ESC frame remains' }
if($font.providers | Where-Object { $_.file -match '/tab_frame/' }) { throw 'Rejected TAB frame provider remains' }
if($font.providers | Where-Object { $_.file -match '(/tab_frame/|/tab_[0-9])' }) { throw 'Legacy frame provider remains' }
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
# The ESC frame is the zero-width label of "Back to Game", so it follows the
# pause grid at every GUI scale. Its advances must sum to zero and every native
# button rectangle must stay transparent.
$escFont=@{};$escAdvance=@{}
foreach($provider in $font.providers) {
    if($provider.type -eq 'space') { foreach($property in $provider.advances.PSObject.Properties) { $escAdvance[[int][char]$property.Name]=[int]$property.Value } }
    elseif($provider.type -eq 'bitmap' -and $provider.file -like 'tilkiinterface:font/esc_menu/*') { $escFont[[int][char]$provider.chars[0]]=$provider }
}
if($escFont.Count -ne 2) { throw 'ESC menu needs one 1x frame glyph and one label glyph' }
$escLabel=$null
foreach ($language in @('tr_tr','en_us','en_gb')) {
    $definition = [IO.File]::ReadAllText((Join-Path $root ('assets/minecraft/lang/'+$language+'.json'))) | ConvertFrom-Json
    if ($definition.'menu.game' -ne '') { throw "Fixed-position ESC title must stay hidden: $language" }
    if (!$definition.'menu.returnToGame' -or ($escLabel -and $definition.'menu.returnToGame' -ne $escLabel)) { throw "Missing ESC frame label: $language" }
    $escLabel=$definition.'menu.returnToGame'
}
$net=0;$tileTops=@{}
foreach($character in $escLabel.ToCharArray()) {
    $code=[int]$character
    if($escAdvance.ContainsKey($code)) { $net+=$escAdvance[$code];continue }
    if(!$escFont.ContainsKey($code)) { throw ('Unknown ESC label glyph {0:x4}' -f $code) }
    $provider=$escFont[$code]
    $bitmap=[Drawing.Bitmap]::new((Join-Path $root ('assets/tilkiinterface/textures/' + $provider.file.Substring('tilkiinterface:'.Length))))
    try {
        $measured=0
        for($x=$bitmap.Width-1;$x -ge 0 -and $measured -eq 0;$x--) { for($y=0;$y -lt $bitmap.Height;$y++) { if($bitmap.GetPixel($x,$y).A -ne 0) { $measured=$x+1;break } } }
        if($provider.file -match 'r(\d)c(\d)\.png$') {
            # Label baseline is button top + 6; glyph top = baseline + 7 - ascent.
            $top=13-$provider.ascent;$left=$net
            $legacyGrid=@(@(-102,0,204),@(-102,24,98),@(4,24,98),@(-102,48,98),@(4,48,98),@(-102,72,98),@(4,72,98),@(-102,96,204))
            # 26.2+ (overlay): the feedback row becomes four centred 20 px icon buttons.
            $iconGrid=@(@(-102,0,204),@(-102,24,98),@(4,24,98),@(-46,48,20),@(-22,48,20),@(2,48,20),@(26,48,20),@(-102,72,98),@(4,72,98),@(-102,96,204))
            $overlayPath=Join-Path $root ('tilki_icon_menu/assets/tilkiinterface/textures/' + $provider.file.Substring('tilkiinterface:'.Length))
            foreach($variant in @(@($bitmap,$legacyGrid),@($(if(Test-Path $overlayPath){[Drawing.Bitmap]::new($overlayPath)}else{$bitmap}),$iconGrid))) {
                $image=$variant[0];$scale=$image.Height/$provider.height
                if($image.Width -ne $bitmap.Width -or $image.Height -ne $bitmap.Height) { throw 'Overlay ESC tile changes glyph size' }
                foreach($button in $variant[1]) {
                    for($by=$button[1];$by -lt $button[1]+20;$by++) { for($bx=$button[0];$bx -lt $button[0]+$button[2];$bx++) {
                        $tx=[int][Math]::Floor(($bx-$left+0.5)*$scale);$ty=[int][Math]::Floor(($by-$top+0.5)*$scale)
                        if($tx -ge 0 -and $ty -ge 0 -and $tx -lt $image.Width -and $ty -lt $image.Height -and $image.GetPixel($tx,$ty).A -ge 26) { throw "ESC frame covers a pause button at $bx,$by" }
                    } }
                }
            }
        }
        $net+=[int][Math]::Floor(0.5+$measured*$provider.height/$bitmap.Height)+1
    } finally { $bitmap.Dispose() }
}
if($net -ne 0) { throw "ESC label must have zero width, got $net" }
foreach ($state in @('button','button_highlighted','button_disabled')) {
    $imagePath = Join-Path $root ('assets/minecraft/textures/gui/sprites/widget/'+$state+'.png')
    $bitmap = [Drawing.Bitmap]::new($imagePath)
    try { if ($bitmap.Width -ne 200 -or $bitmap.Height -ne 20) { throw 'GUI button must match the vanilla 200x20 sprite' } } finally { $bitmap.Dispose() }
    $metadata = [IO.File]::ReadAllText($imagePath+'.mcmeta') | ConvertFrom-Json
    if ($metadata.gui.scaling.type -ne 'nine_slice' -or $metadata.gui.scaling.width -ne 200 -or $metadata.gui.scaling.height -ne 20 -or $metadata.gui.scaling.border -ne 3) { throw 'GUI button scaling invalid' }
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
    if (!$zip.GetEntry('pack.mcmeta') -or !$zip.GetEntry('assets/minecraft/lang/tr_tr.json') -or !$zip.GetEntry('assets/tilkiinterface/textures/font/esc_menu/r0c0.png') -or !$zip.GetEntry('tilki_icon_menu/assets/tilkiinterface/textures/font/esc_menu/r0c0.png') -or !$zip.GetEntry('assets/tilkiinterface/textures/font/sidebar_logo.png') -or !$zip.GetEntry('assets/minecraft/textures/gui/sprites/icon/ping_5.png') -or $zip.GetEntry('assets/tilkiinterface/textures/font/esc_frame.png')) { throw 'ZIP layout invalid' }
    if($zip.GetEntry('assets/tilkiinterface/textures/font/esc_frame_bottom.png') -or $zip.Entries.FullName | Where-Object { $_ -like 'assets/tilkiinterface/textures/font/tab_frame/*' }) { throw 'Removed frame assets remain in ZIP' }
    if($zip.Entries.FullName | Where-Object { $_ -like 'assets/tilkiinterface/textures/font/hd/*' -or $_ -match 'assets/tilkiinterface/textures/font/tab_[0-9]' }) { throw 'Legacy frame texture remains' }
} finally { $zip.Dispose() }
$hash = (Get-FileHash $zipPath -Algorithm SHA1).Hash.ToLowerInvariant()
if ($hash -ne ([IO.File]::ReadAllText((Join-Path $projectRoot 'dist/resourcepack-sha1.txt'))).Trim()) { throw 'Pack SHA1 mismatch' }
$config = [IO.File]::ReadAllText((Join-Path $projectRoot 'src/main/resources/config.yml'))
if (!$config.Contains($hash)) { throw 'Default config SHA1 mismatch' }
Write-Output 'VALID: forest ESC frame (1x, button-anchored, 1.21.4 and 26.2+ holes), sidebar logo and icons, TAB frame pieces, ping bars, kit buttons, fonts, ZIP and SHA1.'
