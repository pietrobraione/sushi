# SUSHI

## About

SUSHI is an automatic test case generator for Java programs, aimed at (high) branch coverage. It leverages a technique called 
symbolic execution to calculate, from a program path that reaches a branch, a *path constraint*, i.e., a constraint on the 
program inputs that, when satisfied, allows to cover the path. SUSHI calculates path constraints by exploiting the symbolic 
executor [JBSE](http://pietrobraione.github.io/jbse/). To solve path constraints SUSHI transforms them in search problems 
and feeds by them the search-based test case generator [EvoSuite](http://www.evosuite.org/).

The main advantage of SUSHI is its ability of generating tests for programs that take as inputs complex data structures, 
allowing, for instance, to automatically test compiler passes taking parse trees as inputs.

## Disclaimer

SUSHI is a research prototype. As such, it is more focused on functionality than on usability.

## Installing

Currently SUSHI is distributed as an [Eclipse](http://www.eclipse.org/) project. Install Eclipse and the EGit plugin, 
then on the main menu select File > Import... and in the dialog select Projects from Git. Insert the URI of this 
repository, and when asked for a wizard for importing projects answer Import Existing Eclipse Projects. This will import
the SUSHI project in the current Eclipse workspace.

## Dependencies

SUSHI depends on some software packages, most of which are already included in the repository. You will need to fix three
dependencies manually. The first is on the SUSHI runtime library, that resides on another repository at 
[this](https://github.com/pietrobraione/sushi-lib) URI. It must be installed as an Eclipse project in the same workspace
where the SUSHI project resides (with identical procedure). The second and third dependencies are the 
[GNU Linear Programming Kit (GLPK)](https://www.gnu.org/software/glpk/) and its Java wrapper 
[GLPK-Java](http://glpk-java.sourceforge.net/). If your operating system is Debian or Ubuntu you can install the libglpk 
and libglpk-java packages. Under OSX there is a GLPK package under Macports, but no package for GLPK-Java.
In the worst case you will need to install GLPK and GLPK-Java from the sources, in which case consider that both have many
dependency on their own. Once done that you need to reconfigure the Eclipse SUSHI project so it uses the GLPK 
