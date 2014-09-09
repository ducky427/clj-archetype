(def neo4j-version "2.1.3")

(defproject clj-archetype "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.neo4j/neo4j ~neo4j-version]
                 [org.neo4j/neo4j-kernel ~neo4j-version :classifier "tests"]
                 [org.neo4j/server-api ~neo4j-version]
                 [org.neo4j.app/neo4j-server ~neo4j-version]
                 [org.neo4j.app/neo4j-server ~neo4j-version :classifier "tests"]
                 [com.sun.jersey/jersey-client "1.9"]
                 [com.google.guava/guava "18.0"]
                 [commons-validator/commons-validator "1.4.0"]
                 [junit/junit "4.11"]
                 [cheshire "5.3.1"]]
  :plugins [[lein-cloverage "1.0.2"]
            [lein-ancient "0.5.5"]
            [jonase/eastwood "0.1.4"]
            [lein-kibit "0.0.8"]]
  :target-path "target/%s"
  :aot :all
  :global-vars {*warn-on-reflection* true}
  :profiles {:provided {:dependencies []}})
