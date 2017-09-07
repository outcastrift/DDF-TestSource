import groovy.json.JsonOutput
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
output = []
def param = [:]
def jsonSlurper = new JsonSlurper()
def inputObj = jsonSlurper.parseText(input)

if (inputObj?.contextualSearch?.searchPhrase != null) {
    def phrase
    if(inputObj?.contextualSearch?.searchPhrase == "searchPhrase"){
        phrase = "1"
    }else{
        phrase = inputObj.contextualSearch.searchPhrase
    }
    param.name = "amount"
    param.value = phrase
    output << param
}
output = (JsonOutput.toJson(output))

