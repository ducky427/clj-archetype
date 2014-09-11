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
    (is (= {"error" "Invalid E-mail Parameter."} (get-request-json "service/identity?email=invalid")))
    (is (= {"error" "Invalid MD5Hash Parameter."} (get-request-json "service/identity?md5hash=invalid")))
    (is (= {"error" "Identity not found."} (get-request-json (str "service/identity?email=" not-found-email))))))


(deftest test-identity-likes
  (testing "Identity Likes"
    (let   [^GraphDatabaseService db  (.getGraph (.getDatabase *server*))]
      (with-open [^Transaction tx     (.beginTx db)]
        (let  [^Node node   (.createNode db)
               ^Node page1  (.createNode db)
               ^Node page2  (.createNode db)]
          (.addLabel node (ac/make-label "Identity"))
          (.addLabel page1 (ac/make-label "Page"))
          (.addLabel page2 (ac/make-label "Page"))
          (.setProperty node "hash" valid-md5-hash)
          (.setProperty page1 "url" valid-url)
          (.setProperty page2 "url" valid-url2)
          (.createRelationshipTo node page1 (ac/make-rel "LIKES"))
          (.createRelationshipTo node page2 (ac/make-rel "LIKES"))
          (.success tx))))
    (is (= valid-identity-pages
           (get-request-json (str "service/identity/likes?email=" valid-email))))
    (is (= valid-identity-pages
           (get-request-json (str "service/identity/likes?md5hash=" valid-md5-hash))))
    (is (= {"error" "Missing Query Parameters."} (get-request-json "service/identity/likes")))
    (is (= {"error" "Invalid E-mail Parameter."} (get-request-json "service/identity/likes?email=invalid")))
    (is (= {"error" "Invalid MD5Hash Parameter."} (get-request-json "service/identity/likes?md5hash=invalid")))
    (is (= {"error" "Identity not found."} (get-request-json (str "service/identity/likes?email=" not-found-email))))))


(deftest test-identity-hates
  (testing "Identity Hates"
    (let   [^GraphDatabaseService db  (.getGraph (.getDatabase *server*))]
      (with-open [^Transaction tx     (.beginTx db)]
        (let  [^Node node   (.createNode db)
               ^Node page1  (.createNode db)
               ^Node page2  (.createNode db)]
          (.addLabel node (ac/make-label "Identity"))
          (.addLabel page1 (ac/make-label "Page"))
          (.addLabel page2 (ac/make-label "Page"))
          (.setProperty node "hash" valid-md5-hash)
          (.setProperty page1 "url" valid-url)
          (.setProperty page2 "url" valid-url2)
          (.createRelationshipTo node page1 (ac/make-rel "HATES"))
          (.createRelationshipTo node page2 (ac/make-rel "HATES"))
          (.success tx))))
    (is (= valid-identity-pages
           (get-request-json (str "service/identity/hates?email=" valid-email))))
    (is (= valid-identity-pages
           (get-request-json (str "service/identity/hates?md5hash=" valid-md5-hash))))
    (is (= {"error" "Missing Query Parameters."} (get-request-json "service/identity/hates")))
    (is (= {"error" "Invalid E-mail Parameter."} (get-request-json "service/identity/hates?email=invalid")))
    (is (= {"error" "Invalid MD5Hash Parameter."} (get-request-json "service/identity/hates?md5hash=invalid")))
    (is (= {"error" "Identity not found."} (get-request-json (str "service/identity/hates?email=" not-found-email))))))


(deftest test-identity-knows
  (testing "Identity knows"
    (let   [^GraphDatabaseService db  (.getGraph (.getDatabase *server*))]
      (with-open [^Transaction tx     (.beginTx db)]
        (let  [^Node node   (.createNode db)
               ^Node know1  (.createNode db)
               ^Node know2  (.createNode db)]
          (.addLabel node (ac/make-label "Identity"))
          (.addLabel know1 (ac/make-label "Identity"))
          (.addLabel know2 (ac/make-label "Identity"))
          (.setProperty node "hash" valid-md5-hash)
          (.setProperty know1 "hash" valid-md5-hash2)
          (.setProperty know2 "hash" valid-md5-hash3)
          (.createRelationshipTo node know1 (ac/make-rel "KNOWS"))
          (.createRelationshipTo node know2 (ac/make-rel "KNOWS"))
          (.success tx))))
    (is (= valid-identity-knows
           (get-request-json (str "service/identity/knows?email=" valid-email))))
    (is (= valid-identity-knows
           (get-request-json (str "service/identity/knows?md5hash=" valid-md5-hash))))
    (is (= {"error" "Missing Query Parameters."} (get-request-json "service/identity/knows")))
    (is (= {"error" "Invalid E-mail Parameter."} (get-request-json "service/identity/knows?email=invalid")))
    (is (= {"error" "Invalid MD5Hash Parameter."} (get-request-json "service/identity/knows?md5hash=invalid")))
    (is (= {"error" "Identity not found."} (get-request-json (str "service/identity/knows?email=" not-found-email))))))


(deftest test-identity-create
  (testing "Identity create"
    (let   [^GraphDatabaseService db  (.getGraph (.getDatabase *server*))]
      (with-open [^Transaction tx     (.beginTx db)]
        (let  [^Node node   (.createNode db)]
          (.addLabel node (ac/make-label "Identity"))
          (.setProperty node "hash" valid-md5-hash2)
          (.success tx))))
    (is (= 200
           (.getStatus ^JaxRsResponse (.post *request* (str "service/identity?email=" valid-email) ""))))
    (is (= 200
           (.getStatus ^JaxRsResponse (.post *request* (str "service/identity?md5hash=" valid-md5-hash) ""))))
    (is (= 200
           (.getStatus ^JaxRsResponse (.post *request* (str "service/identity?md5hash=" valid-md5-hash2) ""))))

    (is (= {"error" "Missing Query Parameters."}
           (cc/parse-string (.getEntity ^JaxRsResponse (.post *request* "service/identity" "")))))
    (is (= {"error" "Invalid E-mail Parameter."}
           (cc/parse-string (.getEntity ^JaxRsResponse (.post *request* "service/identity?email=invalid" "")))))
    (is (= {"error" "Invalid MD5Hash Parameter."}
           (cc/parse-string (.getEntity ^JaxRsResponse (.post *request* "service/identity?md5hash=invalid" "")))))
    (-> *server*
        (.getDatabase)
        (.getGraph)
        (.shutdown))
    (is (= {"error" "Unable to create Identity."}
           (cc/parse-string (.getEntity ^JaxRsResponse
                                        (.post *request* (str "service/identity?md5hash=" valid-md5-hash) "")))))))
