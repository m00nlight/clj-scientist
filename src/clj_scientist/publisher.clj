(ns clj-scientist.publisher
  (require [clojure.tools.logging :as log]
           [clojure.data.json :as json]))

(defn logger-everything
  "Logging every result."
  [result]
  (log/info (json/write-str result)))

(defn logger-only-mismatch
  "Only logging mismatch result"
  [result]
  (if (not (result :match))
    (log/error "[MISMATCH]:"(json/write-str result))))
