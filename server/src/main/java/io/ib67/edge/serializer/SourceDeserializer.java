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

package io.ib67.edge.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
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
