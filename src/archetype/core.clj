(ns archetype.core
  (:import (org.neo4j.graphdb DynamicLabel DynamicRelationshipType Label RelationshipType)))

(defn make-label
  ^Label
  [^String x]
  (DynamicLabel/label x))

(defn make-rel
  ^RelationshipType
  [^String x]
  (DynamicRelationshipType/withName x))


(defn #^"[Lorg.neo4j.graphdb.DynamicRelationshipType;" get-rels
  [rel-name]
  (into-array ^RelationshipType [(make-rel rel-name)]))
