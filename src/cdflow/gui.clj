(ns cdflow.gui
  (:require [cdflow.eventbus :as eventbus]
            [clojure.java.io :as io]
            [clojure.pprint]
            [clojure.string :refer [lower-case]])
  (:import [javafx.application Application]
           [javafx.fxml FXMLLoader]
           [javafx.stage Stage]
           [javafx.scene.text Text]
           [javafx.scene Scene])
  (:gen-class :name cdflow.gui :extends javafx.application.Application))


(defonce log-area (atom nil))

(defn -start [^cdflow.gui app ^Stage stage]
  (let [root (FXMLLoader/load (io/resource "main.fxml"))
        scene (Scene. root 1242 768)]

    (reset! log-area (.lookup scene "#log"))

    (doto stage
          (.setTitle "Git CDFlow")
          (.setScene scene)
          (.show))

    (eventbus/publish :gui-ready scene)

;    (cond (str/starts-with? (str (System/getProperty "os.name")) "Mac")
;      (.set (.useSystemMenuBarProperty (.lookup scene "#menuBar") true)))
    ))

(defn log 
  ([message] (log message [])) 
  ([message options]
    (if-not (nil? @log-area)
      (.appendText @log-area (str message "\n")))))
  

