# ReplayStudio
## About ReplayStudio
ReplayStudio is a library for manipulating replay file.

Replay files are basically packet dumps of the connection from the Minecraft Server to a Minecraft Client.
These packet dumps contain all packets sent from the server to the client (except login phase).
They may also contain additional packets added by the recording software in order to display the client whose connection is recorded.

### Replay files
Replay files (file extension ".mcpr") can be created using the [ReplayMod](http://replaymod.com) (Minecraft Forge Mod) or the [BukkitRecording API](https://github.com/Johni0702/BukkitRecording) (Bukkit Plugin API).
They can be played back using the same [ReplayMod](http://replaymod.com) (which has tons of more features during replay, such as smooth camera movement, slow motion, time lapse, video exporting and more) or the [ReplayServer](https://github.com/Johni0702/ReplayServer) (which doesn't require any client modifications but in turn doesn't do any of that fancy stuff).

### Features
- Loading / Saving replay files
- Remove specific packets from the replay (chat, mobs, etc.)
- Cut replays into parts
- Concatenate replays or parts of replays
- Squash specific parts of a replay into one moment removing redundant packets (e.g. removing the first 30m of a replay)
- List amount of packets by type
- **Expandable through custom filters** (in fact most of the above is implemented as a custom filter)

## Building
ReplayStudio is built using the [Java Development Kit](http://www.oracle.com/technetwork/java/javase/downloads/index.html) (Version 8 and Version 7 are both required, see below) and [Gradle](http://gradle.org/).
Even though jdk8 is required to build ReplayStudio, thanks to [RetroLambda](https://github.com/orfjackal/retrolambda) the resulting jar runs on java 7 as well.
Before building make sure you set your `JAVA7_HOME` environment variable to point to your java 7 installation and your `JAVA8_HOME` to points to your java 8 installation. These are necessary in order for RetroLambda to port the resulting java 8 jar file back to java 7.

You can then build ReplayStudio by using the command `./gradlew`. You may also use a local installation of gradle. However if you do, make sure it's compatible with the RetroLambda version in use.

If everything went well, the generated jar file should be in the `build/libs` directory ending with `-all.jar`.
ReplayStudio will also be installed into your local maven repository if you want to use it in a project of yours.

## Running
ReplayStudio packages its dependencies in the generated jar file, therefore you can just run the jar file without any extra preparations: `java -jar replaystudio.jar`. Append `--help` to show all available commands.

The command line arguments will probably be confusing and can be looked up on the [GitHub wiki](https://github.com/Johni0702/ReplayStudio/wiki). If a command isn't listed on the wiki or isn't explained well enough feel free to drop by on IRC (see below) and ask your question.

Replay studio does not yet implement a mechanism for loading filter e.g. from a separate folder so you have to add them to the classpath manually. This will be improved as soon as people actually have a need for it.

## Documentation and Support
Javadocs can be generated using the `./gradlew javadoc` command and can then be found in the `build/docs/javadoc` folder but they might be incomplete on some parts (especially the internal classes and methods) so looking at the code might be the best help.

When using ReplayStudio as a standalone application as well as a library have a look at the [GitHub wiki](https://github.com/Johni0702/ReplayStudio/wiki) for information on how to properly use it.

If you're lost completely or just want to ask a question feel free to drop by on IRC (#replaydev on [Esper.net](https://www.esper.net/)).

## License
ReplayStudio is free software licensed under the MIT license. See `LICENSE` for more information.