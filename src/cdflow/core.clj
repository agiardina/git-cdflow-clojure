(ns cdflow.core
  (:require [clojure.java.io :as io]
            [cdflow.gui]
            [cdflow.cli]
            [cdflow.server :as server]
            [clojure.tools.cli :refer [parse-opts]])
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
    (reset! server nil)))

(def cli-options
  ;; An option with a required argument
  [[nil "--no-gui" "Does not start the gui, useful for command line usage" :id :no-gui]
   [nil "--no-server" "Does not start the server, useful for command line usage" :id :no-server]
   [nil "--cli" "Use the command line interface" :id :cli]])

(defn -main [& args]

  (clojure.pprint/pprint (System/getProperty "user.dir"))

  (let [options (parse-opts args cli-options)]
    (if (nil? (get-in options [:options :no-server]))
      (start-server))

    (if (nil? (get-in options [:options :no-gui]))
      (Application/launch cdflow.gui (into-array String args)))

    (if (get-in options [:options :cli])
      (cdflow.cli/run (System/getProperty "user.dir") options))))