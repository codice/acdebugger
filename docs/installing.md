# Installation

# Linux:

From a terminal run:
```bash
VERSION=1.7-SNAPSHOT # Set to the version to download
REPOSITORY=snapshots # Set to snapshots for a snapshot version or releases for released version
curl -L \
  http://artifacts.codice.org/service/local/artifact/maven/content/?g=org.codice.acdebugger&a=acdebugger-distribution&v=$VERSION&r=$REPOSITORY&e=tar.g -o acdebugger-distribution-$VERSION \
  -o acdebugger-distribution-$VERSION.tar.gz
 
sudo mkdir -p /usr/local/share/codice

sudo tar xzf acdebugger-distribution-$VERSION.tar.gz -C /usr/local/share/codice/

sudo ln -s /usr/local/share/codice/acdebugger-distribution-$VERSION/bin/acdebugger /usr/local/bin/
```

# Windows

From powershell run:

```powershell
$version="1.7-SNAPSHOT"
$repository="snapshots"
$url = "http://artifacts.codice.org/service/local/artifact/maven/content/?g=org.codice.acdebugger&a=acdebugger-distribution&v=$version&r=$repository&e=zip"
$tmpOutput="$env:temp\acdebugger-distribution-$version.zip"

(New-Object System.Net.WebClient).DownloadFile($url, $tmpOutput)

New-Item -ItemType directory -Path $env:ProgramFiles\codice
Add-Type -AssemblyName System.IO.Compression.FileSystem

[System.IO.Compression.ZipFile]::ExtractToDirectory($tmpOutput, $env:ProgramFiles\codice)
```
