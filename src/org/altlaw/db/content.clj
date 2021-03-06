(ns org.altlaw.db.content
  (:require [org.altlaw.util.s3 :as s3]
            [org.altlaw.util.context :as context])
  (:import (org.apache.commons.codec.digest DigestUtils)
           (org.apache.commons.io FileUtils)
           (java.io File)))

(def #^{:private true} *bucket* "content.altlaw.org")

(defn put-page-string [path string mime-type]
  (s3/put-object-gzip-string *bucket* (str "www/" path) string {}))

(defn put-page-bytes [path bytes mime-type]
  (s3/put-object-gzip-bytes *bucket* (str "www/" path) bytes {}))

(defn get-page [path]
  (s3/get-object-string *bucket* (str "www/" path)))


;;; Content objects, identified by SHA1 hash

(defn put-content-string [content metadata]
  (let [hash (DigestUtils/shaHex content)
        key (str "sha1/" hash)]
    (when-not (try (s3/get-object-meta *bucket* key)
                   (catch Exception e nil))
      (s3/put-object-gzip-string
       *bucket* key content metadata))
    hash))

(defn put-content-bytes [content metadata]
  (let [hash (DigestUtils/shaHex content)
        key (str "sha1/" hash)]
    (when-not (try (s3/get-object-meta *bucket* key)
                   (catch Exception e nil))
      (s3/put-object-gzip-bytes
       *bucket* key content metadata))
    hash))

(defn- get-content-string-from-file [key]
  (let [path (File. (context/content-storage-dir) key)]
    (when (.exists path)
      (FileUtils/readFileToString path "UTF-8"))))

(defn get-content-string [hash]
  (let [key (str "sha1/" hash)]
    (or (get-content-string-from-file key)
        (s3/get-object-string *bucket* key))))

(defn get-content-bytes [hash]
  (let [key (str "sha1/" hash)]
    (s3/get-object-bytes *bucket* key)))

(defn delete-content [hash]
  (let [key (str "sha1/" hash)]
    (s3/delete-object *bucket* key)))
