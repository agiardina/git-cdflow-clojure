(ns cdflow.controller
  (:require [clojure.java.io :as io]
            [cdflow.git :as git]
            [cdflow.state :as state]
            [clojure.pprint :as pp]
            )
  (:import [javafx.event ActionEvent]
           [javafx.stage Stage DirectoryChooser FileChooser StageStyle Window Modality]
           [javafx.application Platform]
           [javafx.scene Scene]
           [javafx.scene.input KeyEvent KeyCode]
           [javafx.scene.control TreeView TreeItem]
           [java.awt Desktop])

  (:gen-class
    :methods [[onLoad [javafx.event.ActionEvent] void]
              [onOpen [javafx.event.ActionEvent] void]]
    ))

(def current-stage (atom nil))

(defn create-item [item parent]
  (let [tree-item (TreeItem. item)]
    (if (nil? parent)
      tree-item
      (.add (.getChildren parent) tree-item))
    tree-item))

(defn create-menu [node parent]
  (let [current-node (create-item (first node) parent)
        children (rest node)]
    (doall (map #(create-menu % current-node) children))
    current-node))

;@todo manage error for
(defn showBranches [tree repo opt]
    (.setRoot tree (create-menu (git/branch-tree (.getAbsolutePath repo) opt) nil)))

(defn -onLoad [this ^ActionEvent event]
  (if (not (nil? (state/get-repository)))
    (let [source (.getSource event)
          scene (.getScene source)
          editor (.lookup scene "#editor")
          webview (.lookup scene "#webview")
          menuBar (.lookup scene "#menuBar")
          engine (.getEngine webview)]

      (.set (.useSystemMenuBarProperty menuBar) true)
      (.load engine (.toString (io/resource "tree/index.html")))
      (.setRoot (.lookup scene "#branches") nil))))

(defn -onOpen [this ^ActionEvent event]
  (let [chooser (doto (DirectoryChooser.)
                      (.setTitle "Import"))
        repo (.showDialog chooser (Stage.))
        scene (->> event
                   .getSource
                   .getParentPopup
                   .getOwnerWindow
                   .getScene)
        webview (.lookup scene "#webview")
        engine (.getEngine webview)]
    (state/set-repository (.getAbsolutePath repo))
    (showBranches (.lookup scene "#branches") repo :local)
    (showBranches (.lookup scene "#branchesorigin") repo :remote)

    (.load engine (.toString (io/resource "tree/index.html")))))