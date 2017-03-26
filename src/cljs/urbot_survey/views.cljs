(ns urbot-survey.views
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-react-material-ui.core :as ui]
            [cljs-react-material-ui.icons :as ico]
            [cljs-react-material-ui.reagent :as mui]

            [cljs.core.async :refer [chan <! >! timeout close!]]

            [urbot-survey.styles :as styles]
            [urbot-survey.inputs :as inputs]

            [utilis.types.number :refer [string->long]]

            [loom.graph :as loom]

            [palette.core :as palette]

            [reagent.core :as reagent]
            [re-frame.core :as re-frame]))

;;; Declarations

(declare widget-frame)

(def ^:private z-index-base 25000)

;;; Public

(defn main-panel []
  (fn [{:keys [data]}]
    [mui/mui-theme-provider
     {:mui-theme (ui/get-mui-theme (styles/theme))}
     [widget-frame
      {:preview-img-src (:data-preview-img-src data)
       :preview-img-caption (:data-preview-img-caption data)
       :target-url (:data-target-url data)
       :target-label (:data-target-label data)
       :survey-urls (:data-survey-urls data)
       :survey-button-label (:data-survey-button-label data)
       :minimized-label (:data-minimized-label data)
       :description (:data-description data)
       :appear-after-ms (let [x (:data-appear-after-ms data)]
                          (when (and (string? x) (not-empty x))
                            (string->long x)))}]]))

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
  [{:keys [href style]}]
  (fn []
    [:div {:style (merge {:width "100%"
                          :height "100%"} style)}
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
  (fn [{:keys [style expand? on-click icon] :or {expand? true}}]
    (let [this (reagent/current-component)
          {:keys [hover?]} (reagent/state this)
          {:keys [primary-color text-icons-color]} (styles/theme)
          icon-size (if hover? 23 18)]
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
       [(or icon
            (if expand?
              ico/navigation-expand-less
              ico/navigation-expand-more))
        {:style {:width icon-size
                 :height icon-size
                 :margin "auto"}
         :color text-icons-color}]])))

(defn- widget-header
  []
  (fn [{:keys [title style on-click]}]
    [:div {:style (merge {:width "calc(100% - 16px)"
                          :height "calc(100% - 16px)"
                          :padding 8
                          :display "flex"
                          :cursor "pointer"} style)
           :onClick (fn [] (when (fn? on-click) (on-click)))}
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
                       :position "relative"
                       :min-height 100}}
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
  (if @(re-frame/subscribe [:show-typeform])
    400 ;; typeform width
    300 ;; normal width

    ))

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
          {:keys [style id on-tab-click width height tab-label tab-expand? body
                  tab-width tab-height header-height tab-icon]} (reagent/props this)
          {:keys [text-icons-color font-family font-weight primary-color]} (styles/theme)
          children (reagent/children this)]
      [mui/paper {:style (merge {:background "transparent"
                                 :color text-icons-color
                                 :font-family font-family
                                 :font-weight font-weight

                                 :width width
                                 :max-height height

                                 :z-index z-index-base

                                 :transition "all 450ms cubic-bezier(0.23, 1, 0.32, 1) 0ms"
                                 :transition-property "top, width, height, max-height, opacity"}
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
           :icon tab-icon
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
          {:on-click on-tab-click
           :title tab-label}]]

        ;; body
        [:div {:style (merge {:overflow-y "scroll"}
                             {:max-height "calc(100% - 8px)"
                              :width "calc(100% - 8px)"
                              :background primary-color
                              :padding-left 4
                              :padding-right 4
                              :padding-bottom 4})} (first children)]]])

    ))

(defn- widget-frame
  "state in #{:hidden :minimized :open :survey}"
  [{:keys [target-url
           target-label
           survey-urls
           survey-button-label
           minimized-label
           description
           preview-img-src
           preview-img-caption
           appear-after-ms]}]
  (let [survey-id (str (gensym))
        frame-id (str (gensym))
        survey (re-frame/subscribe [:surveys survey-id])
        show-typeform (re-frame/subscribe [:show-typeform])
        window-size (re-frame/subscribe [:window-did-resize])

        survey-url (rand-nth survey-urls)]
    (fn []
      (reagent/create-class
       {:component-will-mount
        (fn []

          (let [survey {:survey-urls survey-urls
                        :preview {:img-src preview-img-src
                                  :img-caption preview-img-caption}
                        :target {:href target-url
                                 :label target-label}
                        :description description
                        :survey-button-label survey-button-label
                        :tab-label minimized-label
                        :state :hidden}]

            ;; dispatch initial survey data
            (re-frame/dispatch [:surveys survey-id survey])

            ;; move survey to minimized after 3 seconds
            (go (<! (timeout 0 #_(or appear-after-ms 3000)))
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
                window-size @window-size
                window-height (:height window-size)
                window-width (:width window-size)
                header-height 40
                tab-width 50
                tab-height 25

                typeform-height (- open-frame-height tab-height header-height 8)]

            (if (not-empty window-size)
              [:div

               ;; milieu tab
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
                         :right (* window-width 0.15)
                         :top (condp = state
                                :minimized (- window-height
                                              tab-height
                                              header-height)
                                :open (if @show-typeform
                                        (- window-height
                                           (+ typeform-height
                                              tab-height
                                              header-height 12))
                                        (- window-height
                                           open-frame-height
                                           8))
                                :hidden window-height
                                window-height)}}
                (condp = state
                  :hidden [:div]

                  (if @show-typeform

                    [typeform {:href survey-url
                               :style {:height typeform-height}}]

                    [widget-body
                     (merge {:survey? (= state :survey)
                             :show-survey-fn (fn []
                                               (re-frame/dispatch [:completed-survey survey-url])
                                               (re-frame/dispatch [:show-typeform true]))}
                            survey)]))]]
              [:div])))}))))

;; TODO
;; - detect mobile, and expand to full screen
;; - Full bar on bottom with yes/no big buttons (configurable)
;; - only appear on mobile after scrolling down a bit (configurable)
;; - appear after 3 seconds if no scrolling has happened
