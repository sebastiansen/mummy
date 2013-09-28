(defproject mummy "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clout "1.1.0"]
                 [korma "0.3.0-RC5"]
                 [ring/ring-json "0.2.0"]]
  :profiles {:dev {:dependencies
                   [[ring-mock "0.1.3" :exclusions [org.clojure/clojure]]
                    [clj-time "0.6.0"]]}})
