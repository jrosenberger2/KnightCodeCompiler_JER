# KnightCodeSkeleton

This directory structure is for for the final project of CS322 (Compiler Construction). 
-There is an ant build.xml file you can use to buid/compile/clean the project if you wish.Refer to the build.xml file for the build/clean targets.

-You can write .kc files using the KnightCode.g4 grammar and then compile the .kc files into java .class files with kcc.java in the compiler directory

-To use the kcc compiler, call < java compiler/kcc ... > with two cmd line args. The first being the .kc file to compile, and the second being the name of the output file.

-The output file must be in the output directory.

-An Example compiler call would be: java compiler/kcc yourFile.kc output/YourOutput