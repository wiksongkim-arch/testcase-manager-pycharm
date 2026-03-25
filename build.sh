#!/bin/bash
# TestCase Manager Build Script with Logging

# 日志文件
LOG_FILE="build-$(date +%Y%m%d-%H%M%S).log"

# 初始化日志
echo "Build started at $(date)" > "$LOG_FILE"

# 日志函数
log_info() {
    echo "[INFO] $1"
    echo "[INFO] $1" >> "$LOG_FILE"
}

log_success() {
    echo "[SUCCESS] $1"
    echo "[SUCCESS] $1" >> "$LOG_FILE"
}

log_error() {
    echo "[ERROR] $1"
    echo "[ERROR] $1" >> "$LOG_FILE"
}

# 执行命令并记录日志
run_cmd() {
    echo "" >> "$LOG_FILE"
    echo "[CMD] $*" >> "$LOG_FILE"
    echo "---" >> "$LOG_FILE"
    if "$@" 2>> "$LOG_FILE"; then
        echo "---" >> "$LOG_FILE"
        return 0
    else
        echo "---" >> "$LOG_FILE"
        return 1
    fi
}

echo "========================================"
echo "TestCase Manager Build Script"
echo "========================================"
echo ""
log_info "Log file: $LOG_FILE"

# Check Java
log_info "Checking Java..."
if ! command -v java &> /dev/null; then
    log_error "Java not found"
    log_info "Please install Java 17: https://adoptium.net/"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
log_info "Java version: $JAVA_VERSION"

# Extract major version
JAVA_MAJOR=$(echo "$JAVA_VERSION" | cut -d'.' -f1)
if [ "$JAVA_MAJOR" = "1" ]; then
    JAVA_MAJOR=$(echo "$JAVA_VERSION" | cut -d'.' -f2)
fi

if [ "$JAVA_MAJOR" -lt 17 ]; then
    log_error "Java version too old: $JAVA_VERSION"
    log_info "Java 17 or higher is required"
    log_info "Please install Java 17: https://adoptium.net/"
    exit 1
fi

log_success "Java version OK"

# Check Gradle Wrapper
log_info "Checking Gradle Wrapper..."
if [ ! -f "./gradlew" ]; then
    log_error "Gradle Wrapper not found"
    log_info "Please run: gradle wrapper"
    exit 1
fi

log_success "Gradle Wrapper found"

# Clean
log_info "Cleaning..."
run_cmd ./gradlew clean --quiet || log_info "Clean warning (no previous build)"

# Compile
log_info "Compiling..."
if run_cmd ./gradlew compileKotlin --quiet; then
    log_success "Compile OK"
else
    log_error "Compile failed"
    log_info "See $LOG_FILE for details"
    exit 1
fi

# Build
log_info "Building plugin..."
if run_cmd ./gradlew buildPlugin --quiet; then
    log_success "Build OK"
else
    log_error "Build failed"
    log_info "See $LOG_FILE for details"
    exit 1
fi

# Check output
if [ -f "build/distributions"/*.zip ]; then
    PLUGIN_FILE=$(ls -t build/distributions/*.zip | head -1)
    echo ""
    log_success "Plugin package:"
    ls -lh "$PLUGIN_FILE" | tee -a "$LOG_FILE"
else
    log_error "Plugin package not found"
    exit 1
fi

echo ""
echo "========================================"
echo "Build Complete!"
echo "========================================"
echo ""
log_info "Log saved to: $LOG_FILE"
echo ""
echo "Install steps:"
echo "1. Open PyCharm"
echo "2. Settings → Plugins → Install from disk"
echo "3. Select: $PLUGIN_FILE"
echo "4. Restart PyCharm"
echo ""
