

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

python -m pip install -q -r scripts/requirements-edge-tests.txt
python scripts/test_edge_cases.py @args
exit $LASTEXITCODE
