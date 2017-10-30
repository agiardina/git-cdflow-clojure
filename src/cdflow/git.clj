(ns cdflow.git
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clj-jgit.porcelain :as git])
  (:import [java.lang String]
           [java.nio.charset StandardCharsets]))

(defn get-branch-name-from-ref [ref]
  (str/replace-first (.getName ref) "refs/heads/" ""))

(defn branch-list [repo]
  (map get-branch-name-from-ref (git/git-branch-list (git/load-repo repo))))

(defn ->branch
  [id kids]
  (conj kids id))

(defn ->leaf
  [id]
  (list id))

(defn descendant
  [adj-list node]
  (seq (map second (filter #(= (first %) node) adj-list))))

(defn ->tree
  [adj-list node]
  (let [->tree' (partial ->tree adj-list)]
    (if-let [kid-ids (descendant adj-list node)]
      (->branch node (map ->tree' kid-ids))
      (->leaf node))))

(defn ->adj [vec adj]
  (if (= 2 (count vec))
    (conj adj (lazy-seq vec))
    (->adj (next vec) (conj adj (take 2 vec)))))

(defn branch-tree [repo]
  (let [branches     (map #(str "root/" %) (branch-list repo))
        vec-branches (map #(str/split % #"/") branches)
        adj-branches (map #(->adj % []) vec-branches)
        adj-list     (distinct (apply concat adj-branches))]
    (->tree adj-list "root")))

(defn create-menu [item]
  (if (seq? item)
    (map create-menu item)
    "a"))

(defn parse-git-notes [repo]
  (try
    (let [repository (.getRepository repo)]
      (->>  (.call (.setNotesRef (.notesList repo) "refs/notes/cdflow"))
            (map #(String. (.getBytes (.open repository (.getData %))) (StandardCharsets/UTF_8)))
            (map #(clojure.string/split % #"\n"))
            (flatten)
            (filter #(re-matches #"\[(.*)->(.*)\]" %))))
    (catch Exception e []))


  ["feature/3 -> feature/4" "feature/2 -> release/56" "master -> feature/1" "master -> feature/2" "feature/2 -> feature/3"])



(defn parse-note-string [note]
  (->>  (str/split note #" -> ")
        (map #(str/replace % #"\[|\]" ""))))

(defn get-parent-from-note [note] (first (parse-note-string note)))

(defn get-child-from-note [note] (last (parse-note-string note)))

(defn flat-parents-children [flat-coll]
  (let [flat-parents (->> flat-coll
                          (map #(get-parent-from-note %))
                          (set)
                          (into [])
                          (reduce (fn [res elem] (assoc res (keyword elem) {})) {}))]
    (reduce (fn [res elem]
             (assoc  res
                     (keyword (get-parent-from-note elem))
                     (assoc  (get res (keyword (get-parent-from-note elem)))
                             (keyword (get-child-from-note elem))
                             {}))) flat-parents flat-coll)))

(defn nest-parents-children
  ([result coll branch]
   (let [new-result (assoc result branch (get coll branch))]
     (->> (map #(nest-parents-children (get-in new-result [branch (key %)]) coll (key %)) (get new-result branch))
          (assoc new-result branch))))
  ([result coll] (nest-parents-children result coll :master)))

(defn format-parents-children [result tree]
  (reduce-kv (fn [res k v]
    (if (= 0 (count v))
      (assoc res :name (subs (str k) 1) :children v)
      (assoc res :name (subs (str k) 1) :children (map #(format-parents-children res %) v)))) result tree))

(defn notes->tree [repo-path]
  (git/with-repo repo-path
    (->>  repo
          (parse-git-notes)
          (flat-parents-children)
          (nest-parents-children {:master {}})
          (format-parents-children {}))))





