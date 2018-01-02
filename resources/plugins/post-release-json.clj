(ns cdflow.plugins.jsonrelease
  (:require [cdflow.eventbus :as eventbus])
  (:import [javafx.scene.control Menu MenuItem]))

(defn run-later*
  [f]
  (javafx.application.Platform/runLater f))


(defn starting [ev]
    (let [scene (:data ev)
          settings-menu (.lookup scene "#settingsMenu")
          release-menu (MenuItem. "JSON Release")
          add-menu-item (fn []
            (.. settings-menu (getItems) (add release-menu)))]
    
    (run-later* add-menu-item)
    ; (clojure.pprint/pprint menu-bar)
    ))

(eventbus/subscribe :gui-ready starting)

(clojure.pprint/pprint "[PLUGIN jsonrelease installed]")