(ns cdflow.server
  (:require [cdflow.state :as state]
            [cdflow.git :as git]
            [clojure.data.json :as json])
  (:use [org.httpkit.server :only [run-server]]))

(defn handler [request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (-> (state/get-repository)
             (git/notes->tree)
             (json/write-str)
             )})

(defn start []
  (run-server #'handler {:port 3000}))
