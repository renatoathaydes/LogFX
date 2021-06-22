FROM ubuntu:18.04 AS zulufx

RUN apt-get update
RUN apt-get -y install curl
RUN curl -o zulu-fx.tar.gz https://static.azul.com/zulu/bin/zulu11.37.19-ca-fx-jdk11.0.6-linux_x64.tar.gz
RUN tar zxf zulu-fx.tar.gz

FROM zulufx as logfx_build

RUN apt-get -y install xvfb
ENV JAVA_HOME ./zulu11.37.19-ca-fx-jdk11.0.6-linux_x64/
COPY gradle ./gradle/
COPY splash-maker ./splash-maker/
COPY src ./src/
COPY build.gradle ./build.gradle
COPY settings.gradle ./settings.gradle
COPY gradlew ./gradlew
# installs Gradle
RUN ./gradlew --no-daemon clean

FROM logfx_build

# To run the splash-screen maker, something like this needs to be run in the container
# (not when the container is being built)
#ENV DISPLAY :99
#RUN Xvfb :99 -screen 0 640x480x8 -nolisten tcp &
#RUN ./gradlew --no-daemon createSplashScreen

# Run all tests
CMD ./gradlew check
