# Stage 9 final acceptance. Run from any directory with PowerShell 5.1+.
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root
$log = Join-Path $root 'Stage9_Gradle_Log.txt'
$out = Join-Path $root 'Stage9_Final_Audit.txt'
$lines = New-Object System.Collections.Generic.List[string]
$lines.Add('TANKS STAGE 9: FINAL QUALITY & JAVA 8 AUDIT')
$lines.Add('')
Write-Host 'Running JUnit, JaCoCo and Javadoc...'
$ErrorActionPreference = 'Continue'
& gradle clean test jacocoTestReport javadoc --rerun-tasks *> $log
$exit = $LASTEXITCODE
$ErrorActionPreference = 'Stop'
if ($exit -ne 0) {
    $lines.Add("GRADLE: FAIL (exit $exit)")
    $lines.Add('Check Stage9_Gradle_Log.txt')
    $lines | Set-Content -Encoding UTF8 $out
    Get-Content $out
    if (Test-Path $log) { Get-Content $log -Tail 100 }
    exit 1
}
$lines.Add('GRADLE: PASS')

$testResults = Join-Path $root 'build\test-results\test'
$xmls = @(Get-ChildItem $testResults -Filter 'TEST-*.xml' -ErrorAction SilentlyContinue)
$total=0; $fails=0; $errors=0; $skipped=0
foreach ($f in $xmls) {
    [xml]$r=Get-Content -Raw $f.FullName
    $total += [int]$r.testsuite.tests
    $fails += [int]$r.testsuite.failures
    $errors += [int]$r.testsuite.errors
    $skipped += [int]$r.testsuite.skipped
}
$lines.Add("JUNIT: $total tests; failed=$fails; errors=$errors; skipped=$skipped")
if ($total -eq 147 -and $fails -eq 0 -and $errors -eq 0 -and $skipped -eq 0) {
    $lines.Add('JUNIT EXPECTED 147/147: PASS')
} else { $lines.Add('JUNIT EXPECTED 147/147: NEEDS REVIEW') }

$jacoco = Join-Path $root 'build\reports\jacoco\test\jacocoTestReport.xml'
if (Test-Path $jacoco) {
    [xml]$r=Get-Content -Raw $jacoco
    $ins=@($r.report.counter | Where-Object {$_.type -eq 'INSTRUCTION'})[0]
    $br=@($r.report.counter | Where-Object {$_.type -eq 'BRANCH'})[0]
    if ($null -ne $ins -and $null -ne $br) {
        $ip=[math]::Round(100*[double]$ins.covered/([double]$ins.covered+[double]$ins.missed),2)
        $bp=[math]::Round(100*[double]$br.covered/([double]$br.covered+[double]$br.missed),2)
        $av=[math]::Round(($ip+$bp)/2,2)
        $lines.Add("JACOCO: instruction=$ip%; branch=$bp%; average=$av%")
        if ($av -gt 90) {$lines.Add('JACOCO >90%: PASS')} else {$lines.Add('JACOCO >90%: NOT MET')}
    }
} else {$lines.Add('JACOCO: REPORT NOT FOUND')}

$jdoc=Join-Path $root 'build\docs\javadoc\index.html'
if (Test-Path $jdoc) {$lines.Add('JAVADOC: PASS')} else {$lines.Add('JAVADOC: NOT FOUND')}

# class-file header: CAFEBABE, u2 minor at bytes 4-5, u2 major at bytes 6-7
$cls=Join-Path $root 'build\classes\java\main\Tanks\App.class'
if (Test-Path $cls) {
    $bytes=[System.IO.File]::ReadAllBytes($cls)
    if ($bytes.Length -ge 8 -and $bytes[0] -eq 202 -and $bytes[1] -eq 254 -and $bytes[2] -eq 186 -and $bytes[3] -eq 190) {
        $major=[int]$bytes[6]*256+[int]$bytes[7]
        if ($major -eq 52) {$lines.Add('JAVA 8 BYTECODE: PASS (major=52)')}
        else {$lines.Add("JAVA 8 BYTECODE: FAIL (major=$major)")}
    } else {$lines.Add('JAVA 8 BYTECODE: INVALID CLASS FILE')}
} else {$lines.Add('JAVA 8 BYTECODE: APP.CLASS NOT FOUND')}
$lines.Add('')
$lines.Add('NOTE: major=52 and --release 8 on newer JDKs are build checks, NOT proof of runtime on a Java 8 installation.')
$lines.Add('NOTE: Rendering tests are headless and do not compare real pixels.')
$lines | Set-Content -Encoding UTF8 $out
Get-Content $out
Write-Host "Result: $out"
Write-Host "Gradle log: $log"
