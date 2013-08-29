# vone

Custom reporting for Version One.

Hosted version: http://vonespy.appspot.com/

## Usage

Install [Leiningen](https://github.com/technomancy/leiningen)
```
lein deps
lein run
```

### Custom install

To generate a war file for deployment:
`lein ring uberwar vone.war`

in src/vone/models/version-one-request.clj:
```clojure
(def base-url "http://www.host.com/Customer/VersionOne/rest-1.v1")
```

### Google App Engine: vonespy.appspot.com

* replace pmap with map in vone.models.queries
* `lein appengine-prepare`
* edit war/WEB-INF/appengine-web.xml inc version
* appcfg.sh update war

## License

Copyright (C) 2011
Distributed under the Eclipse Public License, the same as Clojure.

