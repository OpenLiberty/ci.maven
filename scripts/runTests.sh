#!/bin/bash

# Ensure script always executes from the repository root directory
cd $(dirname $0)/..

RUNTIME="${RUNTIME:-ol}"
RUNTIME_VERSION="${RUNTIME_VERSION:-26.0.0.6}"

./mvnw -V verify \
    --batch-mode --no-transfer-progress --errors \
    -DtrimStackTrace=false \
    -Ponline-its \
    -D"invoker.streamLogsOnFailures"=true \
    # -D"invoker.test"="*setup*,*springboot-3-*,*springboot-4-*,*compile-jsp-source-17-*,*toolchain-java-warning-jvmoption*" \ # Run tests that require a minimum of Java 17 or later
    -Druntime="${RUNTIME}" \
    -DruntimeVersion="${RUNTIME_VERSION}"
MVN_RC=$?

exit $MVN_RC
