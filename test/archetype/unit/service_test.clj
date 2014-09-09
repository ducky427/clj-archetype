(ns archetype.unit.service-test
  (:require [clojure.test :refer :all]
            [archetype.service :refer :all]
            [archetype.core :as ac]
            [archetype.core-test :refer :all]
            [cheshire.core    :as cc])
  (:import (org.neo4j.test TestGraphDatabaseFactory)
           (org.neo4j.graphdb GraphDatabaseService Label Node Transaction)
           (archetype.service ArchetypeService)
           (javax.ws.rs.core Response)))

(def ^{:dynamic true} ^GraphDatabaseService *connection*)
(def ^{:dynamic true} ^ArchetypeService *service*)

(defn each-fixture [f]
  (binding [*connection*   (.newImpermanentDatabase (TestGraphDatabaseFactory.))]
    (binding [*service*    (ArchetypeService.)]
      (f))
    (.shutdown *connection*)))

(use-fixtures :each each-fixture)

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


(deftest test-identity
  (testing "Identity"
    (with-open [^Transaction tx     (.beginTx *connection*)]
      (let  [^Node node   (.createNode *connection*)]
        (.addLabel node (ac/make-label "Identity"))
        (.setProperty node "hash" valid-md5-hash)
        (.success tx)))
    (is (= valid-identity-hash
           (cc/parse-string
            (.getEntity ^Response (.getIdentity *service* valid-email "" *connection*)))))

    (is (= valid-identity-hash
           (cc/parse-string
            (.getEntity ^Response (.getIdentity *service* "" valid-md5-hash *connection*)))))
    (is (thrown-with-msg? Exception #"Identity not found"
                          (.getIdentity *service* not-found-email "" *connection*)))))
