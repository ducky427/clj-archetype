(ns archetype.service
  (:import (javax.ws.rs DefaultValue GET Path QueryParam)
           (javax.ws.rs.core Context Response)
           (org.neo4j.graphdb GraphDatabaseService Transaction)))


(definterface ArcheType
  (helloWorld []))


(deftype ^{Path "/service"} ArchetypeService []
         ArcheType
         (^{GET true
            Path "/helloworld"}
          helloWorld
          [this]
            (require 'clj-archetype.service)
            "Hello World!"))
