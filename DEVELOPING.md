DEVELOPING.md

This document describes how to develop, test, and publish the sercapnp IntelliJ plugin.

Prerequisites
- JDK 11 (or the JDK version targeted in build.gradle). For Platform API v2 or newer you may need Java 17+ or 21 depending on the target.
- Gradle wrapper (bundled: use ./gradlew)
- Internet access for Gradle dependencies

Common tasks

1) Generate the lexer
- The project uses GrammarKit/Grammarkit to generate a lexer from `src/main/java/com/sercapnp/lang/Capnp.flex`.
- Run:
  ./gradlew generateLexer
- Generated sources are placed in `build/generated-src/com/sercapnp/lang/CapnpLexer.java` (sourceSets configured accordingly).

2) Build plugin ZIP (local)
- Build distributable ZIP for installation in JetBrains IDEs:
  ./gradlew buildPlugin
- If running in a headless CI where `buildSearchableOptions` fails, skip it:
  ./gradlew buildPlugin -x buildSearchableOptions
- The ZIP will be in `build/distributions/sercapnp-VERSION.zip`.

3) Build just the JAR
  ./gradlew jar
  The JAR will be under `build/libs/` (instrumented-*.jar and plugin jar)

4) Run IDE with the plugin (for manual integration testing)
  ./gradlew runIde

5) Running tests
- Currently there are no plugin unit tests. Add tests under `src/test/java` using JUnit and the IntelliJ testing framework if needed.

6) Running the Plugin Verifier (recommended for compatibility checks)
- Install the gradle-intellij-plugin's pluginVerifier or use the `verifyPlugin` task if configured.
- Run verification across target IDE versions (this may download many IDE distributions):
  ./gradlew pluginVerifier

7) Publishing to JetBrains Marketplace
- Create a JetBrains Marketplace token and store it in your Gradle properties (do NOT commit tokens):
  In ~/.gradle/gradle.properties:
  intellijPublishToken=YOUR_TOKEN

- Publish with gradle-intellij-plugin:
  ./gradlew publishPlugin

- Alternatively, create a release on GitHub and attach the ZIP from `build/distributions/` manually.

8) CI recommendations
- Configure GitHub Actions to run:
  - ./gradlew --no-daemon clean generateLexer buildPlugin -x buildSearchableOptions
  - ./gradlew pluginVerifier (optional/needs cache for IDEs)
  - Publish on tag via publishPlugin with token from GitHub Secrets.

9) Development tips
- When editing `Capnp.flex`, run `./gradlew generateLexer` and `./gradlew compileJava` to validate generated code.
- Re-run `./gradlew runIde` for manual testing; prefer an IDE run to debug behavior.
- Use `-x buildSearchableOptions` in headless environments.

10) Compatibility matrix
- The plugin currently targets IntelliJ Platform 2022.1+ by default (see build.gradle). If you plan to target newer platform versions or migrate to Platform API v2, update the `intellij.version` and test with plugin verifier.

11) Notes on publishing and security
- Never commit your Marketplace token or other secrets to the repository.
- Use GitHub Actions secrets to store the token and pass it to Gradle securely.

If you want, I can add a GitHub Actions workflow template to this repo to automate builds and publishing.