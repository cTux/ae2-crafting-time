# Building It

The project uses Java 17 for Minecraft 1.20.1, Java 21 for Minecraft 1.21.1
NeoForge, and Java 25 for Minecraft 26.1.2 NeoForge. Gradle handles the declared
toolchains.

To build every supported version:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-all-versions.ps1
```

Pull requests are tested through GitHub Actions.

When the build finishes, the JARs are copied to `dist/`.
