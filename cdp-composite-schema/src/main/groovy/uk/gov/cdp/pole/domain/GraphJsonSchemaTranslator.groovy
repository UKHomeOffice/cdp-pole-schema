package uk.gov.cdp.pole.domain

import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

public class GraphJsonSchemaTranslator {
  private static Map<String, Object> loadedSchemas = new HashMap<>()

  private static Map<String, Object> graphByPropertyNames = new HashMap<>();

  public static Object resolveSchema(uri, parentSchema = null) {
    uri = resolveUri(uri, parentSchema);
    if (!loadedSchemas.containsKey(uri)) {
      tryToLoad(uri);
    }
    return loadedSchemas.get(uri);
  }

  protected static String resolveUri(uri, parent) {
    if (!parent) {
      return uri
    }
    String retVal = null;
    String baseUri = null;
    InputStream input = tryToGetInputStream(uri);
    if (input != null) {
      return uri;
    }
    while (input == null && parent != null) {
//      baseUri = parent.resolvedId

      baseUri = parent.$id
      if (baseUri) {
        int lastSlash = baseUri.lastIndexOf('/')
        if (lastSlash > 0) {
          baseUri = baseUri.substring(0, lastSlash);

        }

        retVal = baseUri + uri.substring(uri.indexOf('/'));
        input = tryToGetInputStream(retVal);
      }
      if (input == null) {
        parent = parent.parent;

      }
    }

    return retVal;  //resolveRelativeUri(uri, baseUri);
  }

  protected static InputStream tryToGetInputStream(String uri) {
    InputStream input = null;
    if (uri == null) {
      return input;
    }
    if (uri ==~ /^classpath:\/\/.*/) {
      input = GraphJsonSchemaTranslator.class.getResourceAsStream(uri.substring(12))
      if (input == null) {
        input = GraphJsonSchemaTranslator.class.getResourceAsStream(uri.substring(11))
      }
    } else if (uri ==~ /^file:\/\/.*/) {
      input = new FileInputStream(uri.substring(7))
    } else if (uri ==~ /^http(?:s)?:\/\/.*/) {
      input = new URL(uri).openStream()
    }
    return input;

  }

  protected static void tryToLoad(String uri) {

    InputStream input = tryToGetInputStream(uri);
    try {
      JsonSlurper sluper = new JsonSlurper()
      Object schema = sluper.parse(new InputStreamReader(input))
      if (schema) {
        schema.resolvedId = uri
        loadedSchemas.put(uri, schema)
      }
    } catch (Exception e) {
      //it's not here
    }
  }

  private static String resolveRelativeUri(String uri, String base) {
    if (!base || isAbsolute(uri)) {
      return uri
    }
    if (base.endsWith("/")) {
      return base + uri;
    }
    return base.substring(0, base.lastIndexOf("/") + 1) + uri;
  }

  private static boolean isAbsolute(uri) {
    return uri ==~ /^.*:\/\/.*/
  }


