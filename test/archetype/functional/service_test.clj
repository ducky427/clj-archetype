(ns archetype.functional.service-test
  (:require [clojure.test :refer :all])
  (:import (com.sun.jersey.api.client Client)
           (org.neo4j.server NeoServer)
           (org.neo4j.server.helpers CommunityServerBuilder)
           (org.neo4j.server.rest JaxRsResponse RestRequest)))

(def ^{:dynamic true} ^NeoServer *server*)
(def ^{:dynamic true} ^RestRequest *request*)
(def client (Client/create))

(defn each-fixture [f]
  (binding [*server*     (-> (CommunityServerBuilder/server)
                             (.withThirdPartyJaxRsPackage "archetype.service" "/v1")
                             (.build))]
    (.start *server*)
    (binding [*request*  (-> (.baseUri *server*)
                             (.resolve "/v1")
                             (RestRequest.))]
      (f))
    (.stop *server*)))

(use-fixtures :once each-fixture)

(defn get-request-result
  [^String url]
  (let [^JaxRsResponse response  (.get *request* url)]
    (.getEntity response)))


(deftest test-should-respond-to-helloWorld
  (testing "Should respond to hello world"
    (is (= "Hello World!" (get-request-result "service/helloworld")))))


(deftest test-should-warm-up
  (testing "Should warm up"
    (is (= "Warmed up and ready to go!" (get-request-result "service/warmup")))))


(deftest test-should-migrate
  (testing "Should migrate"
    (is (= "Migrated!" (get-request-result "service/migrate")))
    (is (= "Already Migrated!" (get-request-result "service/migrate")))))
