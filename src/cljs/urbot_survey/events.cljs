(ns urbot-survey.events
  (:require [urbot-survey.db :as db]
            [re-frame.core :refer [reg-event-db reg-event-fx]]))

(reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(reg-event-db
 :surveys
 (fn [db [_ survey-id survey]]
   (assoc-in db [:surveys survey-id] survey)))
