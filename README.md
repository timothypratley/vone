# vone

Custom reporting for Version One.

Hosted: http://vonespy.appspot.com/

## Usage

Create a vone.properties file containing:
```java
username = username
password = password
base-url = http://www3.v1host.com/MyCompany/VersionOne/rest-1.v1
```
username/password are optional, the user will be prompted if no account is specified

Install [Leiningen](https://github.com/technomancy/leiningen)
```
lein ring server
```

### Tomcat install

To generate a war file for deployment:
`lein ring uberwar vone.war`

### Google App Engine: vonespy.appspot.com

* replace pmap with map in vone.models.queries
* `lein appengine-prepare`
* edit war/WEB-INF/appengine-web.xml inc version
* appcfg.sh update war

### Running tests

`lein midje`


## License

Copyright (C) 2011
Distributed under the Eclipse Public License, the same as Clojure.

## Development notes

The principle is to expose an API of webservices that consume VersionOne webservices, process the data, and spit out data.
All the services can spit out json/csv/or google charts datasource format, depending on the url you access them with.
So the frontend is doing requests to the “vone API” – usually by a chart datasource.

Adding a query consists of constructing the URL that gets useful data from VersionOne:
https://www.host.com/company/VersionOne/rest-1.v1/Data/PrimaryWorkitem?sel=Number&where=Timebox.Name='X'
experimenting with the query in a browser is pretty convenient
Create a function in vone which will construct said query using inputs (sprint in this case)
Create a function which will transform the results (results are processed into a sequence of property maps so you don’t have to mess with XML)
(in this case we might output a table of odd numbered stories and even numbered stories)
Then adding a frontend component with its datasource pointed at the vone API.

