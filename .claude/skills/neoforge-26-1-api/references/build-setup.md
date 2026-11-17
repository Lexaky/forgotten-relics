# Build setup for a 26.1.2 NeoForge addon

## gradle.properties

```properties
org.gradle.jvmargs=-Xmx3G
org.gradle.daemon=true
org.gradle.caching=true
org.gradle.configuration-cache=false
org.gradle.vfs.watch=false   # on Windows file-system watching hung 19 min in listFileSystems before every build
minecraft_version=26.1.2
minecraft_version_range=[26.1.2,26.2)
neo_version=26.1.2.122        # match the version the target library was built with
mod_id=yourmod
mod_name=Your Mod
mod_license=...
mod_version=0.1.0-26.1.2
mod_group_id=your.group
```

## settings.gradle

```groovy
pluginManagement { repositories { gradlePluginPortal(); maven { url = 'https://maven.neoforged.net/releases' } } }
plugins { id 'org.gradle.toolchains.foojay-resolver-convention' version '1.0.0' }
rootProject.name = 'your-mod'
```
Gradle 9.x wrapper (9.2.1 worked). Set `JAVA_HOME` to a JDK 25 for every gradle call (`java.toolchain.languageVersion = 25`).

## build.gradle essentials

```groovy
plugins { id 'java-library'; id 'net.neoforged.moddev' version '2.0.148'; id 'idea' }
java.toolchain.languageVersion = JavaLanguageVersion.of(25)

repositories {
    exclusiveContent { forRepository { maven { url = 'https://cursemaven.com' } }        filter { includeGroup 'curse.maven' } }
    exclusiveContent { forRepository { maven { url = 'https://api.modrinth.com/maven' } } filter { includeGroup 'maven.modrinth' } }
    maven { url = 'https://maven.theillusivec4.top/' }   // Curios
}

neoForge {
    version = project.neo_version
    runs {
        client { client(); gameDirectory = project.file('run') }
        server { server(); gameDirectory = project.file('run-server'); programArgument '--nogui' }
        gameTestServer {
            type = 'gameTestServer'
            gameDirectory = project.file('run-gametest')
            systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id
            systemProperty 'yourmod.gametest', 'true'          // register tests only when set
        }
        clientCheck {                                           // optional automated screenshot run
            client(); gameDirectory = project.file('run-check')
            systemProperty 'yourmod.clientcheck', 'true'
            programArguments.addAll '--quickPlaySingleplayer', 'check'
        }
    }
    mods { "${mod_id}" { sourceSet(sourceSets.main) } }
}

configurations { runtimeClasspath.extendsFrom localRuntime }
dependencies {
    implementation "curse.maven:thaumaturge-1628024:9428045"     // release jar = compile + runtime, includes generated data
    compileOnly  "top.theillusivec4.curios:curios-neoforge:15.0.0-beta.2+26.1.2:api"
    localRuntime "top.theillusivec4.curios:curios-neoforge:15.0.0-beta.2+26.1.2"
    localRuntime "maven.modrinth:XaDC71GB:84kuUpr4"              // Lithostitched (Thaumaturge runtime dep)
}
```
Use `src/main/templates/META-INF/neoforge.mods.toml` with `${...}` placeholders expanded by a `ProcessResources` task
(MDG template). Declare hard dependencies (`type="required"`, `ordering="AFTER"`) on the target library so its data
loads first and your data map entries win.

Why the release jar and not a source build: 26.1 jars are unobfuscated, so the release jar compiles fine, and it carries
the datagen output (recipes, aspects, research categories) that the git repo does not commit. A locally built jar from
source would be missing that data at runtime.

## First boot check

```bash
mkdir -p run-server && echo "eula=true" > run-server/eula.txt
./gradlew runServer        # in background; wait for "Done (" in the log, grep ERROR/Exception/<modid>, then kill java
```
Kill dev JVMs with PowerShell:
`Get-CimInstance Win32_Process -Filter "Name='java.exe'" | ? { $_.CommandLine -match '<project-folder>' } | % { Stop-Process -Id $_.ProcessId -Force }`

## Extracting sources

```bash
unzip -q -o build/moddev/artifacts/minecraft-patched-26.1.2.122-sources.jar -d <scratch>/mcsrc
unzip -q -o ~/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/26.1.2.122/*/neoforge-26.1.2.122-sources.jar -d <scratch>/mcsrc
```
API jars of other mods (Curios): `javap -cp <unzipped-api-jar-dir> fully.qualified.Class`.

Mods without sources (Thaumaturge, Botania Revived): Gradle already has a decompiler —
`java -jar ~/.gradle/caches/modules-2/files-2.1/org.vineflower/vineflower/<ver>/*/vineflower-<ver>.jar <classes-dir> <out-dir>`.
Unzip only the classes you need into their own directory first (the full jar takes minutes), and list signatures with
`javap -cp <dir> <class> | grep public` before decompiling. The original mod's own dependencies (e.g. Thaumcraft 4 for
a TC4 addon) can be fetched from CurseMaven to read exact numbers instead of guessing:
`https://www.cursemaven.com/curse/maven/<slug>-<projectId>/<fileId>/<slug>-<projectId>-<fileId>.jar`
(Thaumcraft 4.2.2.0: `thaumcraft-223628`, file 2222517). Read-only: never ship their assets.
