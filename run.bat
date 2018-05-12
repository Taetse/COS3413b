echo off
set arg1=%1
.\parser\lexer.exe %arg1% - | .\parser\parser.exe - ct.txt at.txt
if not exist .\src\Main.class javac .\src\*.java
java -cp .\src\ Main at.txt
