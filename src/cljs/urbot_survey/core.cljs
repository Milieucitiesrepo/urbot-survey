(ns urbot-survey.core
  (:require [urbot-survey.setup :refer [setup-env]]
            [urbot-survey.events]
            [urbot-survey.subs]
            [urbot-survey.views :as views]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [utilis.types.keyword :refer [->keyword]]
            [utilis.map :refer [map-keys]]))

(defn mount-roots []
  (re-frame/clear-subscription-cache!)
  (doseq [element (array-seq (.getElementsByClassName js/document "urbot-survey"))]
    (reagent/render
     [views/main-panel
      {:data (->> (array-seq (.-attributes element))
                  (map (fn [attr]
                         [(.-name attr)
                          (.-value attr)]))
                  (into {})
                  (map-keys ->keyword))}] element)))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (setup-env)
  (mount-roots))
