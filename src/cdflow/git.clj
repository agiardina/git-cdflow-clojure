(ns cdflow.git
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clj-jgit.porcelain :as git]))

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

(def testing-repo "/Users/agiardina/dev/uhc-edq-communication-service")
(branch-tree testing-repo)

(defn create-menu [item]
  (if (seq? item)
    (map create-menu item)
    "a"))

(map create-menu (branch-tree testing-repo))
