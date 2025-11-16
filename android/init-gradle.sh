#!/bin/bash
# Initialize Gradle wrapper for Stone Launcher

set -e

echo "Initializing Gradle wrapper..."

# Find Android Studio's gradle
GRADLE_HOME="/Applications/Android Studio.app/Contents/gradle/gradle-8.2"

if [ ! -d "$GRADLE_HOME" ]; then
    echo "Looking for alternative Gradle installation..."
    # Try to find it in Android Studio plugins
    GRADLE_PLUGIN_DIR="/Applications/Android Studio.app/Contents/plugins/gradle"
    if [ -d "$GRADLE_PLUGIN_DIR" ]; then
        # Find gradle wrapper jar in plugin
        GRADLE_WRAPPER_JAR=$(find "$GRADLE_PLUGIN_DIR" -name "gradle-wrapper.jar" | head -1)
        if [ -n "$GRADLE_WRAPPER_JAR" ]; then
            echo "Found Gradle wrapper jar: $GRADLE_WRAPPER_JAR"
            cp "$GRADLE_WRAPPER_JAR" gradle/wrapper/gradle-wrapper.jar
        fi
    fi
fi

# Create gradlew scripts
cat > gradlew << 'EOF'
#!/bin/sh

##############################################################################
# Gradle start up script for UN*X
##############################################################################

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
fi

# Increase the maximum file descriptors if we can.
if [ "$cygwin" = "false" -a "$darwin" = "false" -a "$nonstop" = "false" ] ; then
    MAX_FD_LIMIT=`ulimit -H -n`
    if [ $? -eq 0 ] ; then
        MAX_FD=`ulimit -n`
        if [ "$MAX_FD" = "maximum" -o "$MAX_FD" = "unlimited" ] ; then
            MAX_FD="$MAX_FD_LIMIT"
        fi
        ulimit -n $MAX_FD
        if [ $? -ne 0 ] ; then
            warn "Could not set maximum file descriptor limit: $MAX_FD"
        fi
    else
        warn "Could not query maximum file descriptor limit: $MAX_FD_LIMIT"
    fi
fi

# For Cygwin or MSYS, switch paths to Windows format before running java
if [ "$cygwin" = "true" -o "$msys" = "true" ] ; then
    APP_HOME=`cygpath --path --mixed "$APP_HOME"`
    CLASSPATH=`cygpath --path --mixed "$CLASSPATH"`
fi

exec "$JAVACMD" \
    -classpath "gradle/wrapper/gradle-wrapper.jar" \
    "-Dorg.gradle.appname=$APP_BASE_NAME" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
EOF

chmod +x gradlew

echo "Gradle wrapper initialized!"
echo "You can now run: ./gradlew assembleDebug"
