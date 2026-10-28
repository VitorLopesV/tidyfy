# Le e grava a posicao dos icones da area de trabalho do Windows (ListView do Explorer), sem janela.
# Uso: desktop-icons.ps1 -Mode list
#      desktop-icons.ps1 -Mode move -MovesFile <arquivo UTF-8 com linhas "nome<TAB>x<TAB>y">
# Saida (ASCII, uma informacao por linha): AUTOARRANGE=<bool>, SNAPTOGRID=<bool>, AREA=left,top,width,height,
# CELL=width,height e ICON<TAB>nome-em-base64-UTF8<TAB>x<TAB>y. Falha: mensagem em stderr e codigo de saida 1.
# Em caso de erro no meio de um "move", as posicoes originais sao restauradas antes de sair.
param(
    [Parameter(Mandatory = $true)][ValidateSet('list', 'move')][string]$Mode,
    [string]$MovesFile
)
$ErrorActionPreference = 'Stop'

Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
public static class DeskNative {
    [DllImport("user32.dll")] public static extern bool SetProcessDPIAware();
    [DllImport("user32.dll")] public static extern IntPtr GetShellWindow();
    [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern IntPtr FindWindowEx(IntPtr parent, IntPtr after, string cls, string title);
    [DllImport("user32.dll")] public static extern IntPtr SendMessage(IntPtr h, int msg, IntPtr w, IntPtr l);
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr h, out uint pid);
    [DllImport("user32.dll")] public static extern int GetWindowLong(IntPtr h, int index);
    [DllImport("user32.dll")] public static extern bool ClientToScreen(IntPtr h, ref POINT p);
    [DllImport("user32.dll")] public static extern bool SystemParametersInfo(uint action, uint param, out RECT r, uint flags);
    [DllImport("kernel32.dll")] public static extern IntPtr OpenProcess(uint access, bool inherit, uint pid);
    [DllImport("kernel32.dll")] public static extern IntPtr VirtualAllocEx(IntPtr p, IntPtr addr, IntPtr size, uint type, uint prot);
    [DllImport("kernel32.dll")] public static extern bool VirtualFreeEx(IntPtr p, IntPtr addr, IntPtr size, uint type);
    [DllImport("kernel32.dll")] public static extern bool ReadProcessMemory(IntPtr p, IntPtr addr, byte[] buf, int size, out IntPtr read);
    [DllImport("kernel32.dll")] public static extern bool WriteProcessMemory(IntPtr p, IntPtr addr, byte[] buf, int size, out IntPtr written);
    [DllImport("kernel32.dll")] public static extern bool CloseHandle(IntPtr h);
    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
    [StructLayout(LayoutKind.Sequential)] public struct POINT { public int X, Y; }
}
'@

$LVM_GETITEMCOUNT = 0x1004
$LVM_SETITEMPOSITION = 0x100F
$LVM_GETITEMPOSITION = 0x1010
$LVM_GETITEMSPACING = 0x1033
$LVM_GETEXTENDEDLISTVIEWSTYLE = 0x1037
$LVM_GETITEMTEXTW = 0x1073
$LVS_AUTOARRANGE = 0x100
$LVS_EX_SNAPTOGRID = 0x40000
$GWL_STYLE = -16
$SPI_GETWORKAREA = 0x30
$REMOTE_TEXT_OFFSET = 256
$REMOTE_SIZE = 4096
$MAX_NAME_CHARS = 260

[void][DeskNative]::SetProcessDPIAware()

function Find-DesktopListView {
    $none = [NullString]::Value
    $shell = [DeskNative]::GetShellWindow()
    if ($shell -eq [IntPtr]::Zero) { throw 'A area de trabalho nao esta disponivel (sem sessao grafica).' }
    $defView = [DeskNative]::FindWindowEx($shell, [IntPtr]::Zero, 'SHELLDLL_DefView', $none)
    if ($defView -eq [IntPtr]::Zero) {
        $worker = [IntPtr]::Zero
        do {
            $worker = [DeskNative]::FindWindowEx([IntPtr]::Zero, $worker, 'WorkerW', $none)
            if ($worker -ne [IntPtr]::Zero) {
                $defView = [DeskNative]::FindWindowEx($worker, [IntPtr]::Zero, 'SHELLDLL_DefView', $none)
            }
        } while ($defView -eq [IntPtr]::Zero -and $worker -ne [IntPtr]::Zero)
    }
    if ($defView -eq [IntPtr]::Zero) { throw 'Nao foi possivel localizar a area de trabalho do Explorer.' }
    $listView = [DeskNative]::FindWindowEx($defView, [IntPtr]::Zero, 'SysListView32', $none)
    if ($listView -eq [IntPtr]::Zero) { throw 'Nao foi possivel localizar a lista de icones da area de trabalho.' }
    return $listView
}

function Send($msg, $w, $l) { return [DeskNative]::SendMessage($script:listView, $msg, $w, $l) }

function Read-Remote($address, $size) {
    $buffer = New-Object byte[] $size
    $read = [IntPtr]::Zero
    if (-not [DeskNative]::ReadProcessMemory($script:process, $address, $buffer, $size, [ref]$read)) {
        throw 'Falha ao ler a memoria do Explorer.'
    }
    return $buffer
}

function Get-Position($index) {
    [void](Send $LVM_GETITEMPOSITION ([IntPtr]$index) $script:remote)
    $bytes = Read-Remote $script:remote 8
    return [pscustomobject]@{ X = [BitConverter]::ToInt32($bytes, 0); Y = [BitConverter]::ToInt32($bytes, 4) }
}

