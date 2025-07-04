package io.ib67.edge.script;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.nio.file.Path;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ScriptOption.PathAccess.class, name = "fs_access"),
        @JsonSubTypes.Type(value = ScriptOption.ContextOption.class, name = "context_option")
})
public sealed interface ScriptOption {
    record PathAccess(Path prefix, boolean readOnly) implements ScriptOption {
    }

    record ContextOption(String key, String value) implements ScriptOption {

    }
}
