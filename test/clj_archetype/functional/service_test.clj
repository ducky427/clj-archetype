(ns clj-archetype.functional.service-test
  (:require [clojure.test :refer :all]
            [clj-archetype.service :refer :all])
  (:import (com.sun.jersey.api.client Client)
           (org.neo4j.server NeoServer)
           (org.neo4j.server.helpers CommunityServerBuilder)
           (org.neo4j.server.rest JaxRsResponse RestRequest)
           (clj_archetype.service ArchetypeService)))

(def ^{:dynamic true} ^NeoServer *server*)
(def ^{:dynamic true} ^RestRequest *request*)
(def client (Client/create))

(defn each-fixture [f]
  (binding [*server*     (.build (.withThirdPartyJaxRsPackage (CommunityServerBuilder/server)
                                                              "clj_archetype.service", "/v1"))]
    (.start *server*)
    (binding [*request*  (RestRequest. (.resolve (.baseUri *server*)
                                                 "/v1")
                                       client)]
      (f))
    (.stop *server*)
    ))

(use-fixtures :each each-fixture)


(deftest test-should-respond-to-helloWorld
  (testing "Should respond to hello world"
    (let [^JaxRsResponse response  (.get *request* "service/helloworld")]
      (is (= "Hello World!" (.getEntity response))))))
