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
    [:div {:style {:width "100%" :height "100%"}}
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
  (fn [{:keys [style expand? on-click] :or {expand? true}}]
    (let [this (reagent/current-component)
          {:keys [hover?]} (reagent/state this)
          {:keys [primary-color text-icons-color]} (styles/theme)
          icon-size (if hover? 30 20)]
      [:div {:style (merge {:background primary-color
                            :width 80 :height 35
                            :display "flex"
                            :cursor "pointer"}
                           style)
             :onMouseOver (fn [] (reagent/set-state this {:hover? true}))
             :onMouseOut (fn [] (reagent/set-state this {:hover? false}))
             :on-click (fn [e]
                         (reagent/set-state this {:hover? false})
                         (when (fn? on-click)
                           (on-click e)))}
       [(if expand?
          ico/navigation-expand-less
          ico/navigation-expand-more)
        {:style {:width icon-size
                 :height icon-size
                 :margin "auto"}
         :color text-icons-color}]])))

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

(defn- widget-body
  []
  (fn [{:keys [style preview target description survey-button-label show-survey-fn survey-urls]}]
    (let [padding 10
          {:keys [primary-color dark-primary-color]} (styles/theme)]
      [:div {:style (merge {:width (str "calc(100% - " (* 2 padding) "px)")
                            :background "#FFFFFF"
                            :padding padding}
                           style)}

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
                   :border-radius 4}
           :onTouchTap (fn [] (show-survey-fn))}]

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

       ])))

(def ^:private survey-height-percentage 0.80)

(defn- state->width
  [state]
  (condp = state
    :hidden 300
    :minimized 300
    :open 300
    0))

(defn- state->height
  [state]
  (condp = state
    :hidden 0
    :minimized 500
    :open 500
    0))

(defn- milieu-frame
  []
  (fn []
    (let [this (reagent/current-component)
          {:keys [style id on-tab-click width height tab-label tab-expand? body tab-width tab-height header-height]} (reagent/props this)
          {:keys [text-icons-color font-family font-weight primary-color]} (styles/theme)]
      [mui/paper {:style (merge {:background "transparent"
                                 :color text-icons-color
                                 :font-family font-family
                                 :font-weight font-weight

                                 :width width
                                 :max-height height

                                 :transition "all 450ms cubic-bezier(0.23, 1, 0.32, 1) 0ms"
                                 :transition-property "top, width, height, max-height"}
                                style)
                  :zDepth 0
                  :id id}

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
          {:expand? tab-expand?
           :on-click on-tab-click
           :style {:width tab-width
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
        [:div {:style {:height header-height}}
         [widget-header
          {:title tab-label}]]

        ;; body
        [:div {:style (merge {:overflow-y "scroll"}
                             {:max-height "calc(100% - 8px)"
                              :width "calc(100% - 8px)"
                              :background primary-color
                              :padding-left 4
                              :padding-right 4
                              :padding-bottom 4})} body]]])

    ))

(defn- widget-frame
  "state in #{:hidden :minimized :open :survey}"
  [{:keys [target-url target-label survey-urls]}]
  (let [survey-id (str (gensym))
        frame-id (str (gensym))
        survey (re-frame/subscribe [:surveys survey-id])]
    (fn []
      (reagent/create-class
       {:component-will-mount
        (fn []

          (let [survey {:survey-urls survey-urls
                        :preview {:img-src "https://d4z6dx8qrln4r.cloudfront.net/image-f2014a7980f61d8471013003cfbeb78e-default.jpeg"
                                  :img-caption "Wakefield Spring Redesign Wakefield, La Pêche, QC"}
                        :target {:href "https://milieu.io/en/wakefield"
                                 :label "Read More"}
                        :description "The Lorne Shouldice Spring (Wakefield Spring) is a treasured source of potable freshwater. Do you have any concerns about the Spring and its infrastructure that you would like to see addressed?"
                        :survey-button-label "Take the Survey"
                        :tab-label "New development near you"
                        :state :hidden}]

            ;; dispatch initial survey data
            (re-frame/dispatch [:surveys survey-id survey])

            ;; move survey to minimized after 3 seconds
            (go (<! (timeout 3 #_3000))
                (re-frame/dispatch
                 [:surveys survey-id
                  (assoc survey :state :minimized)])))

          )
        :reagent-render
        (fn []
          (let [this (reagent/current-component)
                {:keys [state survey-urls tab-label] :or {state :hidden} :as survey} @survey
                {:keys [primary-color text-icons-color font-family font-weight]} (styles/theme)
                height (state->height state)
                width (state->width state)
                {:keys [open-frame-height] :or {open-frame-height height}} (reagent/state this)
                window-height (.-innerHeight js/window)
                window-width (.-innerWidth js/window)
                header-height 40
                tab-width 50
                tab-height 25]

            (if (= state :hidden)
              [:div]
              [:div

               [mui/paper {:style {:position "fixed"
                                   :background "red"
                                   :top (/ (* (.-innerHeight js/window) (- 1 survey-height-percentage)) 2)
                                   :left (/ (- (.-innerWidth js/window)
                                               (* (.-innerHeight js/window) survey-height-percentage)) 2)
                                   :width (* (.-innerHeight js/window) survey-height-percentage)
                                   :height (* (.-innerHeight js/window) survey-height-percentage)}}
                [typeform {:href (rand-nth survey-urls)}]]

               [milieu-frame
                {:id frame-id
                 :width width
                 :height height
                 :header-height header-height
                 :tab-width tab-width
                 :tab-height tab-height
                 :tab-label tab-label
                 :tab-expand? (= state :minimized)
                 :on-tab-click (fn []

                                 (when (= state :minimized)
                                   (let [rect (.getBoundingClientRect (.getElementById js/document frame-id))]
                                     (reagent/set-state
                                      this {:open-frame-height (.-height rect)})))

                                 (re-frame/dispatch
                                  [:surveys survey-id
                                   (assoc survey :state
                                          (condp = state
                                            :minimized :open
                                            :open :minimized
                                            state))]))
                 :style {:position "fixed"
                         :right 8
                         :top (condp = state
                                :minimized (- window-height
                                              tab-height
                                              header-height)
                                :open (- window-height open-frame-height 8)
                                :hidden 0
                                0)}

                 :body [widget-body
                        (merge {:survey? (= state :survey)
                                :show-survey-fn (fn [] (re-frame/dispatch
                                                       [:surveys survey-id
                                                        (assoc survey :state :minimized)]))} survey)]}]])))}))))
