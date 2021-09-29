FROM ubuntu:20.04
MAINTAINER Pietro Braione <pietro.braione@gmail.com>

# Setup base image 
RUN apt-get update -y
RUN apt-get install -y openjdk-8-jdk
RUN apt-get install -y unzip
RUN apt-get install -y nano
RUN apt-get install -y git
RUN apt-get install -y z3
RUN apt-get install -y libglpk40
RUN apt-get install -y libglpk-java
RUN rm -rf /var/lib/apt/lists/*

# Setup environment variables
ENV HOME /root
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64
ENV JARS_HOME /usr/share/java
ENV JNI_HOME /usr/lib/x86_64-linux-gnu/jni
ENV Z3_HOME /usr/bin
ENV CLASSPATH ${JAVA_HOME}/lib/tools.jar:${JARS_HOME}/glpk-java.jar:${JARS_HOME}/jbse-0.10.0-SNAPSHOT-shaded.jar:${JARS_HOME}/sushi-master-0.2.0-SNAPSHOT.jar:${JARS_HOME}/args4j-2.32.jar:${JARS_HOME}/ojalgo-48.0.0.jar:${JARS_HOME}/sushi-lib-0.2.0-SNAPSHOT.jar:${JARS_HOME}/asm-debug-all-5.0.1.jar:${JARS_HOME}/org.jacoco.core-0.7.5.201505241946.jar

# Build and install
WORKDIR ${HOME}
RUN git clone https://github.com/pietrobraione/sushi
WORKDIR ${HOME}/sushi
RUN git submodule init && git submodule update
RUN ln --symbolic ${GLPK_JNI_HOME} /usr/local/lib/jni
RUN ./gradlew build
RUN cp jbse/build/libs/jbse-0.10.0-SNAPSHOT-shaded.jar ${JARS_HOME}/.
RUN cp master/build/libs/sushi-master-0.2.0-SNAPSHOT.jar ${JARS_HOME}/.
RUN cp master/deps/args4j-2.32.jar ${JARS_HOME}/.
RUN cp master/deps/ojalgo-48.0.0.jar ${JARS_HOME}/.
RUN cp runtime/build/libs/sushi-lib-0.2.0-SNAPSHOT.jar ${JARS_HOME}/.
RUN cp runtime/deps/asm-debug-all-5.0.1.jar ${JARS_HOME}/.
RUN cp runtime/deps/org.jacoco.core-0.7.5.201505241946.jar ${JARS_HOME}/.
RUN cp libs/evosuite-shaded-1.1.1-SNAPSHOT.jar ${JARS_HOME}/.

# Create script
RUN echo "#!/bin/sh" > /usr/local/bin/sushi
RUN echo "java -Xms16G -Xmx16G -cp ${CLASSPATH} -Djava.library.path=${JNI_HOME} sushi.Main -evosuite ${JARS_HOME}/evosuite-shaded-1.1.1-SNAPSHOT.jar -jbse_lib ${JARS_HOME}/jbse-0.10.0-SNAPSHOT-shaded.jar -sushi_lib ${JARS_HOME}/sushi-lib-0.2.0-SNAPSHOT.jar -z3 ${Z3_HOME}/z3 \$@" >> /usr/local/bin/sushi
RUN chmod +x /usr/local/bin/sushi

# Get some examples and compile them
WORKDIR ${HOME}
RUN git clone https://github.com/pietrobraione/sushi-experiments
WORKDIR ${HOME}/sushi-experiments
RUN mkdir bin
RUN javac -cp ${CLASSPATH}:${HOME}/sushi-experiments/bin -d bin -g src/common/*.java
RUN javac -cp ${CLASSPATH}:${HOME}/sushi-experiments/bin -d bin -g src/avl/*.java
RUN javac -cp ${CLASSPATH}:${HOME}/sushi-experiments/bin -d bin -g src/dll_hard/*.java
RUN javac -cp ${CLASSPATH}:${HOME}/sushi-experiments/bin:${HOME}/sushi-experiments/libs/ganttproject-guava.jar -d bin -g src/ganttproject/*.java
RUN javac -cp ${CLASSPATH}:${HOME}/sushi-experiments/bin -d bin -g src/ncll/*.java
RUN javac -cp ${CLASSPATH}:${HOME}/sushi-experiments/bin -d bin -g src/treemap/*.java
RUN javac -cp ${CLASSPATH}:${HOME}/sushi-experiments/bin -d bin -g src/tsafe/*.java
RUN javac -cp ${CLASSPATH}:${HOME}/sushi-experiments/bin -d bin sushi-src/common/*.java
RUN javac -cp ${CLASSPATH}:${HOME}/sushi-experiments/bin -d bin sushi-src/avl/*.java
RUN javac -cp ${CLASSPATH}:${HOME}/sushi-experiments/bin -d bin sushi-src/dll_hard/*.java
RUN javac -cp ${CLASSPATH}:${HOME}/sushi-experiments/bin -d bin sushi-src/ganttproject/*.java
RUN javac -cp ${CLASSPATH}:${HOME}/sushi-experiments/bin -d bin sushi-src/ncll/*.java
RUN javac -cp ${CLASSPATH}:${HOME}/sushi-experiments/bin -d bin sushi-src/treemap/*.java
RUN javac -cp ${CLASSPATH}:${HOME}/sushi-experiments/bin -d bin sushi-src/tsafe/*.java

WORKDIR ${HOME}