  static String getGraphSchemaInfo(String urlStr) {
    JsonSlurper slurper = new JsonSlurper();
    URL schemaURL = getClass().getResource(urlStr);

    Object schema = slurper.parse((URL) schemaURL);
    // clear the map with property names as keys before we traverse the schema.
    graphByPropertyNames.clear();

    // traverseSchema will go through the whole schema, resolving any '$ref' entries,
    // and will add any entries that have graph annotations into the graphByPropertyNames map.
    traverseSchema(schema);


    Set<String> vertexLabelsStrSet = new HashSet<>();
    Set<String> edgeLabelsStrSet = new HashSet<>();

    def propertyKeys = [];

    Map <String, Set<String>> indicesByElasticSearchAnalyzers = new HashMap<>();

    // go through all the properties that have graph annotations, and start building the
    // JSON file that drives the current graph.

    Map<String, List<Map.Entry>> indices = graphByPropertyNames.groupEntriesBy{ k, v ->

      def propertyKeyEntry = [:]

      propertyKeyEntry.put("name", k)
      propertyKeyEntry.put("dataType", v.graphPropertyType)
      propertyKeyEntry.put("cardinality", v.graphCardinality)

      propertyKeys.add(propertyKeyEntry);

      String idxName = 'none';

      if (k.startsWith('association.relationship')) {

        edgeLabelsStrSet.addAll(v.enum)

      } else {


        int firstDot = k.indexOf('.');
        int secondDot = k.substring(firstDot + 1).indexOf('.') + firstDot + 1;

        String graphIndexType = v.graphIndexType;

        if ('mixed' == graphIndexType || 'composite' == graphIndexType) {
          idxName = "by_${k.substring(0, secondDot)}_${graphIndexType.toLowerCase()}Idx"
          vertexLabelsStrSet.add("${k.substring(0, secondDot)}")
        }
      }


      if (v.graphAnalyzer){

        v.graphAnalyzer.each{analyzerKey, analyzerVal ->
          Set<String> indicesWithAnalyzer = indicesByElasticSearchAnalyzers.putIfAbsent(analyzerVal,new HashSet<>());
          if(indicesWithAnalyzer == null){
            indicesWithAnalyzer = indicesByElasticSearchAnalyzers.get(analyzerVal)
          }
          indicesWithAnalyzer.add(idxName);
        }


      }




      return idxName;


    }
    indices.remove('none')


    def vertexLabels = [];

    vertexLabelsStrSet.each {
      def vertexLabelEntry = [:];
      vertexLabelEntry.put('name', it)
      vertexLabelEntry.put('partition', false)
      vertexLabelEntry.put('useStatic', false)
      vertexLabels.add(vertexLabelEntry);
    }


    def edgeLabels = [];


    edgeLabelsStrSet.each{
      def edgeLabelEntry = [:];
      edgeLabelEntry.name  = it;
      edgeLabelEntry.multiplicity = "MULTI";
      edgeLabelEntry.unidirected = false;

      edgeLabels.add(edgeLabelEntry)



    }


    def vertexIndices = []


    indices.each{ k, v ->

      def vertexIndexEntry = [:];
      vertexIndexEntry.name = k;
      vertexIndexEntry.unique = v[0].value?.graphUnique?: false;
      vertexIndexEntry.composite = ('composite' == v[0].value?.graphIndexType);
      vertexIndexEntry.indexOnly = null
      vertexIndexEntry.mixedIndex = ('mixed' == v[0].value?.graphIndexType)? 'search': null;
      vertexIndexEntry.propertyKeys = new HashSet<String>();

      vertexIndexEntry.propertyKeyMappings = [:]

      v.each { entry ->

        vertexIndexEntry.propertyKeys.add(entry.key)


        def propertyKeyMappingEntry = [:]


        propertyKeyMappingEntry.mapping = entry.value.graphMapping
        if (entry.value.graphAnalyzer){

          entry.value.graphAnalyzer.each{ analyzerKey, analyzerVal ->
            def analyzerMapping = [:]
            analyzerMapping.name = analyzerKey;
            analyzerMapping.value = analyzerVal;
            propertyKeyMappingEntry.analyzer = analyzerMapping;
          }
        }

        vertexIndexEntry.propertyKeyMappings.put(entry.key, propertyKeyMappingEntry)


      }

      vertexIndexEntry.propertyKeys = vertexIndexEntry.propertyKeys.toSorted();
      vertexIndexEntry.propertyKeyMappings = vertexIndexEntry.propertyKeyMappings.sort{ e1, e2 -> e1.key <=> e2.key };
      vertexIndices.add(vertexIndexEntry);



    }
    schema.elasticSearchTemplates.each { template ->

      Set<String> elIndices = indicesByElasticSearchAnalyzers.get(template.name)
      def newIdxPatterns = [];
      template.body.index_patterns?.each{ idxPattern ->
        elIndices.each { elIdx ->
          newIdxPatterns.add("${idxPattern}${elIdx}".toLowerCase())

        }
      }

      template.body.index_patterns=newIdxPatterns;

    }

    def retVal = [:]

    retVal.elasticSearchTemplates = schema.elasticSearchTemplates;
    retVal.propertyKeys = propertyKeys.sort { a,b -> a.name <=> b.name };
    retVal.vertexLabels = vertexLabels.sort { a,b -> a.name <=> b.name };
    retVal.edgeLabels = edgeLabels.sort { a,b -> a.name <=> b.name };
    retVal.vertexIndexes = vertexIndices.sort { a,b -> a.name <=> b.name };

    String retValStr = JsonOutput.prettyPrint(new JsonBuilder(retVal).toString());





    return retValStr;


  }

  static boolean traverseSchema(Object schema, String fullName = null) {
    fullName = fullName ?: "";


    if (!schema) {
      return true
    }

    if (schema instanceof String) {
      schema = resolve(schema)
    }


    if (schema in List) {
      schema.each {
        setParentIfNotNull(it, schema.parent)
        return traverseSchema(it, fullName);

      }
    }

    if (schema.$ref) {

      if (schema.$ref in List) {
        schema.$ref.each {
          def resolvedSchema = resolveSchema(it, schema.parent ?: schema);
          setParentIfNotNull(resolvedSchema, schema);
          return traverseSchema(resolvedSchema, fullName)
        }

      } else {
        def resolvedSchema = resolveSchema(schema.$ref, schema.parent ?: schema);
        setParentIfNotNull(resolvedSchema, schema);
        return traverseSchema(resolvedSchema, fullName)

      }
    }


    if (schema.graphIndexType) {
      graphByPropertyNames.put(fullName, schema)
    }

    if (schema.items) {
      setParentIfNotNull(schema.items, schema);
      return traverseSchema(schema.items, fullName)

    }

    if (schema.anyOf) {
      if (schema.anyOf in List) {
        schema.anyOf.each {
          setParentIfNotNull(it, schema);
          return traverseSchema(it, fullName)
        }

      } else {
        setParentIfNotNull(schema.anyOf, schema);
        return traverseSchema(schema.anyOf, fullName)

      }


    }
    if (schema.oneOf) {
      if (schema.oneOf in List) {
        schema.oneOf.each {
          setParentIfNotNull(it, schema);
          return traverseSchema(it, fullName)
        }

      } else {
        setParentIfNotNull(schema.oneOf, schema);
        return traverseSchema(schema.oneOf, fullName)

      }
    }


    if (schema.properties) {
      return schema.properties.every { name, property ->

        setParentIfNotNull(property, schema)

        return traverseSchema(property, fullName + ((fullName.size() > 0) ? "." : "") + name);
      }

    }

    return true
  }


  static Object resolve(String id) {
    return resolveSchema(id);
  }

  private static setParentIfNotNull(schema, parent) {
    if (schema) {
      if (schema in Map) {
        schema.parent = parent;

      }
    }
  }
}