var process = require("process");
var fs = require("fs")
var path = require("path")
var moveFrom = "cdp-composite-schema/src/main/resources/JSONSchema";

const Promise = require("bluebird")
var readdir = Promise.promisify(fs.readdir);
var fsstat = Promise.promisify(fs.stat);
const _ = require("lodash")

const relationships = []
const filelist = []

const dir_handle = (dir) =>
  readdir(dir)
    .map(file => path.join(dir, file))
    .map(file => fsstat(file).then(stat => stat.isDirectory() ? dir_handle(file) : file))
    .then(_.flattenDeep)

process.stdout.write("digraph{\n  node [shape=rectangle];")

const reformat_filenames = filename => filename.replace(/\.json/g, "").replace(/\//g, "_")

dir_handle(moveFrom)
  .tap(files => files.map(file => filelist.push(reformat_filenames(path.relative(moveFrom, file)))))
  .map(file =>
    fs.readFileSync(file).toString().split("\n")
      .map(line => line.includes("$ref") ? line.match(/(?<=ref"\: ").*(?=")/)[0] : null)
      .filter(line => line !== null)
      .map(relation => `${path.dirname(file)}/${relation}`)
      .map(relation => `${path.relative(moveFrom, file)} -> ${path.relative(moveFrom, relation)}`)
      .map(reformat_filenames)
      .map(relation => relationships.push(relation))
  )
  .then(() => process.stdout.write(
    `

  ${filelist.join(";\n  ")}

  ${relationships.join(";\n  ")}
}`))