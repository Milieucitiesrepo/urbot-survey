(ns urbot-survey.views
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-react-material-ui.core :as ui]
            [cljs-react-material-ui.icons :as ico]
            [cljs-react-material-ui.reagent :as mui]

            [cljs.core.async :refer [chan <! >! timeout close!]]

            [urbot-survey.styles :as styles]
            [urbot-survey.inputs :as inputs]

            [loom.graph :as loom]

            [palette.core :as palette]

            [reagent.core :as reagent]
            [re-frame.core :as re-frame]))

;;; Declarations

(declare widget-frame)

;;; Public

(defn main-panel []
  (fn [{:keys [data]}]
    [mui/mui-theme-provider
     {:mui-theme (ui/get-mui-theme (styles/theme))}
     [widget-frame
      {:target-url (:data-target-url data)
       :target-label (:data-target-label data)
       :survey-urls (js->clj (.parse js/JSON (:data-survey-urls data)))}]]))

;;; Private

(defn- survey-header
  [{:keys [survey-id]}]
  (let [survey (re-frame/subscribe [:surveys survey-id])
        background "#92C7C6"]
    (fn []
      (let [this (reagent/current-component)
            {:keys [hover?]} (reagent/state this)
            {:keys [target-url target-label] :as survey} @survey

            text-color "#FAFAFA"
            hover-text-color (palette/darken text-color)]
        [mui/paper {:zDepth 0
                    :rounded false
                    :style {:width "100%"
                            :min-height 100
                            :height 100
                            :display "flex"
                            :padding 8
                            :background background}
                    :onClick (fn [_] (re-frame/dispatch
                                     [:surveys survey-id
                                      (assoc survey :state :minimized)]))}
         [:a {:className "surveyHeaderLink"
              :onMouseOver (fn [] (reagent/set-state this {:hover? true}))
              :onMouseOut (fn [] (reagent/set-state this {:hover? false}))
              :style {:margin "auto"
                      :text-align "center"
                      :text-decoration "none"
                      :color (if hover? hover-text-color text-color)
                      :transition "all 450ms cubic-bezier(0.23, 1, 0.32, 1) 0ms"}
              :href target-url
              :target "_blank"}
          target-label]]))))

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
  [{:keys [survey-id]}]
  (let [survey (re-frame/subscribe [:surveys survey-id])
        background "#92C7C6"]
    (fn []
      (reagent/create-class
       {:component-did-mount
        (fn [this]
          (go (<! (timeout 450))
              (reagent/set-state this {:render? true})))
        :reagent-render
        (fn []
          (let [this (reagent/current-component)
                {:keys [render?]} (reagent/state this)
                {:keys [survey-urls]} @survey]
            [mui/paper {:zDepth 0
                        :rounded false
                        :style {:width "100%"
                                :height "100%"
                                :min-height 400
                                :max-height 400
                                :display "flex"
                                :flex-direction "column"
                                :background "#FAFAFA"}}
             (if render?
               [typeform {:href (rand-nth survey-urls)}]
               [:div])]))}))))

(defn- widget-frame
  "state in #{:hidden :minimized :open}"
  [{:keys [target-url target-label survey-urls]}]
  (let [survey-id (str (gensym))
        survey (re-frame/subscribe [:surveys survey-id])]
    (fn []
      (reagent/create-class
       {:component-will-mount
        (fn []

          (let [survey {:target-url target-url
                        :target-label target-label
                        :survey-urls survey-urls
                        :state :hidden}]

            ;; dispatch initial survey data
            (re-frame/dispatch [:surveys survey-id survey])

            ;; move survey to minimized after 3 seconds
            (go (<! (timeout 3000))
                (re-frame/dispatch
                 [:surveys survey-id
                  (assoc survey :state :minimized)])))

          )
        :reagent-render
        (fn []
          (let [{:keys [state target-label] :or {state :hidden} :as survey} @survey
                background "#92C7C6"
                height (condp = state
                         :hidden 0
                         :minimized 100
                         :open 500
                         0)
                width (condp = state
                        :hidden 350
                        :minimized 350
                        :open 350
                        0)]
            [mui/paper {:style {:position "fixed"
                                :width width
                                :height height
                                :max-height height
                                :bottom 32
                                :right 32
                                :background (condp = state
                                              :open "#FAFAFA"
                                              :minimized background
                                              "#FAFAFA")}
                        :rounded false
                        :zDepth 3}
             (condp = state

               :open
               [:div {:style {:width "100%"
                              :height "100%"}}
                [survey-header {:survey-id survey-id}]
                [survey-body {:survey-id survey-id}]]

               :minimized
               [mui/flat-button
                {:label (reagent/as-element
                         [:div {:style {:text-align "center"
                                        :width "calc(100% - 32px)"
                                        :height "100%"
                                        :padding-left 16
                                        :margin-bottom 16
                                        :position "relative"
                                        :bottom 18}} target-label])
                 :hoverColor (palette/darken background)
                 :style {:width "100%"
                         :height "100%"
                         :min-width width
                         :max-width width
                         :min-height height
                         :max-height height
                         :margin 0
                         :padding-left 0
                         :padding-right 0
                         :border-radius 0}
                 :onTouchTap (fn [_] (re-frame/dispatch
                                     [:surveys survey-id
                                      (assoc
                                       survey :state
                                       (condp = state
                                         :hidden :minimized
                                         :minimized :open
                                         :open :minimized
                                         :hidden))]))}]

               [:div])]))}))))
