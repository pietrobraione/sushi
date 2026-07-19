FROM ubuntu:26.04
MAINTAINER Pietro Braione <pietro.braione@gmail.com>

# Setup base image 
RUN apt-get update -y
RUN apt-get install -y openjdk-8-jdk
RUN apt-get install -y openjdk-21-jdk
RUN apt-get install -y unzip
RUN apt-get install -y nano
RUN apt-get install -y git
RUN apt-get install -y z3
RUN apt-get install -y libglpk40
RUN apt-get install -y libglpk-java
RUN rm -rf /var/lib/apt/lists/*

# Setup environment variables
ENV HOME=/root
ENV JAVA_HOME_8=/usr/lib/jvm/java-8-openjdk-amd64
ENV JAVA_HOME_21=/usr/lib/jvm/java-21-openjdk-amd64
ENV JAVA_HOME=${JAVA_HOME_21}
ENV JARS_HOME=/usr/share/java
ENV JNI_HOME=/usr/lib/x86_64-linux-gnu/jni
ENV Z3_HOME=/usr/bin
ENV JARS_8=${JARS_HOME}/glpk-java-1.12.0.jar:${JARS_HOME}/jbse-0.12.0-SNAPSHOT-shaded.jar:${JARS_HOME}/sushi-master-0.3.0-SNAPSHOT.jar:${JARS_HOME}/args4j-2.32.jar:${JARS_HOME}/ojalgo-48.0.0.jar:${JARS_HOME}/log4j-api-2.14.0.jar:${JARS_HOME}/log4j-core-2.14.0.jar
ENV CLASSPATH_8=${JAVA_HOME_8}/lib/tools.jar:${JARS_8}

# Build and install
WORKDIR ${HOME}
RUN git clone https://github.com/pietrobraione/sushi
WORKDIR ${HOME}/sushi
RUN git submodule init && git submodule update
RUN ln --symbolic ${GLPK_JNI_HOME} /usr/local/lib/jni
RUN ./gradlew build
RUN cp jbse/build/libs/jbse-0.12.0-SNAPSHOT-shaded.jar ${JARS_HOME}/.
RUN cp master/build/libs/sushi-master-0.3.0-SNAPSHOT.jar ${JARS_HOME}/.
RUN cp master/deps/glpk-java-1.12.0.jar ${JARS_HOME}/.
RUN cp master/deps/args4j-2.32.jar ${JARS_HOME}/.
RUN cp master/deps/ojalgo-48.0.0.jar ${JARS_HOME}/.
RUN cp master/deps/log4j-api-2.14.0.jar ${JARS_HOME}/.
RUN cp master/deps/log4j-core-2.14.0.jar ${JARS_HOME}/.
RUN cp libs/sushi-lib-0.3.0-SNAPSHOT.jar ${JARS_HOME}/.
RUN cp libs/evosuite-shaded-1.2.1-SNAPSHOT.jar ${JARS_HOME}/.

# Create script
RUN echo "#!/bin/sh" > /usr/local/bin/sushi
RUN echo "${JAVA_HOME_8}/bin/java -Xms16G -Xmx16G -cp ${CLASSPATH_8} -Djava.library.path=${JNI_HOME} sushi.Main -evosuite ${JARS_HOME}/evosuite-shaded-1.2.1-SNAPSHOT.jar -jbse_lib ${JARS_HOME}/jbse-0.12.0-SNAPSHOT-shaded.jar -sushi_lib ${JARS_HOME}/sushi-lib-0.3.0-SNAPSHOT.jar -z3 ${Z3_HOME}/z3 \$@" >> /usr/local/bin/sushi
RUN chmod +x /usr/local/bin/sushi

# Get some examples and compile them
WORKDIR ${HOME}
RUN git clone https://github.com/pietrobraione/sushi-experiments
WORKDIR ${HOME}/sushi-experiments
RUN mkdir bin
RUN ${JAVA_HOME_8}/bin/javac -cp ${CLASSPATH_8}:${HOME}/sushi-experiments/bin -d bin -g src/common/*.java
RUN ${JAVA_HOME_8}/bin/javac -cp ${CLASSPATH_8}:${HOME}/sushi-experiments/bin -d bin -g src/avl/*.java
RUN ${JAVA_HOME_8}/bin/javac -cp ${CLASSPATH_8}:${HOME}/sushi-experiments/bin -d bin -g src/dll_hard/*.java
RUN ${JAVA_HOME_8}/bin/javac -cp ${CLASSPATH_8}:${HOME}/sushi-experiments/bin:${HOME}/sushi-experiments/libs/ganttproject-guava.jar -d bin -g src/ganttproject/*.java
RUN ${JAVA_HOME_8}/bin/javac -cp ${CLASSPATH_8}:${HOME}/sushi-experiments/bin -d bin -g src/ncll/*.java
RUN ${JAVA_HOME_8}/bin/javac -cp ${CLASSPATH_8}:${HOME}/sushi-experiments/bin -d bin -g src/treemap/*.java
RUN ${JAVA_HOME_8}/bin/javac -cp ${CLASSPATH_8}:${HOME}/sushi-experiments/bin -d bin -g src/tsafe/*.java
RUN ${JAVA_HOME_8}/bin/javac -cp ${CLASSPATH_8}:${HOME}/sushi-experiments/bin -d bin sushi-src/common/*.java
RUN ${JAVA_HOME_8}/bin/javac -cp ${CLASSPATH_8}:${HOME}/sushi-experiments/bin -d bin sushi-src/avl/*.java
RUN ${JAVA_HOME_8}/bin/javac -cp ${CLASSPATH_8}:${HOME}/sushi-experiments/bin -d bin sushi-src/dll_hard/*.java
RUN ${JAVA_HOME_8}/bin/javac -cp ${CLASSPATH_8}:${HOME}/sushi-experiments/bin -d bin sushi-src/ganttproject/*.java
RUN ${JAVA_HOME_8}/bin/javac -cp ${CLASSPATH_8}:${HOME}/sushi-experiments/bin -d bin sushi-src/ncll/*.java
RUN ${JAVA_HOME_8}/bin/javac -cp ${CLASSPATH_8}:${HOME}/sushi-experiments/bin -d bin sushi-src/treemap/*.java
RUN ${JAVA_HOME_8}/bin/javac -cp ${CLASSPATH_8}:${HOME}/sushi-experiments/bin -d bin sushi-src/tsafe/*.java

WORKDIR ${HOME}

