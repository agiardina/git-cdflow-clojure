(ns cdflow.gui
  (:require [clojure.java.io :as io]
            [clojure.pprint]
            [clojure.string :refer [lower-case]])
  (:import [javafx.application Application]
           [javafx.fxml FXMLLoader]
           [javafx.stage Stage StageBuilder]
           [javafx.scene.text Text]
           [javafx.scene Scene])
  (:gen-class :name cdflow.gui :extends javafx.application.Application))

(defn -start [^cdflow.gui app ^Stage stage]
  (let [root (FXMLLoader/load (io/resource "main.fxml"))
        scene (Scene. root 1242 768)]

    (def log-area (.lookup scene "#log"))

    (doto stage
          (.setTitle "Git CDFlow")
          (.setScene scene)
          (.show))

;    (cond (str/starts-with? (str (System/getProperty "os.name")) "Mac")
;      (.set (.useSystemMenuBarProperty (.lookup scene "#menuBar") true)))
    ))

(defn log 
  ([message] (log message [])) 
  ([message options]
    (if (resolve 'log-area)
      (let [text (Text. (str message "\n"))
            style (.getStyleClass text)
            cls (map #(->> % name lower-case (str "message-")) options)] ; :ERROR becomes message-error
        
        (if (not-empty cls) (doall (map #(.add style %) cls)))

        (.. log-area (getChildren) (add text))))))
  

