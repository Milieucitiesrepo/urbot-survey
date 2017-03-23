(ns urbot-survey.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 :surveys
 (fn [db [_ survey-id]]
   (get-in db [:surveys survey-id])))

(reg-sub
 :show-typeform
 (fn [db]
   (get db :show-typeform)))

(reg-sub
 :window-did-resize
 (fn [db]
   (:window-size db)))

(reg-sub
 :completed-surveys
 (fn [db]
   (:completed-surveys db)))
