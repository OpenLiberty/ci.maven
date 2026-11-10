#!/bin/bash

# Ensure script always executes from the repository root directory
cd $(dirname $0)/..

# Clean and clone dependencies
rm -rf ci.ant ci.common
git clone --depth=1 git@github.com:OpenLiberty/ci.ant.git ci.ant
git clone --depth=1 git@github.com:OpenLiberty/ci.common.git ci.common

# Install dependencies
chmod +x ./mvnw
./mvnw -V clean install -f ci.ant --batch-mode --no-transfer-progress --errors -DtrimStackTrace=false -DskipTests
./mvnw -V clean install -f ci.common --batch-mode --no-transfer-progress --errors -DtrimStackTrace=false -DskipTests
MVN_RC=$?

exit $MVN_RC
