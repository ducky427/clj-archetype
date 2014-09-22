(ns archetype.service
  (:require [archetype.core      :as ac]
            [archetype.identity  :as ai]
            [archetype.exception :as ae]
            [archetype.page      :as ag]
            [clojure.string      :as s]
            [cheshire.core       :as cc])
  (:import (java.util.concurrent TimeUnit)
           (javax.ws.rs DefaultValue GET Path POST QueryParam)
           (javax.ws.rs.core Context Response)
           (org.neo4j.helpers.collection IteratorUtil)
           (org.neo4j.graphdb ConstraintViolationException Direction GraphDatabaseService
                              Label Node Relationship RelationshipType Transaction)
           (org.neo4j.graphdb.schema Schema)
           (org.neo4j.tooling GlobalGraphOperations))
  (:refer-clojure :exclude [hash]))


(defn warm-up-rel
  [^Relationship r]
  (.getPropertyKeys r)
  (.getStartNode r))


(defn warm-up-node
  [^Node n]
  (.getPropertyKeys n)
  (dorun (map warm-up-rel (.getRelationships n))))


(defn get-rel-props
  [^Node start-node ^String rel-name ^String prop-name]
  (map (fn [^Relationship r]
         (.getProperty
          (.getEndNode r)
          prop-name))
       (.getRelationships start-node (Direction/OUTGOING) (ac/get-rels rel-name))))


(defn warm-up
  [^GraphDatabaseService db]
  (with-open [^Transaction tx  (.beginTx db)]
    (dorun (map warm-up-node (.getAllNodes (GlobalGraphOperations/at db)))))
  "Warmed up and ready to go!")


(defn migrated?
  [^GraphDatabaseService db]
  (with-open [^Transaction tx  (.beginTx db)]
    (-> db
        (.schema)
        (.getConstraints)
        (.iterator)
        (.hasNext))))


(defn perform-migration
  [^GraphDatabaseService db]
  (with-open [^Transaction tx  (.beginTx db)]
    (let [^Schema schema  (.schema db)]
      (-> schema
          (.constraintFor (ac/make-label "Identity"))
          (.assertPropertyIsUnique "hash")
          (.create))
      (-> schema
          (.constraintFor (ac/make-label "Page"))
          (.assertPropertyIsUnique "url")
          (.create))
      (.success tx))))


(defn migrate
  [^GraphDatabaseService db]
  (if (migrated? db)
    "Already Migrated!"
    (do
      (perform-migration db)
      (with-open [^Transaction tx  (.beginTx db)]
        (let [^Schema schema  (.schema db)]
          (.awaitIndexesOnline schema 1 (TimeUnit/DAYS))
          "Migrated!")))))


(defn get-identity
  [^GraphDatabaseService db ^String email ^String hash]
  (with-open [^Transaction tx  (.beginTx db)]
    (let  [hash     (ai/get-hash email hash)
           i        (ai/get-identity-node hash db)]
      (-> {"identity" (.getProperty i "hash")}
          cc/generate-string
          (Response/ok)
          (.build)))))


(defn get-identity-rel
  [^GraphDatabaseService db ^String email ^String hash rel-name prop-name]
  (with-open [^Transaction tx  (.beginTx db)]
    (let  [hash     (ai/get-hash email hash)
           i        (ai/get-identity-node hash db)
           result   (get-rel-props i rel-name prop-name)]
      (-> result
          cc/generate-string
          (Response/ok)
          (.build)))))

(defn- create-node
  [^GraphDatabaseService db ^String label ^String prop-name ^String prop-value]
  (with-open [^Transaction tx  (.beginTx db)]
    (let  [l        (ac/make-label label)
           i        (IteratorUtil/singleOrNull (.findNodesByLabelAndProperty db
                                                                             l
                                                                             prop-name
                                                                             prop-value))
           i        (if (nil? i)
                      (doto (.createNode db)
                        (.addLabel l)
                        (.setProperty prop-name prop-value))
                      i)]
      (.success tx)
      (-> (Response/ok)
          (.build)))))


(defn create-identity
  [^GraphDatabaseService db ^String email ^String hash]
  (let [h     (ai/get-hash email hash)]
    (try
      (create-node db "Identity" "hash" h)
      (catch Throwable e
        (if (not (instance? ConstraintViolationException e))
          (throw ae/identity-not-created)
          (-> (Response/ok)
              (.build)))))))


(defn create-page
  [^GraphDatabaseService db ^String url]
  (let [u     (ag/get-page-url url)]
    (try
      (create-node db "Page" "url" u)
      (catch Throwable e
        (if (not (instance? ConstraintViolationException e))
          (throw ae/page-not-created)
          (-> (Response/ok)
              (.build)))))))


