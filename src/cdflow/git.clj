(ns cdflow.git
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clj-jgit.porcelain :as git])
  (:import [java.lang String]
           [java.nio.charset StandardCharsets]))

(defn get-branch-name-from-ref [ref]
  (str/replace-first (.getName ref) "refs/heads/" ""))

(defn branch-list
  ([repo-path opt]
    (git/with-repo repo-path
      (->> (git/git-branch-list repo opt)
           (map get-branch-name-from-ref))))
  ([repo-path]
    (branch-list repo-path :local)))

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

(defn- parse-note-string [note]
  (->>  (str/split note #" -> ")
        (map #(str/replace % #"\[|\]" ""))))

(defn- get-parent-from-note [note] (first (parse-note-string note)))

(defn- get-child-from-note [note] (last (parse-note-string note)))

(defn- flat-parents-children [flat-coll]
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

(defn- nest-parents-children
  ([result coll branch]
   (let [new-result (assoc result branch (get coll branch))]
     (->> (map #(nest-parents-children (get-in new-result [branch (key %)]) coll (key %)) (get new-result branch))
          (assoc new-result branch))))
  ([result coll] (nest-parents-children result coll :master)))

(defn- format-parents-children [result tree]
  (reduce-kv (fn [res k v]
    (if (= 0 (count v))
      (assoc res :name (subs (str k) 1) :size 1000)
      (assoc res :name (subs (str k) 1) :children (map #(format-parents-children res %) v)))) result tree))

(defn- get-tree-root [flat-tree]
  (try
    (->> flat-tree
      (reduce-kv (fn [r k v] (assoc r k (reduce-kv (fn [rr kk vv] (if (not (nil? (get vv k))) (+ rr 1) rr)) 0 flat-tree))) {})
      (filter #(= 0 (second %)))
      (first)
      (first))
    (catch Exception e nil)))

(defn- get-commit-id [repo ref]
  (-> repo
      .getRepository
      (.resolve ref)))

(defn- get-git-directory-path [repo]
  (-> repo
      .getRepository
      .getDirectory
      .getAbsolutePath))

(defn- sort-versions-list [v-list]
  (->>  v-list
        (map #(str/replace % #"v" ""))
        (sort #(let [split1 (map (fn [x] (Integer. x)) (str/split %1 #"\."))
                     split2 (map (fn [x] (Integer. x)) (str/split %2 #"\."))]
          (if (= 0 (compare (first split1) (first split2)))
            (if (= 0 (compare (second split1) (second split2)))
              (compare (last split1) (last split2))
              (compare (second split1) (second split2)))
            (compare (first split1) (first split2)))))
        (map #(str "v" %))))

(defn- release-name [name]
  (let [sname (str name)
        vname (if (re-matches #"^[v|V].*" sname) (str/lower-case (str sname)) (str "v" sname))]
    (cond
      (re-matches #"^v[0-9]{1,3}$" vname) (str vname ".0.0")
      (re-matches #"^v[0-9]{1,2}\.[0-9]{1,2}$" vname) (str vname ".0")
      (re-matches #"^v[0-9]{1,2}\.[0-9]{1,2}\.[0-9]{1,2}$" vname) vname
      :else (throw (Exception. "Not a valid release version!")))))

(defn get-all-commits [repo-path]
  (git/with-repo repo-path
    (->> repo
      git/git-log
      (map (fn [x] {:commit (.getName x)
                    :message (.getFullMessage x)
                    :time (.getCommitTime x)
                    :author {:name (.getName (.getAuthorIdent x))
                             :email (.getEmailAddress (.getAuthorIdent x))}})))))

(defn parse-git-notes [repo-path]
  (try
    (git/with-repo repo-path
      (let [repository (.getRepository repo)]
        (->>  (.call (.setNotesRef (.notesList repo) "refs/notes/cdflow"))
              (map #(String. (.getBytes (.open repository (.getData %))) (StandardCharsets/UTF_8)))
              (map #(clojure.string/split % #"\n"))
              (flatten)
              (filter #(re-matches #"\[(.*)->(.*)\]" %)))))
    (catch Exception e [])))

(defn notes->tree [repo-path]
  (try
    (git/with-repo repo-path
      (let [flat-structure (->> repo-path
                                (parse-git-notes)
                                (flat-parents-children))
            tree-root (get-tree-root flat-structure)]
            (as-> (assoc {} tree-root {}) $
                  (nest-parents-children $ flat-structure tree-root)
                  (format-parents-children {} $))))
    (catch Exception e {})))

(defn git-fetch-with-notes!
  ([repo-path remote]
    (git/with-repo repo-path
      (let [git-dir (get-git-directory-path repo)
            current-branch (git/git-branch-current repo)]
        (git/git-fetch-all repo remote)
        (if (not (.isFile (io/file (str git-dir "/refs/notes/cdflow"))))
          (git/git-fetch repo remote "refs/notes/cdflow:refs/notes/cdflow"))
        (git/git-checkout repo "refs/notes/cdflow")
        (git/git-fetch repo remote "refs/notes/cdflow:refs/notes/origin/cdflow")
        (git/git-merge repo (get-commit-id repo "refs/notes/origin/cdflow") :theirs)
        (git/git-checkout repo current-branch))))
  ([repo-path]
    (git-fetch-with-notes repo-path "origin")))

(defn get-releases-list [repo-path]
  (git/with-repo repo-path
    (git-fetch-with-notes repo-path)
    (->>  (branch-list repo-path :all)
          (filter #(re-matches #"(.*)release\/v[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}" %))
          (map #(str/replace % #"release\/" ""))
          (map #(str/replace % #"refs\/remotes\/origin\/" ""))
          set
          (into [])
          sort-versions-list)))

(defn release-checkout! [repo-path version]
  (git/with-repo repo-path
    (git-fetch-with-notes! repo-path)
    (git/git-checkout repo (str "release/" (release-name version)))))

(defn parent-show [repo-path]
  (git/with-repo repo-path
    (-> repo


      )

    )

  )




