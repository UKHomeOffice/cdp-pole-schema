const jsf = require("json-schema-faker")
jsf.extend('faker', () => require('faker'))
jsf.extend('chance', () => require('chance'))

const batchSize = 1000

const lots = {
  type: "array",
  minItems: batchSize,
  maxItems: batchSize,
  items: {
    type: "object",
    "$ref": "CDPRequestCreate.json",
  }
}

jsf.resolve(lots, null, "cdp-composite-schema/src/main/resources/JSONSchema")
  .then(fakes => process.stdout.write(JSON.stringify(fakes)))

