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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.graalvm.polyglot.Source;

import java.io.IOException;
import java.util.HexFormat;

public class SourceSerializer extends StdSerializer<Source> {
    public SourceSerializer() {
        super(Source.class);
    }

    @Override
    public void serialize(Source source, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("name", source.getName());
        jsonGenerator.writeStringField("mime", source.getMimeType());
        jsonGenerator.writeStringField("file", source.getPath());
        jsonGenerator.writeStringField("language", source.getLanguage());
        if (source.hasBytes()) {
            jsonGenerator.writeStringField("type", "bytes");
            jsonGenerator.writeStringField("data", HexFormat.of().formatHex(source.getBytes().toByteArray()));
        } else if (source.hasCharacters()) {
            jsonGenerator.writeStringField("type", "string");
            jsonGenerator.writeStringField("data", source.getCharacters().toString());
        }
        jsonGenerator.writeEndObject();
    }
}
