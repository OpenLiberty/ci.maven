FROM openliberty/open-liberty:kernel-java8-openj9-ubi

# Add config
COPY --chown=1001:0  pom/target/liberty/wlp/usr/servers/defaultServer/server.xml /config/
COPY --chown=1001:0  pom/target/liberty/wlp/usr/servers/defaultServer/configDropins/overrides/liberty-plugin-variable-config.xml /config/configDropins/overrides/

# Add application
COPY --chown=1001:0  ear/target/guide-maven-multimodules-ear.ear /config/apps/ 