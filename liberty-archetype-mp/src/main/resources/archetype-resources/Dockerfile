FROM open-liberty as server-setup
COPY /target/${artifactId}.zip /config/
RUN unzip /config/${artifactId}.zip && \
    mv /wlp/usr/servers/${artifactId}Server/* /config/ && \
    rm -rf /config/wlp && \
    rm -rf /config/${artifactId}.zip

FROM open-liberty
LABEL maintainer="Graham Charters" vendor="IBM" github="https://github.com/WASdev/ci.maven"
COPY --from=server-setup /config/ /config/
EXPOSE 9080 9443
