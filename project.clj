(defproject git-cdflow-clojure "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-http "3.6.1"]
                 [clj-jgit "0.8.10"]
                 [ring/ring-core "1.5.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [http-kit "2.2.0"]
                 [compojure "1.6.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojars.civa86/pretty.cli "1.0.1"]]
  :plugins [[cider/cider-nrepl "0.15.0"]]
  :main ^:skip-aot cdflow.core
  :aot :all
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
