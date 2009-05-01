(ns org.altlaw.util.crawler
  (:require [org.altlaw.db.download-log :as dl]
            [clojure.contrib.singleton :as sing]
            [clojure.contrib.java-utils :as j])
  (:import (org.restlet Client Context)
           (org.restlet.data Request Protocol Method Form)
           (org.apache.commons.codec.binary Base64)
           (org.apache.commons.io IOUtils)))

(defn- crawler-client-context []
  ;; Automatically follow HTTP redirects
  (doto (Context.)
    (.. getParameters (add "followRedirects" "true"))))

(def #^{:private true} crawler-http-client
     ;; Client is thread-safe.
     (sing/global-singleton
      (fn []
        (doto (Client. Protocol/HTTP)
          (.setContext (crawler-client-context))))))

(defn- make-www-form
  "Creates a POST form from a keyword=>value map.
  Returns a WWW-Form-Encoded Representation object."
  [fields]
  (assert (map? fields))
  (let [form (Form.)]
    (doseq [[key value] fields]
      (.add form (j/as-str key) (j/as-str value)))
    (.getWebRepresentation form)))

(defn- set-client-agent [request]
  (.. request getClientInfo
      (setAgent "AltLaw.org crawler <feedback@altlaw.org>")))

(defn- make-request
  "Creates a GET or POST Request object."
  ([url]
     (assert (string? url))
     (doto (Request. Method/GET url)
       (set-client-agent)))
  ([url form-fields]
     (assert (string? url))
     (doto (Request. Method/POST url)
       (set-client-agent)
       (.setEntity (make-www-form form-fields)))))

(defn- request-headers-vec [request]
  [["Host" (str (.. request getResourceRef getHostDomain))]
   ["Agent" (.. request getClientInfo getAgent)]])

(defn- basic-request-map [request]
  {:request_http_version "1.1"
   :request_uri (str (.. request getResourceRef))
   :request_method (.. request getMethod getName)
   :request_headers (request-headers-vec request)})

(defn- extended-request-map [request]
  (when (= Method/POST (.getMethod request))
    {:request_form_fields
     (into {} (.getValuesMap (.getEntityAsForm request)))}))

(defn- request-map
  "Returns a keyword=>value map for the request part of a download
  record."
  [request]
  (merge (basic-request-map request)
         (extended-request-map request)))

(defn- encode-response-body
  "Returns the response entity as a Base64-encoded string, or nil if
  no response entity."
  [response]
  (when (.isAvailable (.. response getEntity))
    (String. (Base64/encodeBase64Chunked
              (IOUtils/toByteArray
               (.. response getEntity getStream)))
             "US-ASCII")))

(defn- headers-vec [message]
  (when (.. message getAttributes (get "org.restlet.http.headers"))
    (vec (map (fn [[k v]] [k v])
              (.. message getAttributes
                  (get "org.restlet.http.headers") getValuesMap)))))

(defn- basic-response-map [response]
  {:response_status_code (.. response getStatus getCode)
   :response_headers (headers-vec response)})

(defn- extended-response-map [response]
  (when-let [body (encode-response-body response)]
    {:response_body_base64 body}))

(defn- response-map
  "Returns a keyword=>value map for the response part of a download
  record."
  [response]
  (merge (basic-response-map response)
         (extended-response-map response)))

(defn- execute-request
  "Calls the client to handle the request, logs the URL in the
  download log, and returns the download record as a keyword=>value
  map."
  [request]
  (let [response (.handle (crawler-http-client) request)
        result (merge (request-map request)
                      (response-map response))]
    (dl/log-download (:request_uri result))
    result))

(defn crawl
  "Given a URL, downloads it and returns a download record as a
  keyword=>value map.  form-fields is a key=>value map of POST form
  parameters.  Logs the URL in the download log, but does not save the
  download log."
  ([url]
     (execute-request (make-request url)))
  ([url form-fields]
     (execute-request (make-request url form-fields))))

(defn handle-download-request
  "Given a partial download record as a keyword=>value map, runs
  crawl."
  [request]
  (if (:request_form_fields request)
    (crawl (:request_uri request) (:request_form_fields request))
    (crawl (:request_uri request))))