(defn get-page
  [^GraphDatabaseService db ^String url]
  (if (s/blank? url)
    (throw ae/missing-params)
    (with-open [^Transaction tx   (.beginTx db)]
      (let [^Node page   (ag/get-page-node url db)]
        (-> {"url" (.getProperty page "url")}
            cc/generate-string
            (Response/ok)
            (.build))))))


(defn get-page-links
  [^GraphDatabaseService db ^String url]
  (if (s/blank? url)
    (throw ae/missing-params)
    (with-open [^Transaction tx  (.beginTx db)]
      (let [^Node page   (ag/get-page-node url db)
            result       (get-rel-props page "LINKS" "url")]
        (-> result
            cc/generate-string
            (Response/ok)
            (.build))))))


(definterface ArcheType
  (helloWorld [])
  (warmUp [^org.neo4j.graphdb.GraphDatabaseService database])
  (migrate [^org.neo4j.graphdb.GraphDatabaseService database])
  (getIdentity [^String email ^String hash ^org.neo4j.graphdb.GraphDatabaseService database])
  (getIdentityLikes [^String email ^String hash ^org.neo4j.graphdb.GraphDatabaseService database])
  (getIdentityHates [^String email ^String hash ^org.neo4j.graphdb.GraphDatabaseService database])
  (getIdentityKnows [^String email ^String hash ^org.neo4j.graphdb.GraphDatabaseService database])
  (createIdentity [^String email ^String hash ^org.neo4j.graphdb.GraphDatabaseService database])
  (createPage [^String url ^org.neo4j.graphdb.GraphDatabaseService database])
  (getPage [^String url ^org.neo4j.graphdb.GraphDatabaseService database])
  (getPageLinks [^String url ^org.neo4j.graphdb.GraphDatabaseService database]))


(deftype ^{Path "/service"} ArchetypeService
  []
  ArcheType
  (^{GET true
     Path "/helloworld"}
   helloWorld
   [this]
     (require 'clj-archetype.service)
     "Hello World!")

  (^{GET true
     Path "/warmup"}
   warmUp
   [this ^{Context true} database]
     (require 'clj-archetype.service)
     (warm-up database))

  (^{GET true
     Path "/migrate"}
   migrate
   [this ^{Context true} database]
     (require 'clj-archetype.service)
     (migrate database))

  (^{GET true
     Path "/identity"}
   getIdentity
   [this ^{DefaultValue "" QueryParam "email"} email
    ^{DefaultValue "" QueryParam "md5hash"} hash
    ^{Context true} database]
     (require 'clj-archetype.service)
     (get-identity database email hash))

  (^{GET true
     Path "/identity/likes"}
   getIdentityLikes
   [this ^{DefaultValue "" QueryParam "email"} email
    ^{DefaultValue "" QueryParam "md5hash"} hash
    ^{Context true} database]
     (require 'clj-archetype.service)
     (get-identity-rel database email hash "LIKES" "url"))

  (^{GET true
     Path "/identity/hates"}
   getIdentityHates
   [this ^{DefaultValue "" QueryParam "email"} email
    ^{DefaultValue "" QueryParam "md5hash"} hash
    ^{Context true} database]
     (require 'clj-archetype.service)
     (get-identity-rel database email hash "HATES" "url"))

  (^{GET true
     Path "/identity/knows"}
   getIdentityKnows
   [this ^{DefaultValue "" QueryParam "email"} email
    ^{DefaultValue "" QueryParam "md5hash"} hash
    ^{Context true} database]
     (require 'clj-archetype.service)
     (get-identity-rel database email hash "KNOWS" "hash"))

  (^{POST true
     Path "/identity"}
   createIdentity
   [this ^{DefaultValue "" QueryParam "email"} email
    ^{DefaultValue "" QueryParam "md5hash"} hash
    ^{Context true} database]
     (require 'clj-archetype.service)
     (create-identity database email hash))


  (^{POST true
     Path "/page"}
   createPage
   [this ^{DefaultValue "" QueryParam "url"} url
    ^{Context true} database]
     (require 'clj-archetype.service)
     (create-page database url))

  (^{GET true
     Path "/page"}
   getPage
   [this ^{DefaultValue "" QueryParam "url"} url
    ^{Context true} database]
     (require 'clj-archetype.service)
     (get-page database url))


  (^{GET true
     Path "/page/links"}
   getPageLinks
   [this ^{DefaultValue "" QueryParam "url"} url
    ^{Context true} database]
     (require 'clj-archetype.service)
     (get-page-links database url)))
