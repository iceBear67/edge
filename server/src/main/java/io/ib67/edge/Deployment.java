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

package io.ib67.edge;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.ib67.edge.serializer.SourceDeserializer;
import io.ib67.edge.serializer.SourceSerializer;
import org.graalvm.polyglot.Source;

import java.util.Map;

public record Deployment(
        String name,
        Map<String, String> env,
        @JsonSerialize(using = SourceSerializer.class)
        @JsonDeserialize(using = SourceDeserializer.class)
        Source source
) {
    public Deployment {
        if(env == null) env = Map.of();
    }
}
