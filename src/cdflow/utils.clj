(ns cdflow.utils
    (:require [cdflow.gui :as gui])
    (:gen-class :name cdflow.utils))

(defn log 
    ([message] (gui/log message))
    ([message options] (gui/log message options)))