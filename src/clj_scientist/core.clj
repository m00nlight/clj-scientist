(ns clj-scientist.core
  (require [clj-scientist.publisher :as pub]))

(defn new-config
  ([] (new-config {} pub/logger-only-mismatch))
  ([context] (new-config context pub/logger-only-mismatch))
  ([context publisher-fn]
   {:pre [(map? context) (fn? publisher-fn)]}
   {:context context, :publisher publisher-fn}))

;; internal timing evaluation of function
(defn- timing-eval
  [fun-info]
  {:pre [(map? fun-info)
         (not (nil? (fun-info :fun)))
         (not (nil? (fun-info :name)))]}
  (let [{fun-name :name, f :fun} fun-info
        start (. java.lang.System nanoTime)
        throw? (atom false)
        result (try
                 (f)
                 (catch Exception e
                   (do
                     (swap! throw? (fn [_] true))
                     e)))
        end (. java.lang.System nanoTime)]
    (if @throw?
      {:name fun-name, :exception result,
       :result nil, :duration (/ (- end start) 1000000.0)}
      {:name fun-name, :exception false,
       :result result, :duration (/ (- end start) 1000000.0)})))

(defn experiment
  [config control-fn try-fn & tries-fn]
  (let [experiment-fns (conj (map
                               (fn [x y]
                                 {:name (str "experiment-" (format "%02d" x))
                                  :fun y})
                               (range 2 (+ 2 (count tries-fn)))
                               tries-fn)
                             {:name "experiment-01" :fun try-fn})
        ;; shuffle to avoid ordering issue of efficiency
        fns (shuffle (conj experiment-fns
                           {:name "control"
                            :fun control-fn}))
        {publisher :publisher, context :context} config
        eval-metrics (map timing-eval fns)
        match? (and (= 1 (count (set
                                 (map #(-> (% :exception) .getClass .getName)
                                      eval-metrics))))
                    (= 1 (count (set (map #(% :result) eval-metrics)))))
        result {:context context, :match match?
                :execution_order (map #(% :name) eval-metrics)
                :time-stamp (quot (System/currentTimeMillis) 1000)
                :metrics (map #(if (% :exception)
                                 (assoc % :exception
                                        (-> (% :exception) .getClass .getName))
                                 %) eval-metrics)}
        control-result (first (filter #(= "control" (% :name)) eval-metrics))]
    (do
      (publisher result)
      (if (control-result :exception)
        (throw (control-result :exception))
        (control-result :result)))
    ))










