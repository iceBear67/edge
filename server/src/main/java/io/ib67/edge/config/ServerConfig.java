package io.ib67.edge.config;

import java.util.Map;
import java.util.Objects;

public record ServerConfig(
        String listenHost,
        int listenPort,
        Map<String, String> engineOptions,
        RuntimeConfig runtime
) {
    public ServerConfig {
        Objects.requireNonNull(listenHost);
        if (listenPort < 0 || listenPort > 65535) {
            throw new IllegalArgumentException("listenPort must be between 0 and 65535");
        }
        engineOptions = engineOptions == null ? Map.of() : engineOptions;
        Objects.requireNonNull(runtime);
    }

    public record RuntimeConfig(
            String pathLibraries,
            String pathLibraryCache,
            Map<String, String> guestContextOptions,
            Map<String, String> hostContextOptions
    ) {
        public RuntimeConfig {
            pathLibraries = pathLibraries == null ? "libraries" : pathLibraries;
            pathLibraryCache = pathLibraryCache == null ? "libcache" : pathLibraryCache;
            guestContextOptions = guestContextOptions == null ? Map.of() : guestContextOptions;
            hostContextOptions = hostContextOptions == null ? Map.of() : hostContextOptions;
        }
    }
}
