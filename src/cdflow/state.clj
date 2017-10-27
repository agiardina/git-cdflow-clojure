(ns cdflow.state)

(def local-repository (atom nil))

(defn get-repository []
  @local-repository)

(defn set-repository [path]
  (reset! local-repository path))