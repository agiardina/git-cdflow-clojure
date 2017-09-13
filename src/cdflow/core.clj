(ns cdflow.core
  (:require [clojure.java.io :as io]
            [cdflow.gui]
            [cdflow.server :as server])
  (:import [javafx.application Application])

  (:gen-class :name cdflow.core))

(defonce server (atom nil))

(defn start-server []
  (reset! server (server/start)))

(defn stop-server []
   (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil))
  )

(defn -main [& args]
  (start-server)
  (Application/launch cdflow.gui (into-array String args))
  )
