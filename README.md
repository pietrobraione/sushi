# SUSHI

## About

SUSHI is an automatic test case generator for Java programs, aimed at achieving high branch coverage. It leverages a technique called symbolic execution, performed with the symbolic executor [JBSE](http://pietrobraione.github.io/jbse/), to calculate from a program path that reaches a branch a *path constraint*, i.e., a constraint on the program inputs that, when satisfied, allows to cover the path. To solve path constraints and generate the corresponding tests SUSHI transforms path constraints in search problems and feeds by them the search-based test case generator [EvoSuite](http://www.evosuite.org/).

The main advantage of SUSHI is its ability of generating test inputs including complex data structures This allows, for instance, to automatically test compiler passes that take parse trees as inputs.

## Installing SUSHI

Right now SUSHI can be installed only by building it from source. Formal releases will be available when SUSHI will be more feature-ready and stable.

## Building SUSHI

SUSHI is composed by several projects, some of which are imported as git submodules, and is built with Gradle version 6.7.1. First, ensure that all the dependencies are present, including Z3, GLPK and GLPK-Java (see section "Dependencies"). Then, clone the SUSHI git repository and init/update its submodules. If you work from the command line, this means running first `git clone`, and then `git submodule init && git submodule update`. Next, patch the build.gradle files to match the installed GLPK-Java, as explained in the section "Patching the build scripts". You shall also follow the instructions described in the section "Patching the tests" of the README.md file of the JBSE subproject. Finally, run the build Gradle task, e.g. by invoking `gradlew build` from the command line.

## Dependencies

SUSHI has many dependencies. It must be built using a JDK version 8 - neither less, nor more. We suggest to use the latest [AdoptOpenJDK](https://adoptopenjdk.net/) v8 with HotSpot JVM (note that the JDK with the OpenJ9 JVM currently does not work, because there are some slight differences in the standard library classes). The Gradle wrapper `gradlew` included in the repository will take care to select the right version of Java. Gradle will automatically resolve and use the following compile-time-only dependencies:

* [JavaCC](https://javacc.org) is used in the JBSE submodule for compiling the parser for the JBSE settings files.
* [JUnit](http://junit.org) is used in the JBSE submodule for running the test suite that comes with JBSE (in future SUSHI might come with a test suite of its own).

The runtime dependencies that are automatically resolved by Gradle and included in the build path are:

* The `tools.jar` library, that is part of every JDK 8 setup (note, *not* of the JRE).
* [Javassist](http://jboss-javassist.github.io/javassist/), that is used by JBSE for all the bytecode manipulation tasks. The patched version of Javassist that is distributed with JBSE is necessary, and TARDIS will not work with an upstream Javassist version.
* [args4j](http://args4j.kohsuke.org/), that is used by SUSHI to process command line arguments.
* The Java wrapper to the linear constraint solver GLPK (more on this later).
* [ojAlgo](https://www.ojalgo.org/), that is used by SUSHI in alternative to GLPK to solve linear constraints.

Another runtime dependency that is included in the git project is:

* [EvoSuite](http://www.evosuite.org/); SUSHI depends on a customized version of EvoSuite that can be found in the `evosuite` subdirectory. It will not work with the upstream EvoSuite versions that you can download from the EvoSuite web page.

There are two additional runtime dependencies that are not handled by Gradle so you will need to fix them manually. 

* JBSE needs to interact with an external numeric solver for pruning infeasible program paths. At the purpose SUSHI requires to use [Z3](https://github.com/Z3Prover/z3), that is a standalone binary and can be installed almost everywhere.
* SUSHI uses the [GNU Linear Programming Kit (GLPK)](https://www.gnu.org/software/glpk/) and its Java wrapper [GLPK-Java](http://glpk-java.sourceforge.net/) to find, among the (many) traces produced by a JBSE run, a minimal subset that still covers all the coverage objectives. Both unfortunately have a native part. If your operating system is Debian or Ubuntu you can install the libglpk and libglpk-java packages. Under OSX there is a GLPK package under Macports, but no package for GLPK-Java. In the worst case you will need to install GLPK and/or GLPK-Java from sources: In such case consider that both have many other dependencies on their own. Refer to the GLPK and GLPK-Java pages for instructions on how to install these packages from sources.

Finally, two runtime dependencies that are not currently used by SUSHI at runtime, but might be in future, are:

* [JaCoCo](http://www.eclemma.org/jacoco/), that is used by the coverage calculator included in SUSHI-Lib.
* [ASM](http://asm.ow2.org/), that is a transitive dependency used by JaCoCo.

Gradle will download them to compile SUSHI-Lib, but you can avoid to deploy them.

## Patching the build scripts

Once installed GLPK-Java, you will (possibly) need to modify the `build.gradle` file in the `master` subdirectory. 

* The provided `master/build.gradle` file instructs Gradle to use version 1.12.0 of the GLPK-Java jar file, but the jar file's version must match the version of the GLPK-Java native library that you installed. For this reason, if it happens that on your development machine you have a version of GLPK-Java different from 1.12.0 you must edit the `master/build.gradle` file as follows: Find the `def glpkVersion = ...` statement in the file and replace the version number on the right-hand side of the assignment with the version number of the GLPK-Java you installed on your platform. 
* The `master/build.gradle` file must also be set with a path to the directory where the native part of GLPK-Java is found. This is not indispensable for building SUSHI, but it is for running it, especially under Eclipse. The provided path is `/usr/local/lib/jni`, thus in the case (notably under Windows and Ubuntu) your actual setup path differs from this default you must edit the `def glpkJniLocation = ...` statement in the file and put the correct path on the right-hand side of the assignment. The GLPK-Java documentation will tell you where the native files are installed. 

## Working under Eclipse

If you want to build and modify JBSE by using (as we do) Eclipse 2021-03 for Java Developers, you are lucky: All the Eclipse plugins that are necessary to import and build SUSHI are already present in the distribution. The only caveat is that, since starting from version 2020-09 Eclipse requires at least Java 11 to run, your development machine will need to have both a Java 11 (to run Eclipse) and a Java 8 setup (to build JBSE). Gradle will automatically select the right version of the JDK when building SUSHI. If you use a different flavor, or an earlier version, of Eclipse you might need to install the egit and the Buildship plugins, both available from the Eclipse Marketplace. After that, to import SUSHI under Eclipse follow these steps:

* To avoid conflicts we advise to import SUSHI under an empty workspace.
* Be sure that the default Eclipse JRE is the JRE subdirectory of a full JDK 8 setup, *not* a standalone (i.e., not part of a JDK) JRE.
* SUSHI and JBSE use the reserved `sun.misc.Unsafe` class, a thing that Eclipse forbids by default. To avoid Eclipse complaining about that you must modify the workspace preferences as follows: From the main menu choose Eclipse > Preferences... under macOS, or Window > Preferences... under Windows and Linux. On the left panel select Java > Compiler > Errors/Warnings, then on the right panel open the option group "Deprecated and restricted API", and for the option "Forbidden reference (access rules)" select the value "Warning" or "Info" or "Ignore".
* Switch to the Git perspective. If you cloned the Github SUSHI repository and the submodules from the command line, you can import the clone under Eclipse by clicking under the Git Repositories view the button for adding an existing repository. Otherwise you can clone the  repository by clicking the clone button, again available under the Git Repositories view (remember to tick the box "Clone submodules"). Eclipse does *not* want you to clone the repository under your Eclipse workspace, and instead wants you to follow the standard git convention of putting the git repositories in a `git` subdirectory of your home directory. If you clone the repository from a console, please follow this standard (if you clone the repository from the Git perspective Eclipse will do this for you).
* Switch back to the Java perspective and from the main menu select File > Import... In the Select the Import Wizard window that pops up choose the Gradle > Existing Gradle Project wizard and press the Next button twice. In the Import Gradle Project window that is displayed, enter in the Project root directory field the path to the SUSHI cloned git repository, and then press the Finish button to confirm. Now your workspace should have four Java project named `jbse`, `sushi`, `sushi-lib`, and `sushi-master`.
* Don't forget to apply all the patches described at the beginning of the "Building SUSHI" section.
* Unfortunately the Buildship Gradle plugin is not able to fully configure the imported projects: As a consequence, after the import you will see some compilation errors due to the fact that the JBSE project did not generate some source files yet. Fix the situation by following this procedure: In the Gradle Tasks view double-click on the sushi > build > build task to build all the projects. Then, right-click the jbse project in the Package Explorer, and in the contextual menu that pops up select Gradle > Refresh Gradle Project. After that, you should see no more errors.

In the end, your Eclipse workspace should contain these projects:

* sushi: the container project from which Gradle must be run;
* sushi-master: the bulk of the SUSHI tool; on the filesystem it is in the `master` subdirectory;
* sushi-lib: the [sushi-lib](https://github.com/pietrobraione/sushi-lib) submodule for the run-time library component of SUSHI; on the filesystem it is in the `runtime` subdirectory;
* jbse: JBSE as a submodule; on the filesystem it is in the `jbse` subdirectory.

## Deploying SUSHI

Deploying SUSHI outside the build environment to a target machine is tricky. The `gradlew build` command will produce a SUSHI-Lib jar `runtime/build/libs/sushi-lib-<VERSION>.jar`, the JBSE jars in `jbse/build/libs` (refer to the JBSE project's README file for more information on them), and a jar for the main SUSHI application `master/build/libs/sushi-master-<VERSION>.jar`. You need to deploy them and all their dependencies. Moreover, `gradlew build` will produce an uber-jar `master/build/libs/sushi-master-<VERSION>-shaded.jar`, containing all the runtime dependencies excluded EvoSuite, `tools.jar`, the GLPK-Java jar file, and the native files (Z3 and the native parts of GLPK and GLPK-Java). Deploying based on the uber-jar is easier, but to our experience a setup based on the uber-jar is more crash-prone (on the other hand, using the uber-jar for JBSE is safe). 

Here follows detailed instructions for deploying SUSHI based on the plain jars:

* Deploy Z3, possibly adding the Z3 binary to the system PATH. 
* Deploy the `sushi-master-<VERSION>.jar` and set the Java classpath to point at it.
* Deploy either the `jbse-<version>.jar` or the `jbse-<version>-shaded.jar` and set the Java classpath to point at it.
* Deploy the `sushi-lib-<VERSION>.jar` and set the Java classpath to point at it.
* Deploy the EvoSuite jar contained in the `evosuite` directory. SUSHI executes EvoSuite in a separate process, therefore you do not need to put the EvoSuite jar in the classpath. 
* SUSHI requires a full JDK (not just a JRE) version 8 installed on the platform it runs. Add the `tools.jar` of the JDK 8 installed on the platform to the classpath.
* Deploy the args4j jar that you find in the Gradle cache. This jar must be in the classpath.
* Deploy the ojAlgo jar that you find in the Gradle cache. This jar must be in the classpath.
* If you do not use the `jbse-<version>-shaded.jar` uber-jar, deploy the Javassist jar that you find in the `jbse/libs` directory. This jar must be in the classpath.
* Deploy GPLK and GLPK-Java, ensuring that the version of GLPK-Java you are deploying is the same used during compilation. Then, set the Java native library path to point to the directory where the native libraries of GLPK-Java are installed, either by providing the `-Djava.library.path=...` option when launching SUSHI, or (under any UNIX-like system) by setting the environment variable `LD_LIBRARY_PATH`. Also, set the classpath to point at the GLPK-Java jar file.

If you deploy the `sushi-master-<VERSION>-shaded.jar` uber-jar you do not need to deploy the JBSE, SUSHI-Lib, args4j, ojAlgo and Javassist jars.

## Usage

Compile the target program with the debug symbols, then launch SUSHI from the command line as follows:

    $ java -Xms16G -Xmx16G -cp <classpath> -Djava.library.path=<nativeLibraryPath> sushi.Main <options>

where `<classpath>` and `<nativeLibraryPath>` must be set according to the indications of the previous section. If you launch SUSHI without options it will print a help screen that lists all the available options. The indispensable ones, that you *must* set in order for SUSHI to work, are:

* `-java8_home`: the path to the home directory of a Java 8 full JDK setup, in case the default JDK installed on the deploy platform should be overridden. If this parameter is not provided, TARDIS will try with the default JDK installed on the deploy platform.
* `-evosuite`: the path to the EvoSuite jar file contained in the `evosuite/` folder.
* `-jbse_lib`: this must be set to the path of the JBSE jar file from the `jbse/build/libs` directory. It must be the same you put in the classpath. If you chose to deploy the `sushi-master-<VERSION>-shaded.jar` uber-jar, set this option to point to it.
* `-sushi_lib`: this must be set to the path of the SUSHI-Lib jar file from the `runtime/build/libs` directory. If you chose to deploy the `sushi-master-<VERSION>-shaded.jar` uber-jar, set this option to point to it.
* `-z3`:  the path to the Z3 binary.
* `-classes`: a colon- or semicolon-separated (depending on the OS) list of paths; It is the classpath of the software under test.
* `-target_class`: the name in [internal classfile format](https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.2.1) of the class to test: SUSHI will generate tests for all the methods in the class. Or alternatively:
* `-target_method`: the signature of a method to test. The signature is a colon-separated list of: the name of the container class in internal classfile format; the [descriptor](https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.3.3) of the method; the name of the method. You can use the `javap` command, included with every JDK setup, to obtain the internal format signatures of methods: `javap -s my.Class` prints the list of all the methods in `my.Class` with their signatures in internal format.
* `-tmp_base`: a path to a temporary directory; SUSHI needs to create many intermediate files, and will put them in a subdirectory of the one that you specify with this option. The subdirectory will have as name the date and time when SUSHI was launched.
* `-out`: a path to a directory where SUSHI will put the generated tests.
* `-evosuite_no_dependency`: when active, the generated test classes will not depend on the EvoSuite jar (i.e., no scaffolding class will be generated).

An alternative way to configure SUSHI is to define a subclass of the class `sushi.configure.ParametersModifier` contained in the runtime subproject. The subclass should override one or more of the `modify` methods that receive as input a  parameter object, and modify the object by setting the parameters of interest. In this case SUSHI must be invoked by specifying the following options:

* `-params_modifier_path`: the path where your custom subclass of  `sushi.configure.ParametersModifier` is.
* `-params_modifier_class`: the name of your custom subclass of  `sushi.configure.ParametersModifier`.

You will find examples of this way of configuring SUSHI in the [sushi-experiments](https://github.com/pietrobraione/sushi-experiments), [sushi-experiments-closure01](https://github.com/pietrobraione/sushi-experiments-closure01) and [sushi-experiments-closure72](https://github.com/pietrobraione/sushi-experiments-closure72) projects. A possible example of command line is the following:

    java -Xms16G -Xmx16G -cp sushi-master-0.2.0-SNAPSHOT.jar:sushi-lib-0.2.0-SNAPSHOT.jar:jbse-0.9.0-SNAPSHOT-shaded.jar:args4j-2.32.jar:/usr/lib/jvm/java-8-openjdk-amd64/lib/tools.jar:/usr/share/java/glpk-java.jar  -Djava.library.path=/usr/lib/jni sushi.Main -jbse_lib jbse-0.9.0-SNAPSHOT-shaded.jar -sushi_lib sushi-lib-0.2.0-SNAPSHOT.jar -evosuite evosuite-shaded-1.0.6-SNAPSHOT.jar -use_mosa -z3 /opt/local/bin/z3 -classes ./my-application/bin -target_class my/Class -tmp_base ./tmp -out ./tests
    
In the case you prefer (at your own risk) to use the SUSHI uber-jar the command line becomes a bit (but not that much) shorter:

    java -Xms16G -Xmx16G -cp sushi-master-0.2.0-SNAPSHOT-shaded.jar:/usr/lib/jvm/java-8-openjdk-amd64/lib/tools.jar:/usr/share/java/glpk-java.jar  -Djava.library.path=/usr/lib/jni sushi.Main -jbse_lib sushi-master-0.2.0-SNAPSHOT-shaded.jar -sushi_lib sushi-master-0.2.0-SNAPSHOT-shaded.jar -evosuite evosuite-shaded-1.0.6-SNAPSHOT.jar -use_mosa -z3 /opt/local/bin/z3 -classes ./my-application/bin -target_class my/Class -tmp_base ./tmp -out ./tests

## Generated tests

The generated tests are in EvoSuite format, where a test suite is composed by two classes: a scaffolding class, and the class containing all the test cases (the actual suite). In case the `-evosuite_no_dependency` option is active, the scaffolding class will not be generated, and the suite will be composed by the actual suite class only: Note, however, that without scaffolding the tests might end up being flaky. SUSHI will produce many suites each containing exactly one test case: If, e.g., a run of SUSHI generates 10 test cases, then in the directory indicated with the `-out` command line parameter you will find 10 scaffolding classes, and 10 actual test suite classes each containing exactly 1 test case. Note that you do *not* need the scaffolding class to compile and run the tests in the test suite classes, but these depend on JUnit and on the EvoSuite jar (they will not depend on the EvoSuite jar when the `-evosuite_no_dependency` option is active). You can safely remove the latter dependency by manually editing the generated files, otherwise you need to put the EvoSuite jar used to generate the tests in the classpath when compiling and running the generated test suites.

The generated files have names structured as follows:
    
    <class name>_<method number>_<trace number>_Test_scaffolding.java //the scaffolding class
    <class name>_<method number>_<trace number>_Test.java             //the actual suite class

where `<class name>` is the name of the class under test, `<method number>` is a number identifying the method under test, and `<trace number>` identifies the trace along which the test executes. To associate a method number with the corresponding method under test open the generated actual suite source file, e.g., `AvlTree_2_1_Test.java`: On top of the file you will find a comment line starting with the words "Covered goal", e.g., `//Covered goal: avl_tree.AvlTree.findMax()I:`. The method signature you find in the comment is the signature of the method under test (`findMax` in the example). Alternatively, go to the temporary directory of the SUSHI run that generated the tests, and look for the file `branches_<method number>.txt` (`branches_2.txt` in our example). The first line of the file contains the signature of the method under test. 

The generated scaffolding/actual suite classes are declared in the same package as the class under test, so they can access its package-level members. This means, for example, that if you have specified the option `-out /your/out/dir`, an `avl_tree.AvlTree` class under test will produce a test `/your/out/dir/avl_tree/AvlTree_2_1_Test.java`. If you want to compile and execute the test suites add the output directory to the classpath and qualify the class name of the test suite with the package name, e.g.:

    $ javac -cp junit.jar:evosuite-shaded-1.0.6-SNAPSHOT.jar:avltree.jar
        /your/out/dir/avl_tree/AvlTree_2_1_Test.java
    $ java -cp junit.jar:evosuite-shaded-1.0.6-SNAPSHOT.jar:avltree.jar:/your/out/dir
        org.junit.runner.JUnitCore avl_tree.AvlTree_2_1_Test

## Disclaimer

SUSHI is a research prototype. As such, it is more focused on functionality than on usability. We are committed to progressively improving the situation.
