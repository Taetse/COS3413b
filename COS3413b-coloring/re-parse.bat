echo off
set arg1=%1
.\parser\lexer.exe %arg1% - | .\parser\parser.exe - ct.txt at.txt