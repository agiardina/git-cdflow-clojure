(ns cdflow.controller
  (:require [clojure.java.io :as io]
            [cdflow.utils :refer [log]]
            [cdflow.gui :as gui]
            [cdflow.git :as git]
            [cdflow.state :as state]
            [clojure.pprint :as pp]
            [clojure.core.async :as async])

  (:import  [javafx.fxml FXMLLoader]
            [javafx.event ActionEvent EventHandler]
            [javafx.scene.input MouseEvent]
            [javafx.stage Stage Popup DirectoryChooser FileChooser StageStyle Window Modality]
            [javafx.application Platform]
            [javafx.scene Scene]
            [javafx.scene.input KeyEvent KeyCode]
            [javafx.scene.control TreeView TreeItem Button Alert]
            [javafx.scene.control.cell PropertyValueFactory]
            [javafx.collections ObservableList]
            [javafx.collections FXCollections]
            [javafx.scene.control TableColumn]
            [javafx.scene.control TableRow]
            [javafx.util Callback]
            [javafx.beans.property SimpleStringProperty]
            [java.awt Desktop]
            [java.util Date]
            [java.text SimpleDateFormat]
            [org.eclipse.jgit.api.MergeResult$MergeStatus]
            [javafx.scene.control.Alert$AlertType]
           )

  (:gen-class
    :methods
    [[onLoad [javafx.event.ActionEvent] void]
     [onOpen [javafx.event.ActionEvent] void]
     [onPostReleaseSettingsClick [javafx.event.ActionEvent] void]
     [onSelectCommit [javafx.scene.input.MouseEvent] void]
     [onReleasesMenuClick [javafx.scene.input.MouseEvent] void]
     [onBranchesMenuClick [javafx.scene.input.MouseEvent] void]
     [onParentPullClick [javafx.scene.input.MouseEvent] void]
     [onFetchClick [javafx.scene.input.MouseEvent] void]
     [onNewReleaseClick [javafx.scene.input.MouseEvent] void]
     [onSaveNewReleaseClick [javafx.scene.input.MouseEvent] void]     
     [onCancelCloseWindow [javafx.scene.input.MouseEvent] void]
     [initialize [] void]
     ]))

(def current-stage (atom nil))

(defn show-message [title text alert-type]
  (doto 
    (Alert. alert-type)
    (.setTitle title)
    (.setHeaderText nil)
    (.setContentText text)
    (.showAndWait)))

(defn show-info [title text] 
  (show-message title text javafx.scene.control.Alert$AlertType/INFORMATION))

(defn show-error [title text] 
  (show-message title text javafx.scene.control.Alert$AlertType/ERROR))

(defn show-warning [title text] 
  (show-message title text javafx.scene.control.Alert$AlertType/WARNING))
    

(defn create-item [item parent]
  (let [tree-item (TreeItem. item)]
    (if (nil? parent)
      tree-item
      (.add (.getChildren parent) tree-item))
    tree-item))

(defn create-menu [node parent]
  (let [current-node (create-item (first node) parent)
        children     (rest node)]
    (doall (map #(create-menu % current-node) children))
    current-node))

;@todo manage error for
(defn showBranches [tree repo opt]
  (.setRoot tree (create-menu (git/branch-tree (.getAbsolutePath repo) opt) nil)))

(defn show-commits [scene repo]
  (let [description (.getTableColumn (.lookup scene "#commitstableDescription"))
        commit (.getTableColumn (.lookup scene "#commitstableCommit"))
        date (.getTableColumn (.lookup scene "#commitstableDate"))
        author (.getTableColumn (.lookup scene "#commitstableAuthor"))
        table  (.lookup scene "#commitstable")
        commits (FXCollections/observableArrayList (git/get-all-ref-commits repo))
        date-format (SimpleDateFormat. "MMM d yyyy, HH:mm")]

    (doto table (.setItems commits))

    (.. description (setCellValueFactory (PropertyValueFactory. "fullMessage")))
    (.. commit (setCellValueFactory (PropertyValueFactory. "name")))
    
    (.. author (setCellValueFactory (proxy [Callback] []
        (call [c] (SimpleStringProperty. (.getName (.getAuthorIdent (.getValue c))))))))

    (.. date (setCellValueFactory (proxy [Callback] []
        (call [c] (SimpleStringProperty. (.format date-format (Date. (* 1000 (.longValue (.getCommitTime (.getValue c)))))))))))))

    ; (.. date (setCellValueFactory (PropertyValueFactory. "commitTime")))))

(defn show-releases [listview repo]
  (let [releases     (FXCollections/observableArrayList (git/get-releases-list repo))]
    (.setItems listview releases)))

