@echo off
echo Building 100%% Serverless V3 Wiki...
echo.

if not exist out mkdir out
javac -d out tools\IconRenderer.java tools\WikiBuilder.java
java -cp out tools.WikiBuilder

echo.
echo Du kannst nun einfach docs\index.html per Doppelklick in deinem Browser oeffnen!
echo Kein Server mehr noetig.
