FROM openjdk:11-jdk-slim

WORKDIR /app

RUN apt-get update \
    && apt-get -y --no-install-recommends install apt-utils \
    && apt-get -y --no-install-recommends dist-upgrade \
    && apt-get -y --no-install-recommends install build-essential ca-certificates cmake curl git scala maven gcc-mingw-w64-x86-64 \
    && git clone --branch dev_no_csw --single-branch https://github.com/HorizenOfficial/Sidechains-SDK.git /Sidechains-SDK \
    && cd /Sidechains-SDK && mvn install -DskipTests

COPY . .

RUN mvn clean install

CMD java -jar /app/target/sc-bootstrap-be.jar
