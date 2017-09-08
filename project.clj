(defproject git-cdflow-clojure "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-http "3.6.1"]
                 [clj-jgit "0.8.9"]
                 [ring/ring-core "1.5.0"]
                 [http-kit "2.2.0"]
                 ]
  :plugins [[cider/cider-nrepl "0.15.0"]]  
  :main ^:skip-aot cdflow.core
  :aot :all
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
