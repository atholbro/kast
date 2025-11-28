#!/usr/bin/env just --justfile
set dotenv-load
set windows-shell := ["powershell", "-NoProfile", "-Command"]#["cmd", "/c"]

# Build Variables
VERSION := env_var_or_default("VERSION", "latest")
cache_dir := justfile_directory() + '/.ci-cache'
ci := env_var_or_default("CI", "")

# Gradle Variables
gradlew_local := if os_family() == "windows" {
  '& "' + justfile_directory() + '/gradlew.bat" -p "' + justfile_directory() + '"'
} else {
  '"' + justfile_directory() + '/gradlew" -p "' + justfile_directory() + '"'
}
gradlew_ci := 'TERM=dumb GRADLE_USER_HOME="' + cache_dir + '" ' + gradlew_local + ' -q'
gradlew := if ci == "" { gradlew_local } else { gradlew_ci }

# Print a list of available recipes
_default:
  @just --justfile {{justfile()}} --list --unsorted

# Build
build:
    {{gradlew}} -configuration-cache build -x check --warning-mode all

# Deletes all build outputs & artifacts
clean:
    {{gradlew}} -configuration-cache clean

# Run all application targets
run:
    {{gradlew}} -configuration-cache run

# Run unit tests (when clean="true" all tests will run, otherwise only outdated tests are run)
test clean="false":
    {{gradlew}} -configuration-cache {{ if clean == "true" { "cleanTest test" } else { "test" } }}

# Run Gradle checks (lint, test, complexity).
check:
    {{gradlew}} -configuration-cache check

# Generate a code coverage report (via Jacoco).
coverage:
    {{gradlew}} -configuration-cache codeCoverageReport

# Verify source code style.
lint:
    {{gradlew}} -configuration-cache lintKotlin

# Format the source code.
format:
    {{gradlew}} -configuration-cache versionCatalogFormat formatKotlin

# Calculates code complexity.
complexity:
    {{gradlew}} -configuration-cache detekt

# Checks for library updates.
update-check:
    {{gradlew}} dependencyUpdates -Drevision=release

# Automatically updates all dependencies in the version catalog.
update-apply:
    {{gradlew}} versionCatalogUpdate