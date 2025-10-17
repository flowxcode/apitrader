@echo off
setlocal EnableDelayedExpansion

REM Get current timestamp
for /f "tokens=2 delims==" %%a in ('wmic OS Get localdatetime /value') do set "dt=%%a"
set "timestamp=!dt:~0,8!_!dt:~8,6!"

REM Define source and destination
set "source=C:\Users\floiso\JForex4\Strategies\Strategy.java"
set "dest=C:\gitx\tradgrok\strategies"
set "filename=Strategy.java"
set "target=%dest%\!timestamp!_%filename%"

REM Copy the file with timestamp prefix
copy "!source!" "!target!"

if %errorlevel% equ 0 (
    echo File copied successfully to !target!
    
    REM Change to git repo root and add/commit the file
    cd /d "C:\gitx\tradgrok"
    git add "!target!"
    git commit -m "Add timestamped Strategy.java: !timestamp!"
    echo Git add and commit completed.
) else (
    echo Copy failed.
)

endlocal
pause