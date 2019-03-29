# cdp-pole-schema

This is the CDP POLE Schema; it serves two main purposes: 
1) it provides a composite entrypoint that creates the schema for REST API calls to create POLE entities
2) it also has a groovy utility to convert the Schema into something that JanusGraph can use to create indices

The best place to start is by looking at the test case under [[https://github.com/UKHomeOffice/cdp-pole-schema/blob/master/cdp-composite-schema/src/test/groovy/PoleIngestionSpec.groovy]]
