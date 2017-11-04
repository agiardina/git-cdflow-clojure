(ns cdflow.state)

(def local-repository (atom nil))
(def current-commit (atom nil))

(defn get-repository []
  @local-repository)

(defn set-repository [path]
  (reset! local-repository path))

(defn get-commit []
  @current-commit)

(defn set-commit [commit]
  (reset! current-commit commit))