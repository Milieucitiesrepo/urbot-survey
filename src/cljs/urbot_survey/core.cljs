(ns urbot-survey.core
  (:require [urbot-survey.setup :refer [setup-env]]
            [urbot-survey.events]
            [urbot-survey.flow :as flow]
            [urbot-survey.subs]
            [urbot-survey.views :as views]
            [urbot-survey.config :as config]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [utilis.types.keyword :refer [->keyword]]
            [utilis.map :refer [map-keys]]))

(def debug? config/debug?)

(defn- mobile?
  []
  (<= (.-innerWidth js/window) 800))

(defn mount-roots []
  (when-not (mobile?)
    (re-frame/clear-subscription-cache!)

    (flow/init!
     {:on-window-did-resize (fn [size] (re-frame/dispatch [:window-did-resize size]))
      :on-window-did-scroll (fn [] (re-frame/dispatch [:window-did-scroll (flow/window-offset)]))})

    (let [completed-surveys-set @(re-frame/subscribe [:completed-surveys])]
      (doseq [element (array-seq (.getElementsByClassName js/document "urbot-survey"))]
        (let [attrs (->> (array-seq (.-attributes element))
                         (map (fn [attr]
                                [(.-name attr)
                                 (.-value attr)]))
                         (into {})
                         (map-keys ->keyword))
              allow-redo? (-> attrs :data-allow-redo (= "true"))
              attrs (update-in attrs
                               [:data-survey-urls]
                               (fn [survey-urls]
                                 (->> survey-urls
                                      (.parse js/JSON)
                                      js->clj
                                      (remove
                                       (if (or debug? allow-redo?)
                                         #{}
                                         completed-surveys-set)))))]
          (if (not-empty (:data-survey-urls attrs))
            (reagent/render
             [views/main-panel
              {:data attrs}] element)
            (.warn js/console "urbot.survey: No incomplete surveys available.")))))))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (setup-env)
  (mount-roots))
