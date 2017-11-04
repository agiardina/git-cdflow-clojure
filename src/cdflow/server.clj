(ns cdflow.server
  (:require [cdflow.state :as state]
            [cdflow.git :as git]
            [clojure.data.json :as json])
  (:use [clojure.string :as string]
        [clojure.walk :only [postwalk]]
        [org.httpkit.server :only [run-server]]
        [compojure.route :only [files not-found]]
        [compojure.handler :only [site]] ; form, query params decode; cookie; session, etc
        [compojure.core :only [defroutes GET POST DELETE ANY context]]))

(defn get-tree [req]
  {:status 200
   :headers {"Content-Type" "application/json"
             "Access-Control-Allow-Origin" "*"}
   :body (-> (state/get-repository)
             (git/notes->tree)
             (json/write-str))})

(defn get-tree-commit [req]
  {:status 200
   :headers {"Content-Type" "application/json"
             "Access-Control-Allow-Origin" "*"}
   :body (let [repo-path (state/get-repository)
               commit (-> req :params :id)
               branches-with-commit
                (git/branch-list-contains repo-path commit)
               tree (git/notes->tree repo-path)
               add-el (fn  [node] (if (and (map? node) (some #(string/includes? % (:name node))branches-with-commit)) (assoc node :active "1") node))
               tree-highlight (postwalk add-el tree)]
           (json/write-str tree-highlight))})



(defroutes all-routes
  (GET "/tree" [] get-tree)
  (GET "/tree/commit/:id" [] get-tree-commit))

(defn start []
  (run-server #'all-routes {:port 3000}))
