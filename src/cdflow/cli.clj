(ns cdflow.cli
    (:require [clojure.java.io :as io]
              [cdflow.git :as git]
              [pretty.cli.prompt :as prompt])

    (:gen-class :name cdflow.cli))

(defn- exit-with-error [msg]
    (println msg)
    ; (System/exit 1)
    )

(defn help [command]
    (println (slurp (io/resource (str "help/" command ".txt")))))

(defn release-list [path]
    (try
        (git/git-fetch-notes! path)
        (doall (map println (git/get-releases-list path)))
        (catch Exception e (exit-with-error (.getMessage e)))))

(defn release-checkout [path version]
    (cond
        (nil? version) (exit-with-error "Error: Missing Version to checkout\nUsage: git cdflow release checkout <VERSION>")
        :else (try
                (git/release-checkout! path version)
                (catch Exception e (exit-with-error (.getMessage e))))))

(defn release-start [path release-name from-release]
    (cond
        (nil? release-name) (exit-with-error "Error: Missing Arguments\nUsage: git cdflow release start <RELEASE_NAME> <FROM_RELEASE>")
        (nil? from-release) (exit-with-error "Error: Missing Arguments\nUsage: git cdflow release start <RELEASE_NAME> <FROM_RELEASE>")
        :else (try
                (git/release-start! path from-release release-name)
                (catch Exception e (exit-with-error (.getMessage e))))))

(defn parent-show [path]
    (try
        (let [parent (git/get-parent path)]
            (if (nil? parent)
                (exit-with-error "Error: Parent has not been set.\nUsage: git cdflow parent set <PARENT_BRANCH>")
                (println parent)))
        (catch Exception e (exit-with-error (.getMessage e)))))

(defn parent-pull [path]
    (try
        (git/parent-pull! path)
        (catch Exception e (exit-with-error (.getMessage e)))))

(defn parent-set [path branch]
    (cond
        (nil? branch) (exit-with-error "Error: Missing parent branch\nUsage: git cdflow parent set <PARENT_BRANCH>")
        :else (try
                (git/parent-set! path branch)
                (git/git-push-notes! path "cdflow")
                (catch Exception e (exit-with-error (.getMessage e))))))

(defn feature-list [path]
    (try
        (git/git-fetch-notes! path)
        (doall (map println (git/get-features-list path)))
        (catch Exception e (exit-with-error (.getMessage e)))))

(defn feature-checkout [path branch]
    (cond
        (nil? branch) (exit-with-error "Error: Missing feature branch\nUsage: git cdflow feature checkout <FEATURE_BRANCH>")
        :else (try
                (git/feature-checkout! path branch)
                (catch Exception e (exit-with-error (.getMessage e))))))

(defn feature-start [path name]
    (cond
        (nil? name) (exit-with-error "Error: Missing feature name\nUsage: git cdflow feature start <FEATURE_NAME>")
        :else (try
                (git/feature-start! path name)
                (catch Exception e (exit-with-error (.getMessage e))))))

(defn feature-finish [path]
    (try
        (git/feature-finish! path)
        (catch Exception e (exit-with-error (.getMessage e)))))

(defn run [path options]
    (let [command (first (:arguments options))
          action (second (:arguments options))
          args (drop 2 (:arguments options))]
        (cond
            ;RELEASE commands
            (= [command action] ["release" "list"]) (release-list path)
            (= [command action] ["release" "checkout"]) (release-checkout path (first args))
            (= [command action] ["release" "start"]) (release-start path (first args) (second args))
            ;PARENT commands
            (= [command action] ["parent" "show"]) (parent-show path)
            (= [command action] ["parent" "pull"]) (parent-pull path)
            (= [command action] ["parent" "set"]) (parent-set path (first args))
            ;FEATURE commands
            (= [command action] ["feature" "list"]) (feature-list path)
            (= [command action] ["feature" "checkout"]) (feature-checkout path first args)
            (= [command action] ["feature" "start"]) (feature-start path (first args))
            (= [command action] ["feature" "finish"]) (feature-finish path)
            ;HELP
            (= action "help") (help command))))