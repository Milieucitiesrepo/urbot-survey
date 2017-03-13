(ns urbot-survey.events
  (:require [urbot-survey.db :as db]
            [re-frame.core :refer [reg-event-db reg-event-fx]]

            [alandipert.storage-atom :refer [local-storage]]))

(def ^:private completed-surveys
  (local-storage (atom #{}) :completed-surveys))

(reg-event-db
 :initialize-db
 (fn  [_ _]
   (assoc db/default-db :completed-surveys @completed-surveys)))

(reg-event-db
 :surveys
 (fn [db [_ survey-id survey]]
   (assoc-in db [:surveys survey-id] survey)))

(reg-event-db
 :show-typeform
 (fn [db [_ show?]]
   (assoc db :show-typeform show?)))

(reg-event-db
 :window-did-resize
 (fn [db [_ window-size]]
   (assoc db :window-size window-size)))

(reg-event-db
 :completed-survey
 (fn [db [_ id]]
   (swap! completed-surveys conj id)
   (update-in db [:completed-surveys] conj id)))