function Get-Name($index) {
    $textAddress = [IntPtr]($script:remote.ToInt64() + $REMOTE_TEXT_OFFSET)
    $item = New-Object byte[] 88
    [BitConverter]::GetBytes([int]0x1).CopyTo($item, 0)
    [BitConverter]::GetBytes([int]$index).CopyTo($item, 4)
    [BitConverter]::GetBytes([int64]$textAddress.ToInt64()).CopyTo($item, 24)
    [BitConverter]::GetBytes([int]$MAX_NAME_CHARS).CopyTo($item, 32)
    $written = [IntPtr]::Zero
    [void][DeskNative]::WriteProcessMemory($script:process, $script:remote, $item, 88, [ref]$written)
    [void](Send $LVM_GETITEMTEXTW ([IntPtr]$index) $script:remote)
    $text = [System.Text.Encoding]::Unicode.GetString((Read-Remote $textAddress ($MAX_NAME_CHARS * 2)))
    $end = $text.IndexOf([char]0)
    if ($end -ge 0) { $text = $text.Substring(0, $end) }
    return $text
}

function Set-Position($index, $x, $y) {
    $packed = (($y -band 0xFFFF) -shl 16) -bor ($x -band 0xFFFF)
    [void](Send $LVM_SETITEMPOSITION ([IntPtr]$index) ([IntPtr]$packed))
}

function Get-Icons {
    $count = [int](Send $LVM_GETITEMCOUNT ([IntPtr]::Zero) ([IntPtr]::Zero))
    $icons = @()
    for ($i = 0; $i -lt $count; $i++) {
        $position = Get-Position $i
        $icons += [pscustomobject]@{ Index = $i; Name = (Get-Name $i); X = $position.X; Y = $position.Y }
    }
    return $icons
}

function ConvertTo-Base64($text) { return [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($text)) }

$listView = Find-DesktopListView
[uint32]$processId = 0
[void][DeskNative]::GetWindowThreadProcessId($listView, [ref]$processId)
$process = [DeskNative]::OpenProcess(0x0008 -bor 0x0010 -bor 0x0020, $false, $processId)
if ($process -eq [IntPtr]::Zero) { throw 'Sem permissao para acessar o Explorer.' }
$remote = [IntPtr]::Zero
try {
    $remote = [DeskNative]::VirtualAllocEx($process, [IntPtr]::Zero, [IntPtr]$REMOTE_SIZE, 0x3000, 0x04)
    if ($remote -eq [IntPtr]::Zero) { throw 'Falha ao alocar memoria no Explorer.' }

    $style = [DeskNative]::GetWindowLong($listView, $GWL_STYLE)
    $exStyle = [int](Send $LVM_GETEXTENDEDLISTVIEWSTYLE ([IntPtr]::Zero) ([IntPtr]::Zero))
    $autoArrange = (($style -band $LVS_AUTOARRANGE) -ne 0)
    $snapToGrid = (($exStyle -band $LVS_EX_SNAPTOGRID) -ne 0)
    $spacing = [int64](Send $LVM_GETITEMSPACING ([IntPtr]::Zero) ([IntPtr]::Zero))
    $origin = New-Object DeskNative+POINT
    [void][DeskNative]::ClientToScreen($listView, [ref]$origin)
    $work = New-Object DeskNative+RECT
    [void][DeskNative]::SystemParametersInfo($SPI_GETWORKAREA, 0, [ref]$work, 0)

    if ($Mode -eq 'move') {
        if ($autoArrange) { throw 'A opcao "Organizar icones automaticamente" esta ligada na area de trabalho.' }
        $icons = Get-Icons
        $moves = @()
        foreach ($line in [System.IO.File]::ReadAllLines($MovesFile, [System.Text.Encoding]::UTF8)) {
            if ($line.Length -eq 0) { continue }
            $parts = $line.Split("`t")
            $moves += [pscustomobject]@{ Name = $parts[0]; X = [int]$parts[1]; Y = [int]$parts[2] }
        }
        $moved = @()
        try {
            foreach ($move in $moves) {
                $icon = $icons | Where-Object { $_.Name -eq $move.Name -and $moved -notcontains $_.Index } | Select-Object -First 1
                if ($null -eq $icon) { throw "Icone nao encontrado na area de trabalho: $($move.Name)" }
                Set-Position $icon.Index $move.X $move.Y
                $moved += $icon.Index
            }
        } catch {
            foreach ($index in $moved) {
                $original = $icons | Where-Object { $_.Index -eq $index }
                Set-Position $original.Index $original.X $original.Y
            }
            throw
        }
        Write-Output "MOVED=$($moved.Count)"
    } else {
        Write-Output "AUTOARRANGE=$($autoArrange.ToString().ToLowerInvariant())"
        Write-Output "SNAPTOGRID=$($snapToGrid.ToString().ToLowerInvariant())"
        Write-Output ("AREA={0},{1},{2},{3}" -f ($work.Left - $origin.X), ($work.Top - $origin.Y), ($work.Right - $work.Left), ($work.Bottom - $work.Top))
        Write-Output ("CELL={0},{1}" -f ($spacing -band 0xFFFF), (($spacing -shr 16) -band 0xFFFF))
        foreach ($icon in (Get-Icons)) {
            Write-Output ("ICON`t{0}`t{1}`t{2}" -f (ConvertTo-Base64 $icon.Name), $icon.X, $icon.Y)
        }
    }
} catch {
    [Console]::Error.WriteLine($_.Exception.Message)
    exit 1
} finally {
    if ($remote -ne [IntPtr]::Zero) { [void][DeskNative]::VirtualFreeEx($process, $remote, [IntPtr]::Zero, 0x8000) }
    [void][DeskNative]::CloseHandle($process)
}
