#!/bin/bash
# 서버에서 Java 17 설치 (한 번만 실행)
# 사용법: sudo bash install-java-and-build.sh

set -e
echo "=== Java 17 설치 ==="
apt-get update
apt-get install -y openjdk-17-jdk
java -version
echo "=== Java 17 설치 완료 ==="
