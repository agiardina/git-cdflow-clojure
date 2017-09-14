(ns cdflow.gui
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :as pprint]
            [fn-fx.fx-dom :as fx-dom]
            [fn-fx.diff :refer [component defui render should-update?]]
            [fn-fx.controls :as ui]
            )
  (:import [javafx.application Application]
           [javafx.fxml FXMLLoader])
  (:gen-class :name cdflow.gui :extends javafx.application.Application))


(def initial-state
  {:options {}
   :root-stage? true
   :data [[]]})

(defonce data-state (atom initial-state))



;; TODO need to move those into a util ns

(defn run-later*
  [f]
  (javafx.application.Platform/runLater f))

(defmacro run-later
  [& body]
  `(run-later* (fn [] ~@body)))

(defn run-now*
  [f]
  (let [result (promise)]
    (run-later
     (deliver result (try (f) (catch Throwable e e))))
    @result))

(defmacro run-now
  [& body]
  `(run-now* (fn [] ~@body)))

(defn force-exit [root-stage?]
  (reify javafx.event.EventHandler
    (handle [this event]
      (when-not root-stage?
        (println "Closing application")
        (javafx.application.Platform/exit)))))

(defui Stage
       (render [this args]
               (ui/stage
                 :title "JavaFX Welcome"
                 :on-close-request (force-exit {:root-stage? @data-state})
                 :shown true
                 :scene (ui/scene
                         :root (run-now (FXMLLoader/load (io/resource "main.fxml")))
                         ))))

(defn handle-event [evt]
  (println  "Event got here")
  (pprint/pprint evt)
)

(defn start
  ([] (start {:root-stage? true}))
  ([{:keys [root-stage?]}]
   (swap! data-state assoc :root-stage? root-stage?)

   (let [handler-fn (fn [event]
                      (println "handler-fn")
                      (println event)
                      (try
                        (swap! data-state handle-event event)
                        (catch Throwable exception
                          (println exception))))
         ui-state (agent (fx-dom/app (stage @data-state)
                                     handler-fn))]

     (add-watch
      data-state :ui
      (fn [_ _ _ _]
        (send ui-state
              (fn [old-ui]
                (println "-- State Updated --")
                (println @data-state)
                (fx-dom/update-app old-ui
                                   (stage @data-state)))))))))
