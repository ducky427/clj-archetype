(ns archetype.service
  (:import (java.util.concurrent TimeUnit)
           (javax.ws.rs DefaultValue GET Path QueryParam)
           (javax.ws.rs.core Context Response)
           (org.neo4j.graphdb DynamicLabel GraphDatabaseService Node Relationship Transaction)
           (org.neo4j.graphdb.schema Schema)
           (org.neo4j.tooling GlobalGraphOperations)))

(defn make-label
  [^String x]
  (DynamicLabel/label x))


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
          (.constraintFor (make-label "Identity"))
          (.assertPropertyIsUnique "hash")
          (.create))
      (-> schema
          (.constraintFor (make-label "Page"))
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


(definterface ArcheType
  (helloWorld [])
  (warmUp [^org.neo4j.graphdb.GraphDatabaseService database])
  (migrate [^org.neo4j.graphdb.GraphDatabaseService database]))


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


  )
