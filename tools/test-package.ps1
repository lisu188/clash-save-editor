param(
    [string]$Archive = "$PSScriptRoot/../desktop/build/distributions/ClashSaveEditor-windows-x64.zip",
    [string]$Destination = "$PSScriptRoot/../artifacts/package-smoke"
)
$ErrorActionPreference = 'Stop'
$archivePath = (Resolve-Path -LiteralPath $Archive).Path
$outputPath = [IO.Path]::GetFullPath($Destination)
if (Test-Path -LiteralPath $outputPath) { throw "Choose a new smoke-test directory: $outputPath" }
[IO.Compression.ZipFile]::ExtractToDirectory($archivePath, $outputPath)
$java = Join-Path $outputPath 'ClashSaveEditor/runtime/bin/java.exe'
if (-not (Test-Path -LiteralPath $java)) { throw 'Bundled Java console launcher is missing' }
if (-not (Test-Path -LiteralPath (Join-Path $outputPath 'ClashSaveMcp.cmd'))) { throw 'MCP launcher is missing' }
$start = [Diagnostics.ProcessStartInfo]::new($java)
$start.UseShellExecute = $false
$start.CreateNoWindow = $true
$start.RedirectStandardInput = $true
$start.RedirectStandardOutput = $true
$start.RedirectStandardError = $true
$start.ArgumentList.Add('-cp')
$start.ArgumentList.Add((Join-Path $outputPath 'mcp/lib/*'))
$start.ArgumentList.Add('com.lis.clash.mcp.ClashSaveMcpServerKt')
$process = [Diagnostics.Process]::new()
$process.StartInfo = $start
[void]$process.Start()
$stdout = $process.StandardOutput.ReadToEndAsync()
$stderr = $process.StandardError.ReadToEndAsync()
$process.StandardInput.WriteLine('{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25"}}')
$process.StandardInput.WriteLine('{"jsonrpc":"2.0","id":2,"method":"tools/list"}')
$process.StandardInput.Close()
if (-not $process.WaitForExit(30000)) { $process.Kill(); throw 'Packaged MCP timed out' }
$responses = @($stdout.GetAwaiter().GetResult() -split "`r?`n" | Where-Object { $_ } | ForEach-Object { $_ | ConvertFrom-Json })
$errorText = $stderr.GetAwaiter().GetResult()
if ($process.ExitCode -ne 0 -or $responses.Count -ne 2) { throw "Packaged MCP failed: $errorText" }
if ($responses[0].result.serverInfo.version -ne '2.0.0') { throw 'Unexpected packaged server version' }
$actualTools = @($responses[1].result.tools.name | Sort-Object)
$expectedTools = @('save_get_schema','save_get_overview','save_list_entities','save_read_object','save_read_bytes','save_set_property','save_write_bytes' | Sort-Object)
if (Compare-Object $expectedTools $actualTools) { throw 'Packaged tool contract changed' }
Write-Output "PACKAGED_MCP_OK tools=$($actualTools.Count) runtime=$java"
