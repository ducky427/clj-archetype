(ns archetype.exception
  (:import (javax.ws.rs WebApplicationException)
           (javax.ws.rs.core MediaType Response)))


(defn make-exception
  [code error]
  (WebApplicationException. (Throwable. error) (-> (Response/status code)
                                                   (.entity (str "{\"error\":\"" error "\"}"))
                                                   (.type (MediaType/APPLICATION_JSON))
                                                   (.build))))

(def missing-params (make-exception 400 "Missing Query Parameters."))
(def invalid-email-param  (make-exception 400 "Invalid E-mail Parameter."))
(def invalid-md5-param (make-exception 400 "Invalid MD5Hash Parameter."))
(def identity-not-found (make-exception 404 "Identity not found."))
(def page-not-found (make-exception 404 "Page not found."))
(def identity-not-created (make-exception 500 "Unable to create Identity."))
