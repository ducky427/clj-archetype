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


(deftest test-identity-likes
  (testing "Identity Likes"
    (with-open [^Transaction tx     (.beginTx *connection*)]
      (let  [^Node node   (.createNode *connection*)
             ^Node page1  (.createNode *connection*)
             ^Node page2  (.createNode *connection*)]
        (.addLabel node (ac/make-label "Identity"))
        (.addLabel page1 (ac/make-label "Page"))
        (.addLabel page2 (ac/make-label "Page"))
        (.setProperty node "hash" valid-md5-hash)
        (.setProperty page1 "url" valid-url)
        (.setProperty page2 "url" valid-url2)
        (.createRelationshipTo node page1 (ac/make-rel "LIKES"))
        (.createRelationshipTo node page2 (ac/make-rel "LIKES"))
        (.success tx)))
    (is (= valid-identity-pages
           (cc/parse-string
            (.getEntity ^Response (.getIdentityLikes *service* valid-email "" *connection*)))))

    (is (= valid-identity-pages
           (cc/parse-string
            (.getEntity ^Response (.getIdentityLikes *service* "" valid-md5-hash *connection*)))))
    (is (thrown-with-msg? Exception #"Identity not found"
                          (.getIdentityLikes *service* not-found-email "" *connection*)))))


(deftest test-identity-hates
  (testing "Identity Hates"
    (with-open [^Transaction tx     (.beginTx *connection*)]
      (let  [^Node node   (.createNode *connection*)
             ^Node page1  (.createNode *connection*)
             ^Node page2  (.createNode *connection*)]
        (.addLabel node (ac/make-label "Identity"))
        (.addLabel page1 (ac/make-label "Page"))
        (.addLabel page2 (ac/make-label "Page"))
        (.setProperty node "hash" valid-md5-hash)
        (.setProperty page1 "url" valid-url)
        (.setProperty page2 "url" valid-url2)
        (.createRelationshipTo node page1 (ac/make-rel "HATES"))
        (.createRelationshipTo node page2 (ac/make-rel "HATES"))
        (.success tx)))
    (is (= valid-identity-pages
           (cc/parse-string
            (.getEntity ^Response (.getIdentityHates *service* valid-email "" *connection*)))))

    (is (= valid-identity-pages
           (cc/parse-string
            (.getEntity ^Response (.getIdentityHates *service* "" valid-md5-hash *connection*)))))
    (is (thrown-with-msg? Exception #"Identity not found"
                          (.getIdentityHates *service* not-found-email "" *connection*)))))


(deftest test-identity-knows
  (testing "Identity knows"
    (with-open [^Transaction tx     (.beginTx *connection*)]
      (let  [^Node node   (.createNode *connection*)
             ^Node know1  (.createNode *connection*)
             ^Node know2  (.createNode *connection*)]
        (.addLabel node (ac/make-label "Identity"))
        (.addLabel know1 (ac/make-label "Identity"))
        (.addLabel know2 (ac/make-label "Identity"))
        (.setProperty node "hash" valid-md5-hash)
        (.setProperty know1 "hash" valid-md5-hash2)
        (.setProperty know2 "hash" valid-md5-hash3)
        (.createRelationshipTo node know1 (ac/make-rel "KNOWS"))
        (.createRelationshipTo node know2 (ac/make-rel "KNOWS"))
        (.success tx)))
    (is (= valid-identity-knows
           (cc/parse-string
            (.getEntity ^Response (.getIdentityKnows *service* valid-email "" *connection*)))))

    (is (= valid-identity-knows
           (cc/parse-string
            (.getEntity ^Response (.getIdentityKnows *service* "" valid-md5-hash *connection*)))))
    (is (thrown-with-msg? Exception #"Identity not found"
                          (.getIdentityKnows *service* not-found-email "" *connection*)))))


(deftest test-identity-create
  (testing "Identity create"
    (with-open [^Transaction tx     (.beginTx *connection*)]
      (let  [^Node node   (.createNode *connection*)]
        (.addLabel node (ac/make-label "Identity"))
        (.setProperty node "hash" valid-md5-hash2)
        (.success tx)))
    (is (= 200
           (.getStatus ^Response (.createIdentity *service* valid-email "" *connection*))))

    (is (= 200
           (.getStatus ^Response (.createIdentity *service* "" valid-md5-hash2 *connection*))))

    (is (= 200
           (.getStatus ^Response (.createIdentity *service* "" valid-md5-hash *connection*))))

    (.shutdown *connection*)

    (is (thrown-with-msg? Exception #"Unable to create"
                          (.createIdentity *service* "" valid-md5-hash *connection*)))))
