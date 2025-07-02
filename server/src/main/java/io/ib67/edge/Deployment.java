package io.ib67.edge;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.ib67.edge.script.ScriptOption;
import io.ib67.edge.serializer.SourceDeserializer;
import io.ib67.edge.serializer.SourceSerializer;
import org.graalvm.polyglot.Source;

import java.util.List;

public record Deployment(
        String name,
        String version,
        @JsonSerialize(using = SourceSerializer.class)
        @JsonDeserialize(using = SourceDeserializer.class)
        Source source,
        List<ScriptOption> options
) {
}
