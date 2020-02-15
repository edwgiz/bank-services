
FROM azul/zulu-openjdk-alpine:11 as packager

RUN { \
        java --version ; \
        echo "jlink version:" && \
        $JAVA_HOME/bin/jlink --version ; \
    }

ENV JAVA_MINIMAL=/opt/jre

# build modules distribution
RUN $JAVA_HOME/bin/jlink \
    --verbose \
    --add-modules \
        java.base,java.sql,java.naming,java.desktop,java.xml \
    --compress 2 \
    --strip-debug \
    --no-header-files \
    --no-man-pages \
    --output "$JAVA_MINIMAL"

# Second stage, add only our minimal "JRE" distr and our app
FROM alpine

ENV JAVA_MINIMAL=/opt/jre
ENV PATH="$PATH:$JAVA_MINIMAL/bin"

COPY --from=packager "$JAVA_MINIMAL" "$JAVA_MINIMAL"
COPY target/account-microservice-1.0.jar account-microservice.jar

EXPOSE 8080

CMD ["/bin/sh","-c","java -Dwebserver.http.hostname=127.0.0.1 -Dwebserver.http.port=8080 -jar /account-microservice.jar >> /var/log/account-microservice.log 2>&1"]




