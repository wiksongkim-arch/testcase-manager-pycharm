#!/bin/bash
# TestCase Manager Build Script

set -e

echo "========================================"
echo "TestCase Manager Build Script"
echo "========================================"
echo ""

# Check Java
echo "[INFO] Checking Java..."
if ! command -v java &> /dev/null; then
    echo "[ERROR] Java not found"
    echo "Please install Java 17: https://adoptium.net/"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
echo "[INFO] Java version: $JAVA_VERSION"

# Extract major version
JAVA_MAJOR=$(echo "$JAVA_VERSION" | cut -d'.' -f1)
if [ "$JAVA_MAJOR" = "1" ]; then
    JAVA_MAJOR=$(echo "$JAVA_VERSION" | cut -d'.' -f2)
fi

if [ "$JAVA_MAJOR" -lt 17 ]; then
    echo "[ERROR] Java version too old: $JAVA_VERSION"
    echo "Java 17 or higher is required"
    echo "Please install Java 17: https://adoptium.net/"
    exit 1
fi

echo "[SUCCESS] Java version OK"

# Check Gradle Wrapper
echo "[INFO] Checking Gradle Wrapper..."
if [ ! -f "./gradlew" ]; then
    echo "[ERROR] Gradle Wrapper not found"
    echo "Please run: gradle wrapper"
    exit 1
fi

echo "[SUCCESS] Gradle Wrapper found"

# Clean
echo "[INFO] Cleaning..."
./gradlew clean --quiet || true

# Compile
echo "[INFO] Compiling..."
if ./gradlew compileKotlin --quiet; then
    echo "[SUCCESS] Compile OK"
else
    echo "[ERROR] Compile failed"
    exit 1
fi

# Build
echo "[INFO] Building plugin..."
if ./gradlew buildPlugin --quiet; then
    echo "[SUCCESS] Build OK"
else
    echo "[ERROR] Build failed"
    exit 1
fi

# Check output
if [ -f "build/distributions"/*.zip ]; then
    PLUGIN_FILE=$(ls -t build/distributions/*.zip | head -1)
    echo ""
    echo "[SUCCESS] Plugin package:"
    ls -lh "$PLUGIN_FILE"
else
    echo "[ERROR] Plugin package not found"
    exit 1
fi

echo ""
echo "========================================"
echo "Build Complete!"
echo "========================================"
echo ""
echo "Install steps:"
echo "1. Open PyCharm"
echo "2. Settings → Plugins → Install from disk"
echo "3. Select: $PLUGIN_FILE"
echo "4. Restart PyCharm"
echo ""
