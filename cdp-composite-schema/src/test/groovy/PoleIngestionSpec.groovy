import cdp.groovy.test.GroovyTester
import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.everit.json.schema.Schema
import org.everit.json.schema.loader.SchemaClient
import org.everit.json.schema.loader.SchemaLoader
import org.json.JSONObject
import org.json.JSONTokener
import spock.lang.Specification

class PoleIngestionSpec extends Specification implements GroovyTester {

  static final String scriptFile = "src/test/resources/TestScript_groovy";

  @Override
  def getScript() {
    return scriptFile;
  }


  def "load the JSON Schema and try various JSON inputs"() {

    given: 'The CDP JSON Schema'
      InputStream inputStream = getClass().getResourceAsStream(schemaPath)
      JSONObject rawSchema = new JSONObject(new JSONTokener((InputStream) inputStream));

    and: 'load and validate the schema'
      SchemaClient schemaClient = SchemaClient.classPathAwareClient();
      Schema schema = SchemaLoader.load(rawSchema, schemaClient);
      String errorString = null;

      JsonSlurper slurper = new JsonSlurper();
      URL expectedFileURL = getClass().getResource(jsonFilePath);

      Object expectedJSON = slurper.parse((URL) expectedFileURL);
      JSONObject objectUnderTest = new JSONObject(JsonOutput.prettyPrint(new JsonBuilder(expectedJSON).toString()))

    when: 'validate file'

      try {
        schema.validate(objectUnderTest);
        // throws a ValidationException if this object is invalid

      }
      catch (Exception e) {
        errorString = e.toString();
      }

    then: "make sure we have an error message from ${jsonFilePath}: ${errorString} "
      (jsonFilePath != null) &&
        (isValid? (errorString == null) :
          (errorString != null && errorString.equals(expectedError)
          )
        )


    where:
      schemaPath                          | jsonFilePath                                         | isValid | expectedError
      "/JSONSchema/CDPRequestCreate.json" | "/jsonFiles/validRequest.json"                       | true    | null
      "/JSONSchema/CDP_P_Person.json"     | "/jsonFiles/invalidRequest-wrongSubTypePerson.json"  | false   | "org.everit.json.schema.ValidationException: #/P.person/meta/m.subType: persona is not a valid enum value"
      "/JSONSchema/CDPmetadata.json"      | "/jsonFiles/invalidRequest-wrongSubTypeCDPMdta.json" | false   | "org.everit.json.schema.ValidationException: #/m.subType: persona is not a valid enum value"
      "/JSONSchema/CDPRequestCreate.json" | "/jsonFiles/helloWorld.json"                         | false   | "org.everit.json.schema.ValidationException: #: 3 schema violations found"
      "/JSONSchema/CDPRequestCreate.json" | "/jsonFiles/invalidRequest-wrongRelationship.json"   | false   | "org.everit.json.schema.ValidationException: #/associations/0/relationship: #: only 2 subschema matches out of 3"
      "/JSONSchema/CDPRequestCreate.json" | "/jsonFiles/invalidRequest-wrongSubType.json"        | false   | "org.everit.json.schema.ValidationException: #/poles/0: #: no subschema matched out of the total 2 subschemas"


  }


  def "test the graph schema loader"() {

    when: 'The CDP JSON Schema is loaded'
      def result = invokeScriptMethod("testTraversal", (URL) schemaPath)

    then:

      result == res
    where:
      schemaPath                                                     | res
      getClass().getResource("JSONSchema/CDPGraphSchema.json") | true

  }


  def "test the graph schema loader for indices Map"() {

    when: 'The CDP JSON Schema is loaded'
      String result = invokeScriptMethod("testGraphIndices", (String) schemaPath)
      JsonSlurper slurper = new JsonSlurper();
      URL expectedFileURL = getClass().getResource(expectedFile);

      Object expectedJSON = slurper.parse((URL) expectedFileURL);

      String expected = JsonOutput.prettyPrint(new JsonBuilder(expectedJSON).toString());

    then:

      result == expected

    where:
      schemaPath                        | expectedFile
      "/JSONSchema/CDPGraphSchema.json" | "/graphSchema/graphSchema.json"

  }


}
