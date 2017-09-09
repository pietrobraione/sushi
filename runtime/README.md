# SUSHI-lib

## About

This repository contains the runtime library part of SUSHI. The runtime library contains classes for managing the 
configuration parameters of SUSHI and for allowing the interaction of SUSHI with EvoSuite.

## Checking out the code

This repository contains an Eclipse project, so it must be checked out under Eclipse. Install Eclipse and the EGit 
plugin, then on the main menu select File > Import... and in the dialog select Projects from Git. Insert the URI 
of this repository, and when asked for a wizard for importing projects answer Import Existing Eclipse Projects. 
This will import the SUSHI-lib project in the current Eclipse workspace.

## Installing and usage

There is no real use of the SUSHI-lib project apart from being a dependency of the 
[SUSHI project](https://github.com/pietrobraione/sushi), so refer to it for setup instructions. If you want to produce
a standalone version of SUSHI you will need to produce a sushi-lib.jar: Double-click the sushi-lib-jar.jardesc to 
generate it.
