# RaindanceMC/chat

- **`chat-module`** — Rain's Flexible Modules — Chat: Everything that happens to a line typed into public chat: the format it is shown in, @-mentions, a caps and repeat filter, a message cooldown, and the staff tools — /chat clear, freeze and slowmode — to reach for when it goes wrong. Prefix, suffix and name colour stay Core's own Identities; this is what sits around them.
- **`chat-standalone`** — Rain's Chat: Rain's Chat, as a plugin of its own. Nothing here but a build file and a descriptor: the chat module and the standard wrapper are shaded together, and that is the whole plugin.

Part of [RaindanceMC](https://github.com/RaindanceMC); built on [RainsCore](https://github.com/RaindanceMC/RainsCore) and the shared parent in [framework](https://github.com/RaindanceMC/framework). Split out of `rains-flexible-modules` on 16.10.2026, history kept.

## Build

```
mvn install
```

Needs Java 25 and read access to `packages.tstieh.de` (server ids `packages-releases` / `packages-snapshots` in `~/.m2/settings.xml`).

## Plugins

- `chat-standalone/target/RainsChat-1.25.0.jar` — also published as `de.raindancer.modules:chat-standalone:1.25.0`

## Versions

A module's version is a property in this repository's root `pom.xml`, a plugin's is `plugin.version` in its own pom. CI publishes every version that is not in the registry yet on the next push to master.
