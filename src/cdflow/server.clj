(ns cdflow.server
  (:use [org.httpkit.server :only [run-server]]))

(defn handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "Hello World"})

(defn start []
  (run-server #'handler {:port 3000}))
