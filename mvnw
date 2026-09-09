#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# Apache Maven Wrapper entry point (standard distribution script)
# ----------------------------------------------------------------------------

set -e

if [ -z "${JAVA_HOME-}" ]; then
  JAVACMD="$(command -v java)" || JAVACMD=""
else
  JAVACMD="$JAVA_HOME/bin/java"
fi

if [ ! -x "$JAVACMD" ] && [ -z "$JAVACMD" ]; then
  echo "Error: JAVA_HOME is not set correctly and no 'java' command could be found in PATH." >&2
  echo "Please set the JAVA_HOME variable in your environment to match the" >&2
  echo "location of your Java installation." >&2
  exit 1
fi

WRAPPER_JAR="$(cd "$(dirname "$0")" && pwd)/.mvn/wrapper/maven-wrapper.jar"

exec "$JAVACMD" \
  -classpath "$WRAPPER_JAR" \
  -Dmaven.multiModuleProjectDirectory="$(cd "$(dirname "$0")" && pwd)" \
  -Dmaven.home="$(cd "$(dirname "$0")" && pwd)/.mvn" \
  -Dmaven.wrapper.version=3.3.2 \
  -Dmaven.wrapper.dist.dir="$(cd "$(dirname "$0")" && pwd)/.mvn/wrapper" \
  org.apache.maven.wrapper.MavenWrapperMain "$@"