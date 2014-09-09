(ns archetype.identity
  (:require [clojure.string :as s]
            [archetype.core :refer [make-label]]
            [archetype.exception :as ae])
  (:import (com.google.common.base Charsets)
           (com.google.common.hash HashCode Hashing)
           (org.apache.commons.validator.routines EmailValidator)
           (org.neo4j.graphdb GraphDatabaseService Label Node)
           (org.neo4j.helpers.collection IteratorUtil)))


(defn valid-email?
  [^String email]
  (.isValid ^EmailValidator (EmailValidator/getInstance) email))


(defn valid-md5?
  [^String hash]
  (re-matches #"[a-f0-9]{32}" hash))


(defn calculate-hash
  [^String input]
  (-> (Hashing/md5)
      (.newHasher)
      (.putString (s/lower-case input) (Charsets/UTF_8))
      (.hash)
      str))


(defn get-hash
  [^String email ^String hash]
  (let  [hash-l  (s/lower-case hash)]
    (cond
     (and (s/blank? hash) (s/blank? email)) (throw ae/missing-params)
     (and (s/blank? hash) (valid-email? email)) (calculate-hash email)
     (s/blank? hash) (throw ae/invalid-email-param)
     (valid-md5? hash-l) hash-l
     :else (throw ae/invalid-md5-param))))


(defn ^Node get-identity-node
  [^String hash ^GraphDatabaseService db]
  (let  [^Node i    (IteratorUtil/singleOrNull (.findNodesByLabelAndProperty db
                                                                             (make-label "Identity")
                                                                             "hash"
                                                                             hash))]
    (if (nil? i)
      (throw ae/identity-not-found)
      i)))
