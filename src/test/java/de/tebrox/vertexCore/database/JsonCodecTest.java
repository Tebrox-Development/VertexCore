package de.tebrox.vertexCore.database;

import com.google.gson.JsonParser;
import de.tebrox.vertexCore.database.annotation.DbExpose;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonCodecTest {

    private final JsonCodec codec = new JsonCodec();

    @Test
    void toJsonProducesPrettyPrintedJson() {
        SampleData data = new SampleData("Spawn", true, 0);

        String json = codec.toJson(SampleData.class, data);

        assertTrue(json.contains("\n"));
        assertTrue(json.contains("  \"name\": \"Spawn\""));
        assertTrue(json.contains("  \"enabled\": true"));
        assertTrue(json.contains("  \"priority\": 0"));

        assertEquals(JsonParser.parseString("{\"name\":\"Spawn\",\"enabled\":true,\"priority\":0}"), JsonParser.parseString(json));
    }

    @Test
    void fromJsonReadsLegacyCompactJson() {
        SampleData data = codec.fromJson(SampleData.class, "{\"name\":\"Spawn\",\"enabled\":true,\"priority\":0}");

        assertEquals("Spawn", data.name);
        assertTrue(data.enabled);
        assertEquals(0, data.priority);
    }

    @Test
    void prettyPrintedJsonRoundTripsWithoutDataChanges() {
        SampleData original = new SampleData("Spawn", true, 42);

        String json = codec.toJson(SampleData.class, original);
        SampleData restored = codec.fromJson(SampleData.class, json);

        assertEquals(original.name, restored.name);
        assertEquals(original.enabled, restored.enabled);
        assertEquals(original.priority, restored.priority);
    }

    static final class SampleData {
        @DbExpose String name;
        @DbExpose boolean enabled;
        @DbExpose int priority;

        SampleData() {}

        SampleData(String name, boolean enabled, int priority) {
            this.name = name;
            this.enabled = enabled;
            this.priority = priority;
        }
    }
}
