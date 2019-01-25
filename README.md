# SUSHI

## About

SUSHI is an automatic test case generator for Java programs, aimed at (high) branch coverage. It leverages a technique called symbolic execution to calculate, from a program path that reaches a branch, a *path constraint*, i.e., a constraint on the program inputs that, when satisfied, allows to cover the path. SUSHI calculates path constraints by exploiting the symbolic executor [JBSE](http://pietrobraione.github.io/jbse/). To solve path constraints SUSHI transforms them in search problems and feeds by them the search-based test case generator [EvoSuite](http://www.evosuite.org/).

The main advantage of SUSHI is its ability of generating tests for programs that take as inputs complex data structures, allowing, for instance, to automatically test compiler passes taking parse trees as inputs.

## Installing SUSHI

Right now SUSHI can be installed only by building it from source. Formal releases will be available when SUSHI will be more feature-ready and stable.

## Building SUSHI

SUSHI is composed by several projects, some of which are imported as git submodules, and is built with Gradle. First, you should ensure that all the dependencies are present, including Z3, GLPK and GLPK-Java (see section "Dependencies"). Then, you should clone the SUSHI git repository and init/update its submodules. If you work from the command line, this means running first `git clone`, and then `git submodule init && git submodule update`. Next, you may need to patch the build.gradle files to match the installed GLPK-Java, as explained in the section "Patching the build scripts". Once done all this, run the build Gradle task by invoking `gradlew build`. 

## Dependencies

SUSHI has a lot of dependencies. It must be built using a JDK version 8 - neither less, nor more. The Gradle wrapper `gradlew` included in the repository will take care to select the right version of Gradle. Gradle will automatically resolve and use the following compile-time-only dependencies:

* [JavaCC](https://javacc.org) is used in the JBSE submodule for compiling the parser for the JBSE settings files.
* [JUnit](http://junit.org) is used in the JBSE submodule for running the test suite that comes with JBSE (in future SUSHI might come with a test suite of its own).

The runtime dependencies that are automatically resolved by Gradle and included in the build path are:

* The `tools.jar` library, that is part of every JDK 8 setup (note, *not* of the JRE).
* [Javassist](http://jboss-javassist.github.io/javassist/), that is used by JBSE for all the bytecode manipulation tasks.
* [JaCoCo](http://www.eclemma.org/jacoco/), that is used by the coverage calculator included in SUSHI-Lib.
* [ASM](http://asm.ow2.org/), that is a transitive dependency used by JaCoCo.
* [args4j](http://args4j.kohsuke.org/), that is used by SUSHI to process command line arguments.
* The jar part of GLPK (more on this later).

Another runtime dependency that is included in the git project is:

* [EvoSuite](http://www.evosuite.org/); SUSHI depends on customized versions of EvoSuite that can be found in the `evosuite` subdirectory. It will not work with the EvoSuite jars that you can download from the EvoSuite web page.

There are two additional runtime dependencies that are not handled by Gradle so you will need to fix them manually. 

* JBSE needs to interact with an external numeric solver for pruning infeasible program paths. At the purpose SUSHI requires to use [Z3](https://github.com/Z3Prover/z3), that is a standalone binary and can be installed almost everywhere.
* SUSHI uses the [GNU Linear Programming Kit (GLPK)](https://www.gnu.org/software/glpk/) and its Java wrapper [GLPK-Java](http://glpk-java.sourceforge.net/) to find, among the (many) traces produced by a JBSE run, a minimal subset that still covers all the coverage objectives. Both unfortunately have a native part. If your operating system is Debian or Ubuntu you can install the libglpk and libglpk-java packages. Under OSX there is a GLPK package under Macports, but no package for GLPK-Java. In the worst case you will need to install GLPK and/or GLPK-Java from sources: In such case consider that both have many other dependencies on their own. Refer to the GLPK and GLPK-Java pages for instructions on how to install these packages from sources.

## Patching the build scripts

Once installed GLPK-Java, you will (possibly) need to modify the `build.gradle` file in the `master` subdirectory. 

* The provided `master/build.gradle` file instructs Gradle to use version 1.11.0 of the GLPK-Java jar file, but the jar file's version must match the version of the GLPK-Java native library that you installed. This is a good reason to install GLPK-Java 1.11.0, but if it happens that you want to install a different version you must edit the `master/build.gradle` file as follows: Find the `def glpkVersion = ...` statement in the file and replace the version number on the right-hand side of the assignment with the version number of the GLPK-Java you installed on your platform. 
* The `master/build.gradle` file must also be set with a path to the directory where the native part of GLPK-Java is found. This is not indispensable for building SUSHI, but it is for running it, especially under Eclipse. The provided path is `/usr/local/lib/jni`, thus in the case (notably under Windows) your actual setup path differs from this default you must edit the `def glpkJniLocation = ...` statement in the file and put the correct path on the right-hand side of the assignment. The GLPK-Java documentation will tell you where the native files are installed. 

## Working under Eclipse

If you want to work (as us) under Eclipse 2018-12 for Java Developers, you are lucky: All the plugins that are necessary to import SUSHI under Eclipse and make it work are already present in the distribution. If you use another version of Eclipse you must install the egit and the Buildship plugins, both available in the Eclipse Marketplace. After that, you are ready to import SUSHI under Eclipse:

* To avoid conflicts we advise to import SUSHI under an empty workspace.
* Be sure that the default Eclipse JRE is the JRE subdirectory of a full JDK 8 setup, *not* a standalone (i.e., not part of a JDK) JRE.
* SUSHI and JBSE use the reserved `sun.misc.Unsafe` class, a thing that Eclipse forbids by default. To avoid Eclipse complaining about that you must modify the workspace preferences as follows: From the main menu choose Eclipse > Preferences... under macOS, or Window > Preferences... under Windows and Linux. On the left panel select Java > Compiler > Errors/Warnings, then on the right panel open the option group "Deprecated and restricted API", and for the option "Forbidden reference (access rules)" select the value "Warning" or "Info" or "Ignore".
* Switch to the Git perspective. If you cloned the Github SUSHI repository and the submodules from the command line, you can import the clone under Eclipse by clicking under the Git Repositories view the button for adding an existing repository. Otherwise you can clone the  repository by clicking the clone button, again available under the Git Repositories view (remember to tick the box "Clone submodules"). Eclipse does *not* want you to clone the repository under your Eclipse workspace, and instead wants you to follow the standard git convention of putting the git repositories in a `git` subdirectory of your home directory. If you clone the repository from a console, please follow this standard (if you clone the repository from the Git perspective Eclipse will do this for you).
* Switch back to the Java perspective and from the main menu select File > Import... In the Select the Import Wizard window that pops up choose the Gradle > Existing Gradle Project wizard and press the Next button twice. In the Import Gradle Project window that is displayed, enter in the Project root directory field the path to the SUSHI cloned git repository, and then press the Finish button to confirm. Now your workspace should have four Java project named `jbse`, `sushi`, `sushi-lib`, and `sushi-master`.
* Unfortunately the Buildship Gradle plugin is not able to fully configure the imported projects: As a consequence, after the import you will see some compilation errors due to the fact that the JBSE project did not generate some source files yet. Fix the situation by following this procedure: In the Gradle Tasks view double-click on the sushi > build > build task to build all the projects. Then, right-click the jbse project in the Package Explorer, and in the contextual menu that pops up select Gradle > Refresh Gradle Project. After that, you should see no more errors.

In the end, your Eclipse workspace should contain these projects:

* sushi: the container project from which Gradle must be run;
* sushi-master: the bulk of the SUSHI tool; on the filesystem it is in the `master` subdirectory;
* sushi-lib: the [sushi-lib](https://github.com/pietrobraione/sushi-lib) submodule for the run-time library component of SUSHI; on the filesystem it is in the `runtime` subdirectory;
* jbse: JBSE as a submodule; on the filesystem it is in the `jbse` subdirectory.

## Deploying SUSHI

Deploying SUSHI to be used outside Eclipse is tricky. The `gradlew build` command will produce a SUSHI-Lib jar `runtime/build/libs/sushi-lib-<VERSION>.jar`, the JBSE jars in `jbse/build/libs` (refer to the JBSE project's README file for more information on them), and a jar for the main SUSHI application `master/build/libs/sushi-master-<VERSION>.jar`. You may deploy them and all the missing dependencies, if you feel adventurous. However, `gradlew build` will also produce an uber-jar `master/build/libs/sushi-<VERSION>-shaded.jar`, containing all the runtime dependencies excluded EvoSuite, `tools.jar`, the GLPK-Java jar file, and of course the native files (Z3 and the native parts of GLPK and GLPK-Java). Deploying based on the uber-jar currently is the easiest way for deploying SUSHI. Moreover, although JBSE and SUSHI-Lib are already included in the SUSHI uber-jar, you will need to deploy the JBSE and SUSHI-Lib jars. The instruction for deploying based on the uber-jar plus these six dependencies are the following ones:

* You can put the SUSHI uber-jar anywhere: Just set the Java classpath to point at it.
* Deploying Z3 is very easy: Just put the Z3 binary directory somewhere, and add the Z3 binary to the system PATH, or use the `-z3` option when invoking SUSHI to point at it. 
* Deploying EvoSuite is similarly easy: Put the right EvoSuite jar somewhere, and then use the `-evosuite` option when invoking SUSHI to point at it. Since SUSHI executes EvoSuite in a separate process, you do not need to put the EvoSuite jar in the classpath. 
* SUSHI will not run if you deploy it on a machine that has a JRE, instead of a JDK, installed. This because SUSHI needs to invoke the platform's `javac` to compile some intermediate files. Therefore, you need to install a full JDK 8 on the target machine, providing both `tools.jar` and `javac` to SUSHI. Add `tools.jar` to the classpath, if it is not already in it by default.
* Deploy GPLK and GLPK-Java, ensuring that the version of GLPK-Java you are deploying is the same used during compilation. Then, set the Java native library path to point to the directory where the native libraries of GLPK-Java are installed, either by providing the `-Djava.library.path=...` option when launching SUSHI, or (under any UNIX-like system) by setting the environment variable `LD_LIBRARY_PATH`. Also, set the classpath to point at the GLPK-Java jar file.
* Finally, the JBSE and SUSHI-Lib jars need not to be on the classpath (they are included in the SUSHI uber-jar, that is already in the classpath), but the path to them must be passed to SUSHI through the `-jbse_lib` and `-sushi_lib` options. 

## Usage

You can launch SUSHI as follows:

    $ java -cp <classpath> -Djava.library.path=<nativeLibraryPath> sushi.Main <options>

where `<classpath>` and `<nativeLibraryPath>` must be set according to the indications of the previous section. If you launch SUSHI without options it will print a help screen that lists all the available options. The indispensable ones, that you *must* set in order for SUSHI to work, are:

* `-classes`: a colon- or semicolon-separated (depending on the OS) list of paths; It is the classpath of the software under test.
* `-target_class`: the name in [internal classfile format](https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.2.1) of the class to test: SUSHI will generate tests for all the methods in the class. Or alternatively:
* `-target_method`: the signature of a method to test. The signature is a colon-separated list of: the name of the container class in internal classfile format; the [descriptor](https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.3.3) of the method; the name of the method. You can use the `javap` command, included with every JDK setup, to obtain the internal format signatures of methods: `javap -s my.Class` prints the list of all the methods in `my.Class` with their signatures in internal format.
* `-evosuite`: the path to one of the two EvoSuite jar files contained in the `evosuite/` folder. Use `evosuite-shaded-1.0.6-SNAPSHOT.jar` if the option `-use_mosa` is active, otherwise use `evosuite-shaded-1.0.3.jar`.
* `-use_mosa`: configures EvoSuite to use a multi-objective search algorithm (MOSA). You usually want this option to be active, since it makes SUSHI faster in most cases.
* `-jbse_lib`: this must be set to the path of the JBSE jar file. You will find one in the `jbse/build/libs` directory.
* `-sushi_lib`: this must be set to the path of the SUSHI-Lib jar file. You will find one in the `runtime/build/libs` directory.
* `-z3`:  the path to the Z3 binary (you can omit it if Z3 is on the system PATH).
* `-tmp_base`: a path to a temporary directory; SUSHI needs to create many intermediate files, and will put them in a subdirectory of `-tmp_base` having as name the date and time when it was launched.
* `-out`: a path to a directory where SUSHI will put the generated tests.

An alternative way to configure SUSHI is to define a subclass of the class `sushi.configure.ParametersModifier` contained in the runtime subproject. The subclass should override one or more of the `modify` methods that receive as input a  parameter object, and modify the object by setting the parameters of interest. In this case SUSHI must be invoked by specifying the following options:

* `-params_modifier_path`: the path where your custom subclass of  `sushi.configure.ParametersModifier` is.
* `-params_modifier_class`: the name of your custom subclass of  `sushi.configure.ParametersModifier`.

You will find examples of this way of configuring SUSHI in the [sushi-experiments](https://github.com/pietrobraione/sushi-experiments), [sushi-experiments-closure01](https://github.com/pietrobraione/sushi-experiments-closure01) and [sushi-experiments-closure72](https://github.com/pietrobraione/sushi-experiments-closure72) projects.

## Generated tests

The tests are generated in EvoSuite format, where a test suite is composed by two classes: a scaffolding class, and the class containing all the test cases (the actual suite). SUSHI will produce many suites each containing exactly one test case: If, e.g., a run of SUSHI generates 10 test cases, then in the directory indicated with the `-out` command line parameter you will find 10 scaffolding classes, and 10 actual test suite classes each containing exactly 1 test case. Note that you do *not* need the scaffolding class to compile and run the tests in the test suite classes, but these depend on junit and on the EvoSuite jar. You can safely remove the latter dependency by manually editing the generated files, otherwise you need to put the EvoSuite jar used to generate the tests in the classpath, when compiling and running the generated test suites.

The generated files have names structured as follows:
    
    <class name>_<method number>_<trace number>_Test_scaffolding.java //the scaffolding class
    <class name>_<method number>_<trace number>_Test.java             //the actual suite class

where `<class name>` is the name of the class under test, `<method number>` is a number identifying the method under test, and `<trace number>` identifies the trace along which the test executes. To associate a method number with the corresponding method under test open the generated actual suite source file, e.g., `AvlTree_2_1_Test.java`: On top of the file you will find a comment line starting with the words "Covered goal", e.g., `//Covered goal: avl_tree.AvlTree.findMax()I:`. The method signature you find in the comment is the signature of the method under test (`findMax` in the example). Alternatively, go to the temporary directory of the SUSHI run that generated the tests, and look for the file `branches_<method number>.txt` (`branches_2.txt` in our example). The first line of the file contains the signature of the method under test. 

The generated scaffolding/actual suite classes are declared in the same package as the class under test, so they can access its package-level members. This means, for example, that the generated .java file for an `avl_tree.AvlTree` class under test, if you have specified the option `-out /your/out/dir`, will be put in `/your/out/dir/avl_tree/AvlTree_2_1_Test.java`. If you want to compile and execute the test suites add the output directory to the classpath and qualify the class name of the test suite with the package name, e.g.:

    $ javac -cp junit.jar:evosuite-shaded-1.0.3.jar:avltree.jar
        /your/out/dir/avl_tree/AvlTree_2_1_Test.java
    $ java -cp junit.jar:evosuite-shaded-1.0.3.jar:avltree.jar:/your/out/dir
        org.junit.runner.JUnitCore avl_tree.AvlTree_2_1_Test

## Disclaimer

SUSHI is a research prototype. As such, it is more focused on functionality than on usability. We are committed to progressively improving the situation.
