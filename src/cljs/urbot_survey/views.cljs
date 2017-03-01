(ns urbot-survey.views
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-react-material-ui.core :as ui]
            [cljs-react-material-ui.icons :as ico]
            [cljs-react-material-ui.reagent :as mui]

            [cljs.core.async :refer [chan <! >! timeout close!]]

            [urbot-survey.styles :as styles]
            [urbot-survey.inputs :as inputs]

            [loom.graph :as loom]

            [reagent.core :as reagent]
            [re-frame.core :as re-frame]))

;;; Declarations

(declare widget-frame)

;;; Public

(defn main-panel []
  (fn []
    [mui/mui-theme-provider
     {:mui-theme (ui/get-mui-theme (styles/theme))}
     [widget-frame]]))

;;; Private

(defn- survey-header
  []
  (let [survey (re-frame/subscribe [:survey])]
    (fn []
      (let [this (reagent/current-component)
            {:keys [hover?]} (reagent/state this)
            {:keys [artifacts]} @survey
            {:keys [href background label]} (first artifacts)

            text-color "#FAFAFA"
            hover-text-color "#AFAFAF"]
        [mui/paper {:zDepth 0
                    :rounded false
                    :style {:width "100%"
                            :min-height 100
                            :height 100
                            :display "flex"
                            :padding 8
                            :background background}}
         [:a {:className "surveyHeaderLink"
              :onMouseOver (fn [] (reagent/set-state this {:hover? true}))
              :onMouseOut (fn [] (reagent/set-state this {:hover? false}))
              :style {:margin "auto"
                      :text-align "center"
                      :text-decoration "none"
                      :color (if hover? hover-text-color text-color)
                      :transition "all 450ms cubic-bezier(0.23, 1, 0.32, 1) 0ms"}
              :href href
              :target "_blank"}
          label]]))))

(defn- typeform
  [{:keys [href]}]
  (fn []
    [:div {:style {:width "100%"
                   :height "100%"}}
     [:iframe {:id "typeform-widget-body"
               :src href
               :display "inline-block"
               :width "100%"
               :height "100%"
               :style {:border-width 0}}]]))

(defn survey-body
  []
  (let [survey (re-frame/subscribe [:survey])]
    (fn []
      (let [{:keys [typeform-embed-url]} @survey]
        [mui/paper {:zDepth 0
                    :rounded false
                    :style {:width "100%"
                            :height "100%"
                            :min-height 400
                            :max-height 400
                            :display "flex"
                            :flex-direction "column"
                            :background "green"}}
         [typeform {:href typeform-embed-url}]]))))

(defn- widget-frame
  []
  (fn []
    (let [height 500]
      [mui/paper {:style {:position "fixed"
                          :width 350
                          :height height
                          :max-height height
                          :bottom 32
                          :right 32}
                  :rounded false
                  :zDepth 3}
       [:div {:style {:display "flex"
                      :flex-direction "column"}}]
       [survey-header]
       [survey-body]])))
