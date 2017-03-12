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

(defn- widget-tab
  []
  (fn [{:keys [style]}]
    (let [{:keys [primary-color]} (styles/theme)]
      [:div {:style (merge {:background primary-color
                            :width 80 :height 35}
                           style)}])))

(defn- widget-header
  []
  (fn [{:keys [title style]}]
    [:div {:style (merge {:width "calc(100% - 16px)"
                          :height "calc(100% - 16px)"
                          :padding 8
                          :display "flex"} style)}
     [:div {:style {:text-align "center"
                    :margin "auto"
                    :position "relative"
                    :top -1
                    :font-weight 700}} title]]))

#_{:target {:href "https://milieu.io/en/wakefield"
            :label "READ MORE"}
   :description "The Lorne Shouldice Spring ( Wakefield Spring) is a treasured source of potable freshwater. Do you have any concerns about the Spring and its infrastructure that you would like to see addressed?"
   :survey-button-label "Yes"}

(defn- widget-body
  []
  (fn [{:keys [style survey? preview target description survey-button-label]}]
    (let [padding 10
          {:keys [primary-color dark-primary-color]} (styles/theme)]
      [:div {:style (merge {:width (str "calc(100% - " (* 2 padding) "px)")
                            :background "#FFFFFF"
                            :padding padding} style)}

       (if survey?

         ;; show typeform survey in the widget body
         [:div {:style {:width "100%"
                        :height 600
                        :background "red"}}]


         ;; show the preview data in the widget body
         [:div {:style {:width "100%"}}

          ;; preview image
          [:div {:style {:display "inline-block"
                         :position "relative"}}
           [:img {:src (:img-src preview)
                  :width "100%"
                  :height "auto"}]
           [:div {:style {:position "absolute"
                          :top 0 :left 0
                          :width "calc(100% - 50px)"
                          :height "calc(100% - 54px)"
                          :background "rgba(0,0,0,0.40)"
                          :display "flex"
                          :padding 25}}
            [:div {:style {:margin "auto"
                           :text-align "center"
                           :font-size 16
                           :font-weight 700
                           :line-height "22px"}}
             (:img-caption preview)]]]

          ;; description text
          [:div {:style {:width "calc(100% - 16px)"
                         :display "flex"
                         :padding 8}}
           [:div {:style {:margin "auto"
                          :text-align "left"
                          :color "rgb(100,100,100)"
                          :font-size "12px"
                          :line-height "16px"}}
            description]]

          ;; action buttons
          [:div {:style {:display "flex"
                         :justify-content "center"}}

           ;; take survey button
           [mui/flat-button
            {:label survey-button-label
             :label-style {:font-weight 700 :top -1
                           :text-transform "none"}
             :background-color primary-color
             :hover-color dark-primary-color
             :style {:min-width 50
                     :border-radius 4}}]

           ;; buffer
           [:div {:style {:width 10 :height "100%"}}]

           ;; read more button
           [mui/flat-button
            {:label (:label target)
             :label-style {:color "rgb(100,100,100)"
                           :font-weight 700
                           :top -2
                           :padding-left 4
                           :padding-right 4
                           :text-transform "none"}
             :href (:href target)
             :background-color "#FFFFFF"
             :hover-color "rgb(200,200,200)"
             :target "_none"
             :style {:border-width "1px"
                     :border-radius 4
                     :border-color "rgb(200,200,200)"
                     :border-style "solid"}}]

           ]

          ]

         )

       ])))

(defn- state->width
  [state]
  (condp = state
    :hidden 300
    :minimized 300
    :open 300
    :survey (fn []

              ::calculate-height-here

              800

              )
    0))

(defn- state->height
  [state]
  (condp = state
    :hidden 0
    :minimized 100
    :open 500
    :survey (fn []

              ::calculate-width-here

              800

              )
    0))

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
            (go (<! (timeout 3 #_3000))
                (re-frame/dispatch
                 [:surveys survey-id
                  (assoc survey :state :open)])))

          )
        :reagent-render
        (fn []
          (let [{:keys [state target-label] :or {state :hidden} :as survey} @survey
                {:keys [primary-color text-icons-color font-family font-weight]} (styles/theme)
                height (state->height state)
                width (state->width state)
                tab-height 25
                tab-width 60]


            [mui/paper {:style {:position "fixed"
                                :right 8 :bottom 8
                                :width width
                                :max-height height
                                :background "transparent"
                                :color text-icons-color
                                :font-family font-family
                                :font-weight font-weight}
                        :zDepth 0}

             ;; tabs
             [:div {:style {:margin "0"
                            :height tab-height}}

              ;; tab
              [:div {:style {:margin 0
                             :padding 0
                             :height tab-height
                             :position "relative"
                             :top 4
                             :z-index 3}}
               [widget-tab
                {:style {:width tab-width
                         :height tab-height
                         :float "right"
                         :-webkit-border-radius "8px 8px 0 0"
                         :-moz-border-radius "8px 8px 0 0"
                         :border-radius "8px 8px 0 0"
                         :border-bottom 0
                         :color "#000"
                         :-webkit-box-shadow "rgba(0,0,0,0.50) 0 0px 4px"
                         :-moz-box-shadow "rgba(0,0,0,0.50) 0 0px 4px"
                         :box-shadow "rgba(0,0,0,0.50) 0 0px 4px"}}]]]

             ;; content
             [:div
              {:style {:max-height (- height tab-height)
                       :width width
                       :background primary-color

                       :display "flex"
                       :flex-direction "column"

                       :position "relative"
                       :z-index 4


                       :clear "left"

                       :-webkit-box-shadow "rgba(0,0,0,0.50) 0 4px 4px"
                       :-moz-box-shadow "rgba(0,0,0,0.50) 0 4px 4px"
                       :box-shadow "rgba(0,0,0,0.50) 0 4px 4px"

                       }}

              ;; header
              [:div {:style {:height 40}}
               [widget-header
                {:title target-label}]]

              ;; body
              [:div {:style {:max-height "calc(100% - 8px)"
                             :width "calc(100% - 8px)"
                             :overflow-y "scroll"
                             :background primary-color
                             :padding-left 4
                             :padding-right 4
                             :padding-bottom 4}}
               [widget-body
                {:survey? (= state :survey)
                 :preview {:img-src "https://d4z6dx8qrln4r.cloudfront.net/image-f2014a7980f61d8471013003cfbeb78e-default.jpeg"
                           :img-caption "Wakefield Spring Redesign Wakefield, La Pêche, QC"}
                 :target {:href "https://milieu.io/en/wakefield"
                          :label "Read More"}
                 :description "The Lorne Shouldice Spring (Wakefield Spring) is a treasured source of potable freshwater. Do you have any concerns about the Spring and its infrastructure that you would like to see addressed?"
                 :survey-button-label "Take the Survey"}]]]

             ]

            #_[mui/paper {:style {:position "fixed"
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
