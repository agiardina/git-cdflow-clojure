(ns cdflow.cli
    (:require [clojure.java.io :as io]
              [cdflow.git :as git])
              
    (:gen-class :name cdflow.cli))

(defn- help [command]
    (println (slurp (io/resource (str "help/" command ".txt")))))

(defn release-list [path]
    (doall (map println (git/get-releases-list path))))
    
(defn run [path options]
    (let [command (first (:arguments options))
          action (second (:arguments options))]

        (cond 
            (= [command action] ["release" "list"]) (release-list path)
            (= action "help") (help command))
        ))