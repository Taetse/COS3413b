0 GOSUB 12
1 LET V1 = 3
2 LET VAR0 = V0
3 LET VAR1 = V1
4 IF VAR0 > VAR1 THEN GOTO 6
5 GOTO 9
6 LET V2$ = "groot"
7 PRINT V2$
8 GOTO 11
9 LET V3$ = "nope"
10 PRINT V3$
11 GOTO 14
12 INPUT V0
13 RETURN
14 END
