(ns urbot-survey.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 :survey
 (fn [db]
   (:survey db)))
