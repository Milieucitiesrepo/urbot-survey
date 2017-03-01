(defproject urbot-survey "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.473"]

                 [reagent "0.6.0"]
                 [re-frame "0.9.2"]]
  :plugins [[lein-cljsbuild "1.1.4"]]
  :min-lein-version "2.5.3"
  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :figwheel {:css-dirs ["resources/public/css"]}
  :profiles {:dev {:source-paths ["dev/clj"]
                   :dependencies [[ns-tracker "0.3.1"]
                                  [binaryage/devtools "0.9.1"]
                                  [figwheel-sidecar "0.5.9"]
                                  [com.cemerick/piggieback "0.2.1"]
                                  [ring/ring-devel "1.5.1"]]
                   :plugins [[lein-figwheel "0.5.9"]]}}
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/cljs" "dev/cljs"]
                        :figwheel {:on-jsload "urbot-survey.core/mount-root"}
                        :compiler {:main urbot-survey.core
                                   :output-to "resources/public/js/compiled/app.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :asset-path "js/compiled/out"
                                   :source-map-timestamp true
                                   :preloads [devtools.preload]
                                   :external-config {:devtools/config {:features-to-install :all}}}}
                       {:id "min"
                        :source-paths ["src/cljs" "prod/cljs"]
                        :jar true
                        :compiler {:main urbot-survey.core
                                   :output-to "resources/public/js/compiled/app.js"
                                   :optimizations :advanced
                                   :closure-defines {goog.DEBUG false}
                                   :pretty-print false}}
                       {:id "test"
                        :source-paths ["src/cljs" "dev/cljs" "test/cljs"]
                        :compiler {:main urbot-survey.runner
                                   :output-to "resources/public/js/compiled/test.js"
                                   :output-dir "resources/public/js/compiled/test/out"
                                   :optimizations :none}}]}
  :prep-tasks [["cljsbuild" "once" "min"] "compile"])
