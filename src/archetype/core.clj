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
