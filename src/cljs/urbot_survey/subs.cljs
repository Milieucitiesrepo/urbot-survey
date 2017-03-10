(ns urbot-survey.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 :surveys
 (fn [db [_ survey-id]]
   (get-in db [:surveys survey-id])))
