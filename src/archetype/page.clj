(ns archetype.page
  (:require [archetype.exception :as ae]
            [clojure.string      :as s])
  (:import (org.apache.commons.validator.routines UrlValidator)
           (java.net URL HttpURLConnection)))

(defn is-valid-url?
  [^String text]
  (.isValid ^UrlValidator (UrlValidator/getInstance) text))


(defn ^URL get-valid-url
  [^String text]
  (try
    (URL. text)
    (catch Throwable t
      ae/invalid-url)))

(defn is-valid-wiki-url?
  [^URL url]
  (= "en.wikipedia.org" (.getHost url)))


(defn is-wiki-url-found?
  [^URL url]
  (let  [^HttpURLConnection huc  (.openConnection url)]
    (.setRequestMethod huc "HEAD")
    (= 200 (.getResponseCode huc))))


(defn get-page-url
  [^String text]
  (if (s/blank? text)
    (throw ae/missing-params)
    (if (is-valid-url? text)
      (let [^URL url (get-valid-url text)]
        (if (is-valid-wiki-url? url)
          (if (is-wiki-url-found? url)
            text
            (throw ae/wiki-url-not-found))
          (throw ae/invalid-wiki-url)))
      (throw ae/invalid-url))))
