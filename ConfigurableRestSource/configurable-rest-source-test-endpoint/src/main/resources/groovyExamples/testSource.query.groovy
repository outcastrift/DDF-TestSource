package groovyExamples

import groovy.json.JsonSlurper;
import groovy.json.JsonOutput;

//url=https://localhost:8993/services/test/getGroovyResults
output = []
def param = [:]
def jsonSlurper = new JsonSlurper()
def inputObj = jsonSlurper.parseText(input)

if (inputObj.contextualSearch != null) {
  param.name = "amount"
  param.value = inputObj.contextualSearch.searchPhrase
  output << param
}

output = (JsonOutput.toJson(output))
