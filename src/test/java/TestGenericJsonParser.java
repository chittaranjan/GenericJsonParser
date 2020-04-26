import org.junit.Assert;
import org.junit.Test;

public class TestGenericJsonParser {
    @Test
    public void testJsonParser() {
        String jsonfile = "test.json";
        GenericJsonParser genericJsonParser = new GenericJsonParser();
        genericJsonParser.initialize(true);
        genericJsonParser.parserJsonInput(jsonfile);
        Assert.assertNotNull(genericJsonParser.getTestResult());
        Assert.assertEquals(3, genericJsonParser.getTestResult().size());
    }
}
