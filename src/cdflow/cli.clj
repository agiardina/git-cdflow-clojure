(ns cdflow.cli
    (:require [clojure.java.io :as io]
              [cdflow.git :as git]
              [pretty.cli.prompt :as prompt])

    (:gen-class :name cdflow.cli))

(defn help [command]
    (println (slurp (io/resource (str "help/" command ".txt")))))

(defn release-list [path]
    (git/git-fetch-notes! path)
    (doall (map println (git/get-releases-list path))))

(defn release-start [path]
    (let [release-list (->> path
                            git/get-releases-list
                            reverse
                            (take 10))
          from (prompt/list-select "Select the release branch you want to branch from" release-list)]

        (clojure.pprint/pprint from)))

(defn run [path options]
    (let [command (first (:arguments options))
          action (second (:arguments options))]

        (cond
            (= [command action] ["release" "list"]) (release-list path)
            (= [command action] ["release" "start"]) (release-start path)
            (= action "help") (help command))
        ))