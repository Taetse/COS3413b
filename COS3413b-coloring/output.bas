0 INPUT AA
1 LET AB = 0
2 LET AC = 0
3 LET AD = AA
4 LET AE = AB
5 IF AD > AE THEN GOTO 7
6 GOTO 16
7 GOTO 9
8 GOTO 16
9 LET AF = AB
10 LET AG = 1
11 LET AB = AF + AG
12 LET AH = AC
13 LET AI = 1
14 LET AC = AH + AI
15 GOTO 3
16 PRINT AC
17 PRINT AB
18 LET AJ$ = "halting"
19 PRINT AJ$
20 GOTO 33
21 PRINT AC
22 PRINT AB
23 GOSUB 26
24 GOTO 33
25 GOTO 33
26 LET AK$ = "we are"
27 PRINT AK$
28 GOSUB 30
29 RETURN
30 LET AL$ = "done"
31 PRINT AL$
32 RETURN
33 END
