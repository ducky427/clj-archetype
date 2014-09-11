(ns archetype.service
  (:require [archetype.core      :as ac]
            [archetype.identity  :as ai]
            [archetype.exception :as ae]
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
           result   (map (fn [^Relationship r]
                           (.getProperty
                            (.getEndNode r)
                            prop-name))
                         (.getRelationships i (Direction/OUTGOING) (ac/get-rels rel-name)))]
      (-> result
          cc/generate-string
          (Response/ok)
          (.build)))))


(defn- create-identity-safe
  [^GraphDatabaseService db ^String hash]
  (with-open [^Transaction tx  (.beginTx db)]
    (let  [i        (IteratorUtil/singleOrNull (.findNodesByLabelAndProperty db
                                                                             (ac/make-label "Identity")
                                                                             "hash"
                                                                             hash))
           i        (if (nil? i)
                      (doto (.createNode db)
                        (.addLabel (ac/make-label "Identity"))
                        (.setProperty "hash" hash))
                      i)]
      (.success tx)
      (-> (Response/ok)
          (.build)))))


(defn create-identity
  [^GraphDatabaseService db ^String email ^String hash]
  (let [h     (ai/get-hash email hash)]
    (try
      (create-identity-safe db h)
      (catch Throwable e
        (println e)
        (if (not (instance? ConstraintViolationException e))
          (throw ae/identity-not-created)
          (-> (Response/ok)
              (.build)))))))


(definterface ArcheType
  (helloWorld [])
  (warmUp [^org.neo4j.graphdb.GraphDatabaseService database])
  (migrate [^org.neo4j.graphdb.GraphDatabaseService database])
  (getIdentity [^String email ^String hash ^org.neo4j.graphdb.GraphDatabaseService database])
  (getIdentityLikes [^String email ^String hash ^org.neo4j.graphdb.GraphDatabaseService database])
  (getIdentityHates [^String email ^String hash ^org.neo4j.graphdb.GraphDatabaseService database])
  (getIdentityKnows [^String email ^String hash ^org.neo4j.graphdb.GraphDatabaseService database])
  (createIdentity [^String email ^String hash ^org.neo4j.graphdb.GraphDatabaseService database]))


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
     (create-identity database email hash)))
