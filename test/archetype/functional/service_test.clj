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


(deftest test-should-respond-to-helloWorld
  (testing "Should respond to hello world"
    (let [^JaxRsResponse response  (.get *request* "service/helloworld")]
      (is (= "Hello World!" (.getEntity response))))))


(deftest test-should-warm-up
  (testing "Should warm up"
    (let [^JaxRsResponse response  (.get *request* "service/warmup")]
      (is (= "Warmed up and ready to go!" (.getEntity response))))))
