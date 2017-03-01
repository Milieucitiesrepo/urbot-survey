(ns urbot-survey.core
    (:require [urbot-survey.setup :refer [setup-env]]
              [urbot-survey.events]
              [urbot-survey.subs]
              [urbot-survey.views :as views]
              [reagent.core :as reagent]
              [re-frame.core :as re-frame]))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel] (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (setup-env)
  (mount-root))
