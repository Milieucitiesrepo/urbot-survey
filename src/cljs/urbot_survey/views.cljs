(ns urbot-survey.views
  (:require [cljs-react-material-ui.core :as ui]
            [cljs-react-material-ui.icons :as ico]
            [cljs-react-material-ui.reagent :as mui]

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
      (let [{:keys [artifacts]} @survey
            {:keys [href background label]} (first artifacts)]
        [mui/paper {:zDepth 0
                    :rounded false
                    :style {:width "100%"
                            :min-height 125
                            :hieght 125
                            :display "flex"
                            :background background}}
         [:a {:style {:margin "auto"
                      :text-align "center"}
              :href href
              :target "_blank"}
          label]]))))

(defn survey-body
  []
  (let [survey (re-frame/subscribe [:survey])]
    (fn []
      (let [{:keys [active-node-id] :as survey} @survey
            {:keys [prompt input]} (get (-> survey :graph :lookup) active-node-id)]
        [mui/paper {:zDepth 0
                    :rounded false
                    :style {:width "100%"
                            :height 175
                            :display "flex"
                            :flex-direction "column"
                            :background "green"}}

         ;; prompt
         [:div {:style {:margin-left "auto"
                        :margin-right "auto"
                        :padding-top 8
                        :padding-bottom 8}}
          prompt]

         ;; input
         [:div {:style {:width "100%"}}
          [inputs/survey-input input]]]))))

(defn- widget-frame
  []
  (fn []
    [mui/paper {:style {:position "fixed"
                        :width 250
                        :height 300
                        :max-height 300
                        :bottom 32
                        :right 32}
                :rounded false
                :zDepth 3}
     [:div {:style {:display "flex"
                    :flex-direction "column"}}]
     [survey-header]
     [survey-body]]))
