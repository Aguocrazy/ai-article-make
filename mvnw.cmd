@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.2
@REM ----------------------------------------------------------------------------

@SETLOCAL
@SET MAVEN_PROJECTBASEDIR=%%~dp0

@IF EXIST "%%~dp0.mvn\wrapper\maven-wrapper.jar" (
  @SET WRAPPER_JAR="%%~dp0.mvn\wrapper\maven-wrapper.jar"
) ELSE (
  @ECHO "maven-wrapper.jar was not found in the expected location: %%~dp0.mvn\wrapper\"
  @EXIT /B 1
)

@IF NOT DEFINED JAVA_HOME (
  @ECHO Error: JAVA_HOME is not set correctly.
  @ECHO Please set the JAVA_HOME variable in your environment to match the
  @ECHO location of your Java installation.
  @EXIT /B 1
)

@SET WRAPPER_CMD=%%JAVA_HOME%%\bin\java.exe
@IF NOT EXIST "%%WRAPPER_CMD%" (
  @ECHO Error: JAVA_HOME is set to an invalid directory: "%%JAVA_HOME%"
  @ECHO Please set the JAVA_HOME variable in your environment to match the
  @ECHO location of your Java installation.
  @EXIT /B 1
)

@"%%WRAPPER_CMD%" -classpath "%%WRAPPER_JAR%" -Dmaven.multiModuleProjectDirectory="%%MAVEN_PROJECTBASEDIR%" -Dmaven.home="%%MAVEN_PROJECTBASEDIR%.mvn" -Dmaven.wrapper.version=3.3.2 -Dmaven.wrapper.dist.dir="%%MAVEN_PROJECTBASEDIR%.mvn\wrapper" org.apache.maven.wrapper.MavenWrapperMain %*