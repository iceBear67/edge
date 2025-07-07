package io.ib67.edge.config;

import java.util.Map;
import java.util.Objects;

public record ServerConfig(
        String listenHost,
        int listenPort,
        Map<String, String> engineOptions,
        RuntimeConfig runtime
) {
    public static ServerConfig defaultConfig() {
        return new ServerConfig(
                "localhost",
                8080,
                Map.of(),
                new RuntimeConfig(
                        "./lib",
                        Map.of(),
                        Map.of()
                )
        );
    }
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
            Map<String, String> guestContextOptions,
            Map<String, String> hostContextOptions
    ) {
        public RuntimeConfig {
            pathLibraries = pathLibraries == null ? "libraries" : pathLibraries;
            guestContextOptions = guestContextOptions == null ? Map.of() : guestContextOptions;
            hostContextOptions = hostContextOptions == null ? Map.of() : hostContextOptions;
        }
    }
}
