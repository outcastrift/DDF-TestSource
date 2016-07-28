package groovyExamples

import java.io.StringWriter

import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import org.json.JSONObject
import org.json.XML

output = []
def metacard = [:]
def jsonSlurper = new JsonSlurper()
def inputObj = jsonSlurper.parseText(input)

if (inputObj.data != null) {

  inputObj.data.each { testResult ->
    //thread sleep for three seconds = 3000
    sleep(300)
    metacard = [:]
    metacard.id = "testResult_metacard" + testResult['lat'] + testResult['lng']

    metacard.title = testResult['title']
    /*metacard.location = "POINT (" + testResult.geometry.location.lng +
      " " + testResult.geometry.location.lat + ")"*/
    metacard.location = testResult['location']


    JSONObject wJson = new JSONObject(JsonOutput.toJson(testResult))
    metacard.metadata = "<metadata>" + XML.toString(wJson) + "</metadata>"

    output.push(metacard)
  }
}

output = (JsonOutput.toJson(output))
