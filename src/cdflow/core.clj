(ns cdflow.core
  (:require [clojure.java.io :as io]
            [cdflow.gui :as gui]
            [cdflow.server :as server])
  (:import [javafx.application Application])

  (:gen-class :name cdflow.core))

(defonce server (atom nil))

(defn start-server []
  (reset! server (server/start)))

(defn stop-server []
   (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil))
  )

(defn -main [& args]
  (start-server)
  (gui/start)
)

(defn restart [& args]
  (stop-server)
  (apply -main args)
  )
