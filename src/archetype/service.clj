(ns archetype.service
  (:import (javax.ws.rs DefaultValue GET Path QueryParam)
           (javax.ws.rs.core Context Response)
           (org.neo4j.graphdb GraphDatabaseService Node Relationship Transaction)
           (org.neo4j.tooling GlobalGraphOperations)))


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


(definterface ArcheType
  (helloWorld [])
  (warmUp [^org.neo4j.graphdb.GraphDatabaseService database]))


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


  )
