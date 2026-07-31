function Invoke-EnterpriseLabProofTool {
    param([Parameter(Mandatory)][string[]]$Arguments)

    $classPathFile = "target/enterprise-lab-proof-tools-classpath.txt"
    $compileOutput = & mvn -q -DskipTests test-compile dependency:build-classpath `
        "-Dmdep.includeScope=test" "-Dmdep.outputFile=$classPathFile" 2>&1
    $compileExitCode = $LASTEXITCODE
    if ($compileExitCode -ne 0) {
        throw "Enterprise Lab proof-tool compilation failed with exit code $compileExitCode`n$($compileOutput -join "`n")"
    }
    if (-not (Test-Path -LiteralPath $classPathFile)) {
        throw "Enterprise Lab proof-tool dependency classpath was not generated."
    }

    $dependencyClassPath = (Get-Content -LiteralPath $classPathFile -Raw).Trim()
    $classPathEntries = @("target/test-classes", "target/classes")
    if (-not [string]::IsNullOrWhiteSpace($dependencyClassPath)) {
        $classPathEntries += $dependencyClassPath
    }
    $classPath = $classPathEntries -join [System.IO.Path]::PathSeparator
    $output = & java -cp $classPath `
        "com.richmond423.loadbalancerpro.cli.EnterpriseLabProofToolsApplication" `
        @Arguments 2>&1

    return [pscustomobject]@{
        ExitCode = $LASTEXITCODE
        Output = @($output)
    }
}
