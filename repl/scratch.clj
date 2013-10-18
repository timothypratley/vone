(ns vone.scratch
  (:require [clojure.edn]))


(defn- doc-str [m]
  (str (when-let [ns (:ns m)] (str (ns-name ns) "/")) (:name m) \newline
       (:arglists m) \newline
       (:doc m) \newline))

(doc-str (meta #'inc))

(defn parse
  ([s] (parse s nil))
  ([s t]
   (condp = t
     String s
     Integer (Integer/parseInt s)
     Boolean (Boolean/parseBoolean s)
     Double (Double/parseDouble s)
     (let [r (clojure.edn/read-string s)] (if (symbol? r) s r)))))

(parse "2")
(parse "2" Double)
(parse "true" Boolean)
(parse "this is a string")

(type (clojure.edn/read-string "this is a string"))

(parse "ad1" Integer)

(defn err-parse [s arg]
  (let [t (:tag (meta arg))]
    (try
      [(parse s (resolve t)) nil]
      (catch Exception e
        [nil (str "Failed to parse " (name arg) (when t (str " as " t)))]))))

(err-parse "ad1" (-> #'foo meta :arglists first first))

(defn call [f request]
  (try
    (let [params (request :params)
          request-arity (count (keys params))
          arglists (-> f meta :arglists)
          match #(and (if params
                        (empty %)
                        (every? params (map name %)))
                      (= (count params) (count %)))
          arglist (first (filter match arglists))
          args (map params (map name arglist))
          parsed (map err-parse args arglist)
          _ (println parsed)
          parse-errors (map last parsed)
          parse-vals (map first parsed)
          error (cond
                 (not-any? #(= request-arity (count %)) arglists) "Wrong number of arguments"
                 (nil? arglist) "Parameters do not match"
                 (some identity parse-errors) (clojure.string/join \newline parse-errors))]
      (if error
        (str error \newline (doc-str (meta f)))
        (apply f parse-vals)))))

(call #'foo {:params {"x" "1"}})

(let [f #'inc]
  (:doc (meta f)))

(defn foo [^Integer x] (inc x))

(-> #'foo
    meta
    :arglists
    first
    first
    meta
    :tag)

(-> #'inc meta)

(with-out-str (clojure.repl/doc inc))






