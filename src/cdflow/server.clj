(ns cdflow.server
  (:require [cdflow.state :as state]
            [cdflow.git :as git])
  (:use [org.httpkit.server :only [run-server]]))

(defn handler [request]

  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "Hello World"})

(defn start []
  (run-server #'handler {:port 3000}))
