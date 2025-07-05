package io.ib67.edge.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.io.ByteSequence;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SourceSerializerTest {

    @Test
    public void testSerializeDeserializeStringSource() throws Exception {
        String testCode = "function test() { return 'hello'; }";
        Source source = Source.newBuilder("js", testCode, "test.js")
                .name("testSource")
                .mimeType("application/javascript")
                .build();

        SimpleModule module = new SimpleModule();
        module.addSerializer(new SourceSerializer());
        module.addDeserializer(Source.class, new SourceDeserializer());
        ObjectMapper mapper = new ObjectMapper().registerModule(module);

        String json = mapper.writeValueAsString(source);
        Source deserialized = mapper.readValue(json, Source.class);

        assertEquals(source.getName(), deserialized.getName());
        assertEquals(source.getMimeType(), deserialized.getMimeType());
        assertEquals(source.getPath(), deserialized.getPath());
        assertEquals(source.getLanguage(), deserialized.getLanguage());
        assertEquals(source.getCharacters().toString(), deserialized.getCharacters().toString());
    }

    @Test
    public void testSerializeDeserializeBinarySource() throws Exception {
        byte[] bytes = {0x48, 0x65, 0x6c, 0x6c, 0x6f}; // "Hello" in ASCII
        Source source = Source.newBuilder("llvm", ByteSequence.create(bytes), "test.bin")
                .name("binarySource")
                .mimeType("application/octet-stream")
                .build();

        SimpleModule module = new SimpleModule();
        module.addSerializer(new SourceSerializer());
        module.addDeserializer(Source.class, new SourceDeserializer());
        ObjectMapper mapper = new ObjectMapper().registerModule(module);

        String json = mapper.writeValueAsString(source);
        Source deserialized = mapper.readValue(json, Source.class);

        assertEquals(source.getName(), deserialized.getName());
        assertEquals(source.getMimeType(), deserialized.getMimeType());
        assertEquals(source.getPath(), deserialized.getPath());
        assertEquals(source.getLanguage(), deserialized.getLanguage());
        assertArrayEquals(source.getBytes().toByteArray(), deserialized.getBytes().toByteArray());
    }
}
