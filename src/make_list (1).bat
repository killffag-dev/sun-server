@echo off
setlocal

set "FOLDER=%~dp0"
set "OUTFILE=%FOLDER%list.txt"
set "TMPFILE=%FOLDER%list_tmp.txt"

if exist "%TMPFILE%" del "%TMPFILE%" >nul 2>&1

dir /s /b "%FOLDER%" > "%TMPFILE%"

if exist "%OUTFILE%" del "%OUTFILE%" >nul 2>&1

findstr /v /i /e /c:"list.txt" /c:"list_tmp.txt" /c:"make_list.bat" /c:"make_list.cmd" /c:"run_silent.vbs" "%TMPFILE%" > "%OUTFILE%"

del "%TMPFILE%" >nul 2>&1

exit
