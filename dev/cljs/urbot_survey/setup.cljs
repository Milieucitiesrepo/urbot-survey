(ns urbot-survey.setup
    (:require [urbot-survey.config :as config]))

(defn setup-env []
  (when config/debug?
    (enable-console-print!)
    (println "Running in dev mode")))
