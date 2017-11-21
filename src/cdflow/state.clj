(ns cdflow.state)

(def local-repository (atom nil))
(defn get-repository [] @local-repository)
(defn set-repository [path] (reset! local-repository path))

(def current-commit (atom nil))
(defn get-commit [] @current-commit)
(defn set-commit [commit] (reset! current-commit commit))

(defn on-local-repository-change [key fun]
  (add-watch local-repository key fun))