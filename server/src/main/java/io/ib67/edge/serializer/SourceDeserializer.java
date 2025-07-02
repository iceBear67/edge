package io.ib67.edge.serializer;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.io.ByteSequence;

import java.io.IOException;
import java.util.HexFormat;

public class SourceDeserializer extends StdDeserializer<Source> {
    public SourceDeserializer() {
        super(Source.class);
    }

    @Override
    public Source deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        var tree = jsonParser.getCodec().readTree(jsonParser);
        var name = getStrOrNull(tree, "name");
        var mime = getStrOrNull(tree, "mime");
        var file = getStrOrNull(tree, "file");
        var language = getStrOrNull(tree, "language");
        var type = getStrOrNull(tree, "type");
        var data = getStrOrNull(tree, "data");
        Source.Builder builder;
        if ("bytes".equals(type)) {
            builder = Source.newBuilder(language, ByteSequence.create(HexFormat.of().parseHex(data)), file);
        } else if ("string".equals(type)) {
            builder = Source.newBuilder(language, data, file);
        } else {
            throw new JsonMappingException(jsonParser, "Unsupported type: " + type);
        }
        if (name != null) builder.name(name);
        if (mime != null) builder.mimeType(mime);
        return builder.build();
    }

    public String getStrOrNull(TreeNode tree, String key) {
        return switch (tree.get(key)) {
            case TextNode tn -> tn.asText();
            case NullNode n -> null;
            default -> throw new UnsupportedOperationException("Unexpected type at " + key + ": " + tree.get(key));
        };
    }
}
