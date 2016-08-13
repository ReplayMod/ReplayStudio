# ReplayStudio
## About ReplayStudio
ReplayStudio is a library for manipulating replay files.

Replay files are basically packet dumps of the connection from the Minecraft Server to a Minecraft Client.
These packet dumps contain all packets sent from the server to the client (except login phase).
They may also contain additional packets added by the recording software in order to display the client whose connection is recorded.

### Features
- Loading / Saving replay files (including crash recovery)
- Remove specific packets from the replay (chat, mobs, etc.)
- Cut replays into parts
- Concatenate replays or parts of replays
- Squash specific parts of a replay into one moment removing redundant packets (e.g. removing the first 30m of a replay)
- List amount of packets by type
- **Expandable through custom filters** (in fact most of the above is implemented as a custom filter)
- Pathing system used in the ReplayMod

## Building
ReplayStudio is built using the [Java Development Kit](http://www.oracle.com/technetwork/java/javase/downloads/index.html) (Version 8) and [Gradle](http://gradle.org/).

You can then build ReplayStudio by using the command `./gradlew`. You may also use a local installation of gradle.

If everything went well, the generated jar file should be in the `build/libs` directory ending with `-all.jar`.
ReplayStudio will also be installed into your local maven repository if you want to use it in a project of yours.

## Running
ReplayStudio packages its dependencies in the generated jar file, therefore you can just run the jar file without any extra preparations: `java -jar replaystudio.jar`. Append `--help` to show all available commands.

The command line arguments will probably be confusing and can be looked up on the [GitHub wiki](https://github.com/ReplayMod/ReplayStudio/wiki).

Replay studio does not yet implement a mechanism for loading filter e.g. from a separate folder so you have to add them to the classpath manually. This will be improved as soon as people actually have a need for it.

## Documentation and Support
Javadocs can be generated using the `./gradlew javadoc` command and can then be found in the `build/docs/javadoc` folder but they might be incomplete on some parts (especially the internal classes and methods) so looking at the code might be the best help.

When using ReplayStudio as a standalone application as well as a library have a look at the [GitHub wiki](https://github.com/ReplayMod/ReplayStudio/wiki) for information on how to properly use it.

## License
ReplayStudio is free software licensed under the MIT license. See `LICENSE` for more information.