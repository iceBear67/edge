/*
 *    Copyright 2025 iceBear67 and Contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package io.ib67.edge.config;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ServerConfig(
        String listenHost,
        int listenPort,
        String controlListenHost,
        int controlListenPort, // -1 to disable
        List<String> enabledPlugins,
        Map<String, String> engineOptions,
        RuntimeConfig runtime
) {
    public static ServerConfig defaultConfig() {
        return new ServerConfig(
                "localhost",
                8080,
                "localhost",
                8081,
                List.of(),
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
        enabledPlugins = enabledPlugins == null ? List.of() : enabledPlugins;
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