(defn- enable-toolbar [scene] 
  (let [toolbar (.lookup scene "#mainToolbar")
       items (.getItems toolbar)
       buttons (filter #(instance? Button %) items)]
       (doall (map #(.setDisable % false) buttons))))

(defn -onPostReleaseSettingsClick [this ^ActionEvent event]
  (let [node (FXMLLoader/load (io/resource "post-release.fxml"))
        scene (Scene. node)
        stage (Stage.)]

  (doto stage
    (.setTitle "Update version JSON")
    (.setScene scene)
    (.show))))

(defn -onReleasesMenuClick [this ^MouseEvent event]
  (let [scene (.. event (getSource) (getScene))
        sidePanel (.lookup scene "#sidePanel")
        children (.getChildren sidePanel)]

    (.setMaxSize (.get children 0) 0 0 )
    (.setMaxSize (.get children 1) 1000 1000 )))

(defn -onBranchesMenuClick [this ^MouseEvent event]
  (let [scene (.. event (getSource) (getScene))
        sidePanel (.lookup scene "#sidePanel")
        children (.getChildren sidePanel)]

    (.setMaxSize (.get children 1) 0 0 )
    (.setMaxSize (.get children 0) 1000 1000 )))

(defn -onSelectCommit [this ^MouseEvent event]
  (let [tv (.getSource event)
        scene (.getScene tv)
        commit (.getName (.getSelectedItem (.getSelectionModel tv)))
        webview (.lookup scene "#webview")
        engine  (.getEngine webview)]
    (log (str "Select commit " (subs commit 0 7) "..."))
    (.executeScript engine (str "showCommitInBranches('" commit  "');"))))

(defn -onFetchClick [this ^MouseEvent event]
  (git/git-fetch-and-merge-notes! (state/get-repository)))

(defn -onParentPullClick [this ^MouseEvent event]
  (let [merge-status (git/parent-pull! (state/get-repository))
        sucessfull? (.isSuccessful merge-status)]
    (cond
      sucessfull?
        (show-info "Parent Pull" "Parent merged with success")
      (= merge-status org.eclipse.jgit.api.MergeResult$MergeStatus/CONFLICTING)
        (show-warning "Parent Pull" "Merge conflict")
      :else 
        (show-error "Parent Pull" "Parent pull failed"))))  

(defn -onNewReleaseClick [this ^MouseEvent event]
  (let [node (FXMLLoader/load (io/resource "parent-window.fxml"))
        scene (Scene. node)
        stage (Stage.)
        parent-branch (.lookup scene "#parentBranch")]

  (.setItems parent-branch  (FXCollections/observableArrayList 
                              (git/remote-branch-list
                                (state/get-repository))))        

  (doto stage
    (.setTitle "New Relase")
    (.setScene scene)
    (.show))))

(defn -onSaveNewReleaseClick [this ^MouseEvent event]
  (let [scene (.. event (getSource) (getScene))
        release (.. scene (lookup "#release") (getText))
        parent-branch (.. scene (lookup "#parentBranch") (getValue))
        release-name (git/release-name release)] 

    (cond 
      (nil? release-name)
        (show-error "New Release" "Invalid release name (eg release/v1.2.0)")
      (nil? parent-branch)
        (show-error "New Release" "Please, select a parent branch")
      :else
        (do (git/release-start! (state/get-repository) parent-branch release-name)
            (show-info "New Release" (str "Branch " release-name " created"))
            (.. scene (getWindow) (close)))
      )))

(defn -onOpen [this ^ActionEvent event]
  (let [chooser (doto (DirectoryChooser.)
                      (.setTitle "Import"))
        repo    (.showDialog chooser (Stage.))
        scene   (->> event
                     .getSource
                     .getParentPopup
                     .getOwnerWindow
                     .getScene)
        webview (.lookup scene "#webview")
        engine  (.getEngine webview)]
    
    (log (str "Opening " (.getAbsolutePath repo)))
    (state/set-repository (.getAbsolutePath repo))
    (showBranches (.lookup scene "#branches") repo :local)
    (showBranches (.lookup scene "#branchesorigin") repo :remote)
    (show-releases (.lookup scene "#releases") repo)
    (show-commits scene repo)
    (enable-toolbar scene)

    (.load engine (.toString (io/resource "tree/index.html")))))

(defn -onCancelCloseWindow[this ^MouseEvent event]
  (.. event (getSource) (getScene) (getWindow) (close)))    

(defn -initialize [el]
  ;Event on local repository
  (state/on-local-repository-change :enable-toolbar 
    (fn [key reference old-state new-state]
      (clojure.pprint/pprint new-state))))