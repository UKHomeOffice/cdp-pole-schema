# cdp-pole-schema

## Purpose
This is the CDP POLE Schema; it serves three main purposes: 
1) it provides a composite JSON Schema entrypoint that creates the schema for REST API calls to create POLE entities
2) it provides a composite JSON Schema entrypoint that creates a JanusGraph schema with indices and properties
3) it provides a groovy utility to convert the Schema into something that JanusGraph can use to create indices


## Getting started

The best place to start is by looking at the test case under [PoleIngestionSpec](https://github.com/UKHomeOffice/cdp-pole-schema/blob/master/cdp-composite-schema/src/test/groovy/PoleIngestionSpec.groovy).  That has tests that validate various parts of the schema, with valid and invalid JSON files, and also has tests that validate the conversion from the JSON Schema to the format that Graph understands.

The composite JSON Schema entrypoint that creates the schema for REST API calls is here: [CDPRequestCreate.json](https://github.com/UKHomeOffice/cdp-pole-schema/blob/master/cdp-composite-schema/src/main/resources/JSONSchema/CDPRequestCreate.json)

The composite JSON Schema entrypoint that creates the JanusGraph schema with indices and properties is here: [CDPGraphSchema.json](https://github.com/UKHomeOffice/cdp-pole-schema/blob/master/cdp-composite-schema/src/main/resources/JSONSchema/CDPGraphSchema.json)
  
## Tests  

To run the test cases, simply execute `mvn install` or you can use docker `docker run --rm -ti -v $(pwd):/app -w /app maven:3.6-jdk-8-alpine mvn install`

To generate some fake requests: `docker run --rm -ti -v $(pwd):/app -w /app node:alpine sh -c "npm i;npm run faker"`

To see the schema visualised with [graphviz](https://www.graphviz.org/) compatible dot file you can do `docker run --rm -ti -v $(pwd):/app -w /app node:alpine sh -c "npm i;npm run viz"` which you can then copy/paste into http://www.webgraphviz.com/ if you don't have graphviz (dot) on your computer.

## Future direction

In the near future, we will be splitting this repo into 4 or more parts:
  - CDP core - which will be a public git repo with all the core components for POLE entitites
  - CDP meta - which will be also a public git repo with all the meta components for POLE entitites
  - <Data Owner> domain - which will be a private git repo with data-owner specific domain parts
  - <Data Owner> Schema - which will be a composite schema that pulls in parts of the three repos above, and will likely have the code in this current repo.

[repolayout.dot](https://g.cns.me/UKHomeOffice/cdp-pole-schema/blob/CNS-tidyup-review/repolayout.dot)
![](https://g.cns.me/UKHomeOffice/cdp-pole-schema/blob/CNS-tidyup-review/repolayout.dot)
