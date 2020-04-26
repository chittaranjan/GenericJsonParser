import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class GenericJsonParser {
    private ObjectMapper objectMapper;
    private JsonFactory factory;
    private Map<String, Object> jsonFieldMap;
    private boolean testEnabled;

    public GenericJsonParser() {
        testEnabled = false;
    }

    public void initialize(boolean testEnabled) {
        this.objectMapper = new ObjectMapper();
        this.factory = objectMapper.getFactory();
        this.testEnabled = testEnabled;
        if (this.testEnabled) {
            jsonFieldMap = new HashMap<>();
        }
    }

    private String processObject(JsonNode objectNode) {
        String json = null;
        try {
            json = objectMapper.writeValueAsString(objectNode);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return json;
    }

    private void traverseObjectNode(JsonNode objectNode, String currentField, Map<String, Object> objectMap) {
        if (objectNode.isObject()) {
            Iterator<String> fieldNames = objectNode.fieldNames();
            Map<String, Object> fieldObjectMap = new HashMap<>();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldValue = objectNode.get(fieldName);
                traverseObjectNode(fieldValue, fieldName, fieldObjectMap);
            }
        } else if (objectNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) objectNode;
            List<Object> values = new LinkedList<>();
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode arrayElement = arrayNode.get(i);
                values.add(arrayElement.textValue());
            }
            objectMap.putIfAbsent(currentField, values);
        } else {
            if (objectNode.isTextual()) {
                objectMap.putIfAbsent(currentField, objectNode.textValue());
            } else if (objectNode.isBinary()) {
                objectMap.putIfAbsent(currentField, objectNode.booleanValue());
            }
        }
    }

    private void processArrayField(String fieldName, JsonParser jsonParser) throws IOException {
        while(jsonParser.nextToken() == JsonToken.START_OBJECT) {
            // read everything from this START_OBJECT to the matching END_OBJECT
            // and return it as a tree model ObjectNode
            Map<String, Object> objectMap = new HashMap<>();
            JsonNode node = objectMapper.readTree(jsonParser);
            traverseObjectNode(node, fieldName, objectMap);
            if (testEnabled) {
                jsonFieldMap.putIfAbsent(fieldName, objectMap);
            }
            /**
             * Else to do some other processing
             */
        }
    }

    private void processObjectField(String fieldName, JsonParser jsonParser) throws IOException {
        // iterate through the content of the object
        while(jsonParser.nextToken() != JsonToken.END_OBJECT) {
            // read everything from this START_OBJECT to the matching END_OBJECT
            // and return it as a tree model ObjectNode
            Map<String, Object> objectMap = new HashMap<>();
            JsonNode node = objectMapper.readTree(jsonParser);
            traverseObjectNode(node, fieldName, objectMap);
            if (testEnabled) {
                jsonFieldMap.putIfAbsent(fieldName, objectMap);
            }
        }
    }


    private boolean isStartObject(JsonToken jsonToken) {
        return jsonToken == JsonToken.START_OBJECT;
    }

    private boolean isStartArray(JsonToken jsonToken) {
        return jsonToken == JsonToken.START_ARRAY;
    }

    private boolean isStartObjectOrStartArray(JsonToken jsonToken) {
        return jsonToken.isStructStart();
    }

   private void processJsonField(String fieldName, JsonParser jsonParser) throws IOException {
        JsonToken nextToken  = jsonParser.nextToken();
        if (isStartObject(nextToken)) {
            processObjectField(fieldName, jsonParser);
        } else if (isStartArray(nextToken)) {
            processArrayField(fieldName, jsonParser);
        } else if (nextToken.isScalarValue()) {
            if (nextToken.isNumeric()) {
                jsonFieldMap.putIfAbsent(fieldName, jsonParser.getNumberValue());
            }
        }
    }

    public Map<String, Object> getTestResult() {
        return jsonFieldMap;
    }


    public void parserJsonInput(String jsonFile) {
        InputStream inputStream;
        JsonParser jsonParser = null;

        inputStream = this.getClass().getResourceAsStream(jsonFile);
        try {
            jsonParser = factory.createParser(inputStream);
            jsonParser.nextToken(); //START_OBJECT

            while ( jsonParser.nextToken() == JsonToken.FIELD_NAME ) {
                String fieldName = jsonParser.getText();
                processJsonField(fieldName, jsonParser);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
                jsonParser.close();
            } catch( Exception e ) {
                System.out.println("Error while closing jsonParser");
            }
        }
    }
}
