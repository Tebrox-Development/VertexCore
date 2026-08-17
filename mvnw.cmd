@echo off
setlocal
set "BASE_DIR=%~dp0"
set "MAVEN_VERSION=3.9.16"
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%"
set "DIST_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip"
set "DIST_SHA=5af3b743dd8b876b5c45da33b676251e5f1687712644abb4ee519ca56e1d89ce"

if exist "%MAVEN_HOME%\bin\mvn.cmd" goto run

powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $home='%MAVEN_HOME%'; $url='%DIST_URL%'; $sha='%DIST_SHA%'; $tmp=Join-Path ([IO.Path]::GetTempPath()) ('vertexcore-mvn-'+[guid]::NewGuid()); New-Item -ItemType Directory -Path $tmp | Out-Null; try { $zip=Join-Path $tmp 'maven.zip'; Invoke-WebRequest -UseBasicParsing $url -OutFile $zip; if ((Get-FileHash $zip -Algorithm SHA256).Hash.ToLower() -ne $sha) { throw 'Maven distribution checksum mismatch' }; Expand-Archive $zip -DestinationPath $tmp; New-Item -ItemType Directory -Force -Path (Split-Path $home) | Out-Null; if (Test-Path $home) { Remove-Item -Recurse -Force $home }; Move-Item (Join-Path $tmp 'apache-maven-%MAVEN_VERSION%') $home } finally { if (Test-Path $tmp) { Remove-Item -Recurse -Force $tmp } }" || exit /b 1

:run
call "%MAVEN_HOME%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%
