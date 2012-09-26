# vone

Custom reporting for Version One

## Usage

Install Leiningen https://github.com/technomancy/leiningen
lein deps
lein run

To generate a war file for deployment:
lein ring uberwar vone.war

Google App Engine: vonespy
replace pmap with map in vone.models.queries
lein appengine-prepare
edit war/WEB-INF/appengine-web.xml inc version
appcfg.sh update war

## License

Copyright (C) 2011
Distributed under the Eclipse Public License, the same as Clojure.

