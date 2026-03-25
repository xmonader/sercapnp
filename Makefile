# Makefile for sercapnp — quick developer tasks

GRADLE ?= ./gradlew
# Skip searchable options by default (helps in headless CI environments)
SKIP_SEARCHABLE ?= -x buildSearchableOptions

.PHONY: help generate-lexer test build jar run publish clean

help:
	@echo "Usage: make [target]"
	@echo "Targets: generate-lexer, test, build, jar, run, publish, clean"

generate-lexer:
	$(GRADLE) generateLexer

test:
	$(GRADLE) test

# Build plugin ZIP. By default skips building searchable options which can fail in headless envs.
build:
	$(GRADLE) buildPlugin $(SKIP_SEARCHABLE)

jar:
	$(GRADLE) jar

# Run an IDE with the plugin (manual testing)
run:
	$(GRADLE) runIde

# Publish to JetBrains Marketplace. Requires INTELLIJ_PUBLISH_TOKEN env var or configure ~/.gradle/gradle.properties
publish:
	@if [ -n "$(INTELLIJ_PUBLISH_TOKEN)" ]; then $(GRADLE) publishPlugin -x buildSearchableOptions; else echo "ERROR: INTELLIJ_PUBLISH_TOKEN not set."; exit 1; fi

clean:
	$(GRADLE) clean
