(ns urbot-survey.inputs
  (:require [cljs-react-material-ui.core :as ui]
            [cljs-react-material-ui.icons :as ico]
            [cljs-react-material-ui.reagent :as mui]

            [utilis.types.keyword :refer [->keyword]]

            [reagent.core :as reagent]
            [re-frame.core :as re-frame]))

(defmulti survey-input (comp ->keyword :type))

(defmethod survey-input :yesno
  [input]
  (fn []
    [:div {:style {:display "flex"
                   :width "100%"
                   :justify-content "space-around"
                   :margin-left "auto"
                   :margin-right "auto"}}
     [mui/flat-button
      {:label (-> input :data :labels :yes)
       :style {:border-radius 0}}]
     [mui/flat-button
      {:label (-> input :data :labels :no)
       :style {:border-radius 0}}]]))

(defmethod survey-input :default [_] [:div])
