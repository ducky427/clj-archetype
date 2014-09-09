(ns archetype.functional.service-test
  (:require [clojure.test :refer :all]
            [archetype.core  :as ac]
            [archetype.core-test :refer :all]
            [cheshire.core :as cc])
  (:import (com.sun.jersey.api.client Client)
           (org.neo4j.graphdb GraphDatabaseService Label Node Transaction)
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

(use-fixtures :each each-fixture)


(defn get-request-result
  [^String url]
  (let [^JaxRsResponse response  (.get *request* url)]
    (.getEntity response)))


(defn get-request-json
  [^String url]
  (cc/parse-string (get-request-result url)))


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


(deftest test-identity
  (testing "Identity"
    (let   [^GraphDatabaseService db  (.getGraph (.getDatabase *server*))]
      (with-open [^Transaction tx     (.beginTx db)]
        (let  [^Node node   (.createNode db)]
          (.addLabel node (ac/make-label "Identity"))
          (.setProperty node "hash" valid-md5-hash)
          (.success tx))))
    (is (= valid-identity-hash
           (get-request-json (str "service/identity?email=" valid-email))))
    (is (= valid-identity-hash
           (get-request-json (str "service/identity?md5hash=" valid-md5-hash))))
    (is (= {"error" "Missing Query Parameters."} (get-request-json "service/identity")))
    (is (= {"error" "Missing E-mail Parameter."} (get-request-json "service/identity?email=invalid")))
    (is (= {"error" "Missing MD5Hash Parameter."} (get-request-json "service/identity?md5hash=invalid")))
    (is (= {"error" "Identity not found."} (get-request-json (str "service/identity?email=" not-found-email))))))
