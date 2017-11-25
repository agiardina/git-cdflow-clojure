(ns cdflow.eventbus
  (require [clojure.core.async :refer [go-loop <! >!! >! sliding-buffer chan pub sub go]]))

(def publisher (chan))
(def publication (pub publisher #(:topic %)))

(defn set-handler [channel handler]
  (go-loop []
    (handler (<! channel))
    (recur)))

(defn subscribe [topic handler]
  (let [subscriber (chan)]
    (sub publication topic subscriber)
    (set-handler subscriber handler)))

(defn publish [topic data]
  (go (>! publisher {:topic topic :data data })))
