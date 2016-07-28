# DDF Creations and Experiments By Sam Davis

### To build just execute (Note you may need to add modules at the top level parent pom for the project.)

    mvn clean install
## Hard OSGI Is
![Screenshot](https://raw.githubusercontent.com/outcastrift/DDF-Creations/master/yoda.png)


## Experiment : Test Bed Source

This bundle shows a implementation of a REST service that is configured as Federated Source as well as a Source that can be hit via a Groovy Script configuration. 

The REST service provides a endpoint that generates results from two separate endpoints.  
One returns a array of simple Object that has a ID and a randomly generated Lat/Lon, this is to be used with the Groovy Source.  
The other returns a array of complex objects that contain ID, Date, Report Link, Random Lat/Lon, Summary, Orginator, Event Type.

- GET /test/getGroovyResults?amount= - to get simple objects based on amount
- GET /test/getSourceResults?amount=2&startDate=20160301083000&endDate=20160316093000    Get amount fo Simple Objects within Date Range


### Building and Installation
Deploy the bundle.

    
Afterwards in the target directory of this module you will see the oadcgs-ddf-test-source-1.0.jar, place this jar into the DDF deploy directory. 

At this time the Federated Source Endpoint is already running and can be accessed via the url
   
    https://localhost:8993/services/test/getGroovyResults
    
Or

    https://localhost:8993/services/test/getSourceResults

The groovy source has not yet been configured in order to configure it you must navigate to:

    https://localhost:8993/admin

Go down to the option titled TestBedFederatedSource and click it to start creation of the source. 
Within the configuration very no fields have to be configured if you are using the test source endpoint just click save. 
 Otherwise just fill out the blocks according to the external REST source. 


### Running the Test Bed Source 

It is assumed a DDF platform is already running. 

When the example runs in DDF, you can use the DDF console to check its status 

To list all the running bundles:

    list

Then find the bundle number on the left hand side of the console that has the name of the test bed source on the right:

    headers $BUNDLE_NUMBER


### Access endpoints using a web browser

You can use any browser to perform a HTTP GET.  This allows you to very easily test a few of the REST Endpoints:


Use this URL to display the root services for DDF :

    https://localhost:8993/services


### To run a command-line utility:

You can use a command-line utility, such as cURL or wget, to perform the HTTP requests.  

1. Open a command prompt or terminal.
2. Run the following curl commands (curl commands may not be available on all platforms):

    * Retrieves two federated source results.

            curl https://localhost:8993/services/test/getSourceResults?amount=2&startDate=20160301083000&endDate=20160316093000 

    * Retrieves two generic result objects. 

            curl https://localhost:8993/services/test/getGroovyResults?amount=2
            