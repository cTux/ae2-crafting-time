package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AppliedFluxMetadataTest {
    @ParameterizedTest
    @CsvSource({
            "1.20.1-forge, mods.toml, 1.20-1.3.6-forge, false",
            "1.20.1-forge, mods.toml, 1.20-1.3.7-forge, true",
            "1.20.1-forge, mods.toml, 1.20-1.3.8-forge, true",
            "1.21.1-neoforge, neoforge.mods.toml, 1.21-2.1.3-neoforge, false",
            "1.21.1-neoforge, neoforge.mods.toml, 1.21-2.1.4-neoforge, true",
            "1.21.1-neoforge, neoforge.mods.toml, 1.21-2.1.5-neoforge, true",
            "26.1.2-neoforge, neoforge.mods.toml, 26.1-1.0.0-neoforge, false",
            "26.1.2-neoforge, neoforge.mods.toml, 26.1-1.0.1-neoforge, true",
            "26.1.2-neoforge, neoforge.mods.toml, 26.1-1.0.2-neoforge, true"
    })
    void acceptsSupportedDeclaredVersions(String target, String filename, String version,
                                          boolean accepted) throws Exception {
        Path metadata = Path.of("..", target, "src/main/resources/META-INF", filename);
        Config config = new TomlParser().parse(Files.readString(metadata));
        List<Config> dependencies = config.get(List.of("dependencies", "${mod_id}"));
        Config appflux = dependencies.stream()
                .filter(dependency -> "appflux".equals(dependency.get("modId")))
                .findFirst().orElseThrow();
        VersionRange range = VersionRange.createFromVersionSpec(appflux.get("versionRange"));

        assertEquals(accepted, range.containsVersion(new DefaultArtifactVersion(version)));
        assertNull(range.getRestrictions().getFirst().getUpperBound());
        assertEquals("BOTH", appflux.get("side"));
        assertEquals("AFTER", appflux.get("ordering"));
        assertEquals("OPTIONAL", appflux.getOrElse("type", "OPTIONAL"));
        assertFalse(appflux.<Boolean>getOrElse("mandatory", false));
    }
}
