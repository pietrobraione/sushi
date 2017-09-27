# SUSHI

## About

SUSHI is an automatic test case generator for Java programs, aimed at (high) branch coverage. It leverages a technique called 
symbolic execution to calculate, from a program path that reaches a branch, a *path constraint*, i.e., a constraint on the 
program inputs that, when satisfied, allows to cover the path. SUSHI calculates path constraints by exploiting the symbolic 
executor [JBSE](http://pietrobraione.github.io/jbse/). To solve path constraints SUSHI transforms them in search problems 
and feeds by them the search-based test case generator [EvoSuite](http://www.evosuite.org/).

The main advantage of SUSHI is its ability of generating tests for programs that take as inputs complex data structures, 
allowing, for instance, to automatically test compiler passes taking parse trees as inputs.

## Building SUSHI ##

SUSHI is composed of several projects that are imported as git submodules. To get the code for all dependent project, you should clone the SUSHI git repository and then run `git submodules init && git submodule update`. 

To build SUSHI we provide a Maven POM file and compilation is invoked with `mvn compile`. The supported Maven goals are:

* clean
* compile
* test
* package

We advise *not* to rely on the Eclipse default mechanism to import a Maven project from a repository, but rather to clone the git repository, run `mvn eclipse:eclipse` to create the Eclipse project files, and finally import everything under Eclipse by invoking File > Import..., selecting Maven > Existing Maven projects, pointing to your clone of the SUSHI repository, and checking all the boxes. This will create four projects in your Eclipse workspace:

* sushi: the container project from which Maven must be run;
* jbse: JBSE as a submodule; on the filesystem it is in the `jbse` subdirectory;
* sushi-master: the bulk of the SUSHI tool; on the filesystem it is in the `master` subdirectory;
* sushi-lib: the [sushi-lib](https://github.com/pietrobraione/sushi-lib) submodule for the run-time library component of SUSHI; on the filesystem it is in the `runtime` subdirectory;
* sushi-shaded: this project produces an uber JAR for the complete SUSHI tool; on the filesystem it is in the `shaded` subdirectory.

To avoid conflicts we advise to import everything under an empty workspace.

## Dependencies

To provide its functionalities SUSHI depends on several software packages, most of which are automatically resolved by Maven or are included in the repository. These packages are:

* [EvoSuite](http://www.evosuite.org/); SUSHI depends on a modified version of EvoSuite 1.0.3 that can be found in the `evosuite` subdirectory; This dependency is needed at runtime;
* [args4j](http://args4j.kohsuke.org/) version 2.32; This dependency is needed at runtime;
* [JaCoCo](http://www.eclemma.org/jacoco/) version 0.7.5; This dependency is needed at runtime;
* [ASM](http://asm.ow2.org/) version 5.0.1; This dependency is needed at runtime;
* plus the dependencies of JBSE (currently [Javassist](http://jboss-javassist.github.io/javassist/) version 3.4.GA).

Moreover there are two additional dependencies you will need to fix manually. The first dependency is on [Z3](https://github.com/Z3Prover/z3): Just download the last version of Z3 and install it. The second dependency is on the [GNU Linear Programming Kit (GLPK)](https://www.gnu.org/software/glpk/) and its Java wrapper [GLPK-Java](http://glpk-java.sourceforge.net/), both unfortunately having a native part. If your operating system is Debian or Ubuntu you can install the libglpk and libglpk-java packages. Under OSX there is a GLPK package under Macports, but no package for GLPK-Java. In the worst case you will need to install GLPK and/or GLPK-Java from sources, in which case consider that both have many other dependencies on their own. Once done that, you will (possibly) need to modify the Maven POM file, and configure the Eclipse SUSHI project so it links to the native part of the GLPK-Java library. 

* First, if you installed a version of GLPK-Java that is different from that indicated in the POM file of the sushi-master subproject (currently 1.10), you must modify the POM file so that it matches the version you installed on your system. Edit the `master/pom.xml` file and modify the `<version>` tag of the `org.gnu.glpk:glpk-java` artifact.
* Second, you may need to regenerate the Eclipse project files: This is not necessary if you did not need to edit the `master/pom.xml` file, or if you edited it before importing the Eclipse projects in the workspace. Otherwise, run `mvn eclipse:clean eclipse:eclipse` from a console, and then do a refresh of the projects in the Eclipse package explorer. For maximum safety rebuild everything by running `mvn clean compile package`.
* Finally, you need to tell Eclipse where the native libraries for GLPK-Java are. At the purpose right-click the sushi-master Eclipse project in the Eclipse package explorer, and select Build Path > Configure Build Path... from the contextual menu. Then select the Libraries tab, and find the `M2_REPO/org/gnu/glpk/glpk-java/...` entry, click the triangle on the left of it, select Native Library Location, click the Edit button and enter the location of the JNI libraries produced by GLPK-Java (the GLPK-Java documentation will tell you where this location is).

## Usage

Once set the classpath you can launch SUSHI as follows:

    $ java sushi.Main <options>
    
If you launch SUSHI without options it will print a help screen that lists all the available options. The indispensable ones, that you *must* set in order for SUSHI to work, are:

* `-classes`: a semicolon separated list of paths; It is the classpath of the software to test.
* `-target_class`: the name in [internal classfile format](http://docs.oracle.com/javase/specs/jvms/se6/html/ClassFile.doc.html#14757) of the class to test: SUSHI will generate tests for all the methods in the class. Or alternatively:
* `-target_method`: the signature of a method to test. The signature is a semicolon-separated list of: the name of the container class in internal classfile format; the [descriptor](http://docs.oracle.com/javase/specs/jvms/se6/html/ClassFile.doc.html#1169) of the method; the name of the method. You can use the `javap` command, included with every JDK setup, to obtain the internal format signatures of methods: `javap -s my.Class` prints the list of all the methods in `my.Class` with their signatures in internal format.
* `-evosuite`: the path of the EvoSuite jar file contained in the `evosuite/` folder.
* `-jbse_lib`: this must be set to the path of the `jbse.jar` file. You will find one in the `target` directory of the `sushi` submodule.
* `-sushi_lib`: this must be set to the path of the `sushi-lib.jar` file. You will find one in the `target` directory of the `runtime` submodule.
* `-z3`:  the path to the Z3 binary (you can omit it if Z3 is on the system PATH).
* `-tmp_base`: a path to a temporary directory; SUSHI needs to create many files for its intermediate results, and 
will put them in a subdirectory of `-tmp_base` having as name the date and time it was launched.
* `-out`: a path to a directory where the generated tests will be put.

An alternative way to configure SUSHI is to define a subclass of the class `sushi.configure.ParametersModifier` contained 
in the sushi-lib subproject. The subclass should override one or more of the `modify` methods that receive as input a  parameter object, and modify the object
by setting the parameters of interest. In this case SUSHI must be invoked by specifying the following options:

* `-params_modifier_path`: the path where your custom subclass of  `sushi.configure.ParametersModifier` is.
* `-params_modifier_class`: the name of your custom subclass of  `sushi.configure.ParametersModifier`.

You will find examples of this way of configuring SUSHI in the sushi-experiment, sushi-experiment-closure01 and sushi-experiment-closure72 projects.

## Generated tests
The tests are generated in EvoSuite format: Each suite produces two classes,
one scaffolding class and the suite containing all the test cases. SUSHI
will produce many suites each containing exactly one test, so if SUSHI
generates, e.g., 10 test cases then in the directory indicated with the 
`-out` command line parameter you will find 10 scaffolding classes and
10 test suites with 1 test case each. Note that you do *not* need the 
scaffolding to compile and run the test suites, but the test suites 
depend on junit and on the EvoSuite jar. You can safely remove the 
latter dependency by manually editing the generated files, otherwise
you need to put the EvoSuite jar included with this distribution in 
the classpath when compiling the generated test suites.

The generated files have names structured as follows:
    
    <class name>_<method name>_PC_<number>_<number>_Test_scaffolding.java //the scaffolding
    <class name>_<method name>_PC_<number>_<number>_Test.java             //the suite

where `<class name>` is the name of the class under test, `<method name>` is the name
of the method under test, and the `PC_<number>_<number>` identifies the trace along 
which the test executes (you don’t need this information so we will not elaborate
on it further). Note that the scaffolding and test suite classes are declared in the 
same package as the class under test, so they can access the package-level
members of the class under test. This means, for example, that a generated .java files 
for an `avl_tree.AvlTree` class under test, if you have specified the option 
`-out /your/out/dir`, could be something like `/your/out/dir/avl_tree/AvlTree_findMax_PC_2_1_Test.java`. 
If you want to compile and execute the test suites add the right
root to the classpath and qualify the class name of the test suite with the 
package name, e.g.:


    $ javac -cp junit.jar:evosuite-shaded-1.0.3.jar:avltree.jar
        /your/out/dir/avl_tree/AvlTree_findMax_PC_2_1_Test.java
    $ java -cp junit.jar:evosuite-shaded-1.0.3.jar:avltree.jar:/your/out/dir
        org.junit.runner.JUnitCore avl_tree.AvlTree_findMax_PC_2_1_Test

## Disclaimer

SUSHI is a research prototype. As such, it is more focused on functionality than on usability. We are committed to progressively improve the situation.
