# SUSHI

## About

SUSHI is an automatic test case generator for Java programs, aimed at (high) branch coverage. It leverages a technique called symbolic execution to calculate, from a program path that reaches a branch, a *path constraint*, i.e., a constraint on the program inputs that, when satisfied, allows to cover the path. SUSHI calculates path constraints by exploiting the symbolic executor [JBSE](http://pietrobraione.github.io/jbse/). To solve path constraints SUSHI transforms them in search problems and feeds by them the search-based test case generator [EvoSuite](http://www.evosuite.org/).

The main advantage of SUSHI is its ability of generating tests for programs that take as inputs complex data structures, allowing, for instance, to automatically test compiler passes taking parse trees as inputs.

## Installing SUSHI

Right now SUSHI can be installed only by building it from source. Formal releases will be available when SUSHI will be more feature-ready and stable.

## Building SUSHI

SUSHI is composed by several projects, some of which are imported as git submodules, and is built with Maven. First, you should ensure that all the dependencies are present, including Z3, GLPK and GLPK-Java (see section "Dependencies"). Then, you should clone the SUSHI git repository and init/update its submodules; If you work from the command line, this means running first `git clone`, and then `git submodule init && git submodule update`. Next, you may need to patch the POMs to match the installed GLPK-Java, as explained in the section "Patching the POMs". Once done all this, you can run some Maven goals. The supported Maven goals are:

* clean
* compile
* test
* package

## Dependencies

SUSHI has a lot of dependencies. It must be built using a JDK version 8 - neither less, nor more - and Maven version 3.5.0 or above. Maven will automatically resolve and use the following build-time dependencies:

* [JavaCC](https://javacc.org) is used by Maven for compiling the parser for the JBSE settings files. It is not needed at runtime.
* [JUnit](http://junit.org) is used by Maven for running the tests. It is not needed at runtime.

The runtime dependencies that are automatically resolved by Maven and included in the build path are:

* [args4j](http://args4j.kohsuke.org/), that is used to process command line arguments.
* [JaCoCo](http://www.eclemma.org/jacoco/), that is used by the coverage calculator included in SUSHI-Lib.
* [ASM](http://asm.ow2.org/), that is used by JaCoCo.
* [Javassist](http://jboss-javassist.github.io/javassist/), that is used by JBSE for all the bytecode manipulation tasks.
* The `tools.jar` library, that is part of every JDK 8 setup (note, *not* of the JRE).

Another runtime dependency that is included in the git project is:

* [EvoSuite](http://www.evosuite.org/); SUSHI depends on customized versions of EvoSuite that can be found in the `evosuite` subdirectory.

There are two additional dependencies that are not handled by Maven so you will need to fix them manually. 

* JBSE needs to interact at runtime with an external numeric solver for pruning infeasible program paths. At the purpose SUSHI requires to use [Z3](https://github.com/Z3Prover/z3), that is a standalone binary and can be installed almost everywhere. There is a known bug that prevents JBSE to interact with the solver if it is installed in a path containing spaces. Please don't do that until the bug is fixed.
* SUSHI uses the [GNU Linear Programming Kit (GLPK)](https://www.gnu.org/software/glpk/) and its Java wrapper [GLPK-Java](http://glpk-java.sourceforge.net/) to find a minimal set of traces that cover a number of coverage objectives. Both unfortunately have a native part. If your operating system is Debian or Ubuntu you can install the libglpk and libglpk-java packages. Under OSX there is a GLPK package under Macports, but no package for GLPK-Java. In the worst case you will need to install GLPK and/or GLPK-Java from sources, that have both many other dependencies on their own. Refer to the GLPK and GLPK-Java pages for instructions on how to install these packages from sources.

## Patching the POMs

Once installed GLPK-Java, you will (possibly) need to modify the Maven POM file in the root directory of the SUSHI repository clone, and/or the POM file in its `master` subdirectory, for the following reasons. 

* The POM file in the `master` subdirectory will install by default version 1.10 of the GLPK-Java jar file, but the jar file's version must match the version of the GLPK-Java native library, so if you installed a different version of GLPK-Java you must edit the `master/pom.xml` file as follows: Modify the `<version>` tag of the `org.gnu.glpk:glpk-java` artifact and put the version number of GLPK-Java you installed on your platform. 
* The POM file in the root directory of the SUSHI repository clone must be set with a path to the directory where the native library part of GLPK-Java is found. By default this path is set to `/usr/local/lib/jni`, thus in the case (notably under Windows) the path where the GLPK-Java native libraries are installed differs from this default you must edit the `sushi/pom.xml` file and put the right path in the `<argLine>` tag of the `maven-surefire-plugin` artifact. The GLPK-Java documentation will tell you where this path is. 

## Working under Eclipse

If you work (as us) under Eclipse 2018-09, you must install the egit Eclipse plugin (you will find it in the Eclipse Marketplace), the m2e plugin (also in the Eclipse Marketplace), and the m2e connector for javacc-maven-plugin. The last one must be installed manually: Select the menu Help > Install new software..., click the "Add" button to add a new site, and insert the URL `http://objectledge.github.io/maven-extensions/connectors/updates/development/` in the field "Location" (in the field "Name" you can give any name you want, but we advise to call it "Objectledge Maven extensions update site"). Press "OK", and then select the newly added update site in the "Work with" box. Finally, select "objectledge.org m2e connectors" > "m2e connector for javacc-maven-plugin": The version must be at least 1.2.0.*, or it will not work with the current version of m2e. 

Once done this setup, you are ready to import SUSHI under an Eclipse workspace (to avoid conflicts we advise to import everything under an empty workspace):

* Clone the Github SUSHI repository by switching to the git perspective and selecting the clone button under the Git Repositories view. Remember to tick the box "Clone submodules". Alternatively, open a console and clone the repository from the command line, then init/update the submodules. Remember that Eclipse does *not* want you to clone the repository under your Eclipse workspace, and instead wants you to follow the standard git convention of putting the git repositories in a `git` subdirectory of your home directory. If you clone the repository from a console, please follow this standard (if you do it from the git perspective Eclipse will do this for you).
* Click on the icon of the cloned repository, and right-click the Working Tree folder, then select Import Projects...: You should see a window with a progress bar, and after a while Eclipse will tell you that it has found five different projects, and that it will import them all as Maven projects. Press Finish to confirm, and then switch back to the Java perspective: Now your current workspace should have five Java project named `jbse`, `sushi`, `master`, `runtime` and `shaded`. If you chose to clone the repositories from the console, select from the main menu File > Import... and then select Maven > Existing Maven Projects, press Next and then provide as root directory the directory where you cloned the SUSHI repository: Eclipse will, again, detect all the five projects automatically.
* You may see some compilation errors emerge while Eclipse builds the workspace (it usually takes a while after the JBSE project has been created), but in the end there should be none. It could however be the case that Eclipse generates some compilation errors caused by the fact that some of the generate Eclipse projects use the reserved `sun.misc.Unsafe` class. By default this should generate only warnings, but if it generates errors you shall modify the project settings by hand so it does not. Right-click one of the projects in the Package Explorer that generate the compile-time errors, select Properties, and then Java Compiler > Errors/Warnings. Click on "Enable project specific settings", then open the option group "Deprecated and restricted API", and for the option "Forbidden reference" select the value "Warning". Repeat for all the projects that generate the errors.

A brief description of the Eclipse projects follows:

* sushi: the container project from which Maven must be run;
* jbse: JBSE as a submodule; on the filesystem it is in the `jbse` subdirectory;
* master: the bulk of the SUSHI tool; on the filesystem it is in the `master` subdirectory;
* runtime: the [sushi-lib](https://github.com/pietrobraione/sushi-lib) submodule for the run-time library component of SUSHI; on the filesystem it is in the `runtime` subdirectory;
* shaded: this project produces an uber JAR for the complete SUSHI tool; on the filesystem it is in the `shaded` subdirectory.

The last step to do is to bind in the master Eclipse project the GLPK-Java jar file to the native part of the GLPK-Java library, so you can launch SUSHI from an Eclipse launch configuration. Unfortunately m2e does not do this step for you, so you must do it manually. This last step must be done after you have modified the POMs as explained in the section "Patching the POMs". You must proceed as follows: After patching the POMs right-click on the master project in the Package Explorer, select Maven > Update Project..., select again (if it is the case) the master project in the list that will appear and press OK. This will trigger a refresh of the Maven dependencies. Then, right-click the master Eclipse project in the Package Explorer, and select Build Path > Configure Build Path... from the contextual menu, select the Libraries tab, open the Maven Dependencies group, select the `glpk-java-x.y.z.jar` dependency, click the triangle on the left of it, select Native Library Location, click the Edit button and enter the path where the native GLPK-Java libraries are.

## Deploying SUSHI

Deploying SUSHI to be used outside Eclipse is tricky but feasible with some effort. The `mvn package` command will produce a `master/target/sushi-master-<VERSION>.jar`, a `runtime/target/sushi-lib-<VERSION>.jar`, and the JBSE jars in `jbse/target` (refer to the JBSE project's README file for more information on them). You may deploy them and all the missing dependencies, if you feel adventurous. `mvn package` will also produce an uber-jar `shaded/target/sushi-shaded-<VERSION>.jar`, containing all the runtime dependencies excluded Z3, EvoSuite, `tools.jar` and the native part of GLPK and GLPK-Java. Moreover, you will also need to deploy a JBSE jar and a SUSHI-Lib jar, although they are already included in the SUSHI uber-jar. Deploying the uber-jar plus these six dependencies currently is the easiest way for deploying SUSHI:

* You can deploy the SUSHI uber-jar anywhere: Just set the Java classpath to point to it;
* Deploying Z3 is the easiest part of all: Just put the Z3 binary directory somewhere, and add the Z3 binary to the system PATH, or use the `-z3` option when invoking SUSHI. 
* Deploying EvoSuite is similarly easy: Put the right EvoSuite jar somewhere, and then use the `-evosuite` option when invoking SUSHI to point at it. Since SUSHI executes EvoSuite in a separate process, you do not even need to put the EvoSuite jar on the classpath. 
* For what concerns deploying `tools.jar`, note that SUSHI will not run if you deploy it on a machine that has a JRE, instead of a JDK, installed. This because SUSHI needs to invoke the platform's `javac` to compile some intermediate files. Therefore, you need to install a full JDK 8 on the target machine, providing both `tools.jar` and `javac` to SUSHI. Add `tools.jar` to the classpath, if it is not already in it by default.
* When you deploy the native parts of GLPK and GLPK-Java, be sure that the versions you are deploying are the same used during compilation. Then, set the Java native library path to point to the directory where the native libraries of GLPK-Java are installed.
* Finally, the JBSE and SUSHI-Lib jars need not to be on the classpath (they are included in the SUSHI uber-jar), but the path to them must be passed to SUSHI when it is invoked, through the `-jbse_lib` and `-sushi_lib` options. 

## Usage

You can launch SUSHI as follows:

    $ java -cp <classpath> -Djava.library.path=<nativeLibraryPath> sushi.Main <options>

or:

    $ java -cp <classpath> -Djava.library.path=<nativeLibraryPath> -jar <sushiUberJarPath> <options>
 
where `<classpath>` and `<nativeLibraryPath>` must be set according to the indications of the previous section. If you launch SUSHI without options it will print a help screen that lists all the available options. The indispensable ones, that you *must* set in order for SUSHI to work, are:

* `-classes`: a colon- or semicolon-separated (depending on the OS) list of paths; It is the classpath of the software to test.
* `-target_class`: the name in [internal classfile format](http://docs.oracle.com/javase/specs/jvms/se6/html/ClassFile.doc.html#14757) of the class to test: SUSHI will generate tests for all the methods in the class. Or alternatively:
* `-target_method`: the signature of a method to test. The signature is a colon-separated list of: the name of the container class in internal classfile format; the [descriptor](http://docs.oracle.com/javase/specs/jvms/se6/html/ClassFile.doc.html#1169) of the method; the name of the method. You can use the `javap` command, included with every JDK setup, to obtain the internal format signatures of methods: `javap -s my.Class` prints the list of all the methods in `my.Class` with their signatures in internal format.
* `-evosuite`: the path to one of the two EvoSuite jar files contained in the `evosuite/` folder. Use `evosuite-shaded-1.0.6-SNAPSHOT.jar` if the option `-use_mosa` is active, otherwise use `evosuite-shaded-1.0.3.jar`.
* `-use_mosa`: configures EvoSuite to use a multi-objective search algorithm (MOSA). You usually want this option to be active, since it  makes SUSHI faster in most cases.
* `-jbse_lib`: this must be set to the path of the JBSE jar file. You will find one in the `target` directory of the `sushi` submodule.
* `-sushi_lib`: this must be set to the path of the SUSHI-Lib jar file. You will find one in the `target` directory of the `runtime` submodule.
* `-z3`:  the path to the Z3 binary (you can omit it if Z3 is on the system PATH).
* `-tmp_base`: a path to a temporary directory; SUSHI needs to create many intermediate files, and will put them in a subdirectory of `-tmp_base` having as name the date and time when it was launched.
* `-out`: a path to a directory where SUSHI will put the generated tests.

An alternative way to configure SUSHI is to define a subclass of the class `sushi.configure.ParametersModifier` contained in the runtime subproject. The subclass should override one or more of the `modify` methods that receive as input a  parameter object, and modify the object by setting the parameters of interest. In this case SUSHI must be invoked by specifying the following options:

* `-params_modifier_path`: the path where your custom subclass of  `sushi.configure.ParametersModifier` is.
* `-params_modifier_class`: the name of your custom subclass of  `sushi.configure.ParametersModifier`.

You will find examples of this way of configuring SUSHI in the sushi-experiment, sushi-experiment-closure01 and sushi-experiment-closure72 projects.

## Generated tests

The tests are generated in EvoSuite format, where a test suite is composed by two classes: a scaffolding class, and the class containing all the test cases (the actual suite). SUSHI will produce many suites each containing exactly one test case: If, e.g., a run of SUSHI generates 10 test cases, then in the directory indicated with the `-out` command line parameter you will find 10 scaffolding classes and 10 actual test suite classes each containing exactly 1 test case. Note that you do *not* need the scaffolding class to compile and run the tests in the test suite classes, but these depend on junit and on the EvoSuite jar. You can safely remove the latter dependency by manually editing the generated files, otherwise you need to put the EvoSuite jar used to generate the tests in the classpath, when compiling and running the generated test suites.

The generated files have names structured as follows:
    
    <class name>_<method name>_PC_<number>_<number>_Test_scaffolding.java //the scaffolding class
    <class name>_<method name>_PC_<number>_<number>_Test.java             //the actual suite class

where `<class name>` is the name of the class under test, `<method name>` is the name of the method under test, and the `PC_<number>_<number>` identifies the trace along which the test executes (you don’t need this information so we will not elaborate on it further). The scaffolding and test suite classes are declared in the same package as the class under test, so they can access the package-level members of the class under test. This means, for example, that a generated .java files for an `avl_tree.AvlTree` class under test, if you have specified the option `-out /your/out/dir`, could be something like `/your/out/dir/avl_tree/AvlTree_findMax_PC_2_1_Test.java`. If you want to compile and execute the test suites add the right root to the classpath and qualify the class name of the test suite with the package name, e.g.:

    $ javac -cp junit.jar:evosuite-shaded-1.0.3.jar:avltree.jar
        /your/out/dir/avl_tree/AvlTree_findMax_PC_2_1_Test.java
    $ java -cp junit.jar:evosuite-shaded-1.0.3.jar:avltree.jar:/your/out/dir
        org.junit.runner.JUnitCore avl_tree.AvlTree_findMax_PC_2_1_Test

## Disclaimer

SUSHI is a research prototype. As such, it is more focused on functionality than on usability. We are committed to progressively improve the situation.
