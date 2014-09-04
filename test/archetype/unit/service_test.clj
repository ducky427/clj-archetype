(ns archetype.unit.service-test
  (:require [clojure.test :refer :all]
            [archetype.service :refer :all])
  (:import (org.neo4j.test TestGraphDatabaseFactory)
           (org.neo4j.graphdb GraphDatabaseService)
           (archetype.service ArchetypeService)))

(def ^{:dynamic true} ^GraphDatabaseService *connection*)
(def ^{:dynamic true} ^ArchetypeService *service*)

(defn each-fixture [f]
  (binding [*connection*   (.newImpermanentDatabase (TestGraphDatabaseFactory.))]
    (binding [*service*    (ArchetypeService.)]
      (f))
    (.shutdown *connection*)))

(use-fixtures :once each-fixture)

(deftest test-should-respond-to-helloWorld
  (testing "Should respond to hello world"
    (is (= "Hello World!" (.helloWorld *service*)))))


(deftest test-should-warm-up
  (testing "Should warm up"
    (is (= "Warmed up and ready to go!" (.warmUp *service* *connection*)))))


(deftest test-migrate
  (testing "Should migrate"
    (is (= "Migrated!" (.migrate *service* *connection*)))
    (is (= "Already Migrated!" (.migrate *service* *connection*)))))
