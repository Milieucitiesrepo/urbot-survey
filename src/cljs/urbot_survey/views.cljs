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
(def ^:private mobile-desc-max-chars 140)

;;; Public

(defn main-panel []
  (fn [{:keys [data]}]
    [mui/mui-theme-provider
     {:mui-theme (ui/get-mui-theme (styles/theme))}
     [widget-frame
      {:mobile-description (when-let [desc (not-empty (:data-mobile-description data))]
                             (apply str (take mobile-desc-max-chars desc)))
       :mobile-title (:data-mobile-title data)
       :preview-img-src (:data-preview-img-src data)
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
  (let [window-size (re-frame/subscribe [:window-did-resize])]
    (fn [{:keys [title style on-click]}]
      (let [{:keys [category]} @window-size]
        [:div {:style {:width "100%"
                       :height "100%"
                       :display "flex"}}
         [:div {:style (merge {:width "calc(100% - 8px)"
                               :height "calc(100% - 8px)"
                               :margin "auto"
                               :display "flex"
                               :cursor "pointer"}
                              style)
                :onClick (fn [] (when (fn? on-click) (on-click)))}
          [:div {:style {:text-align "center"
                         :margin "auto"
                         :position "relative"
                         :top -1
                         :font-weight 700}} title]]]))))

(defn- title-container
  []
  (fn [{:keys [title text-color]}]
    [:div {:style {:width "calc(100% - 16px)"
                   :display "flex"
                   :padding 8}}
     [:div {:style {:margin "auto"
                    :text-align "center"
                    :font-size 16
                    :font-weight 700
                    :color (or text-color "#000")
                    :line-height "22px"}}
      title]]))

(defn- description-container
  []
  (fn [{:keys [description text-color]}]
    [:div {:style {:width "calc(100% - 16px)"
                   :display "flex"
                   :padding 8}}
     [:div {:style {:margin "auto"
                    :text-align "left"
                    :color (or text-color "rgb(100,100,100)")
                    :font-size "12px"
                    :line-height "16px"}}
      description]]))

(defn- action-buttons
  []
  (fn [{:keys [survey-url
              survey-button-label
              justify-content
              read-more-url
              read-more-label
              buffer-width
              button-min-width
              padding
              on-survey
              invert-survey-button?]
       :or {buffer-width 10
            padding 0
            button-min-width 50
            justify-content "center"}}]
    (let [{:keys [primary-color dark-primary-color]} (styles/theme)
          survey-fn (fn []
                      (re-frame/dispatch [:completed-survey survey-url])
                      (re-frame/dispatch [:show-typeform true]))]
      [:div {:style {:display "flex"
                     :justify-content justify-content
                     :padding padding}}

       ;; take survey button
       [mui/flat-button
        {:label survey-button-label
         :label-style {:font-weight 700 :top -1
                       :text-transform "none"
                       :color (if invert-survey-button?
                                "rgb(100,100,100)"
                                "#FFF")}
         :background-color (if invert-survey-button?
                             "#FFF"
                             primary-color)
         :hover-color (if invert-survey-button?
                        "rgb(200,200,200)"
                        dark-primary-color)
         :style {:min-width button-min-width
                 :border-radius 4}
         :onTouchTap (fn []
                       (if (fn? on-survey)
                         (on-survey survey-fn)
                         (survey-fn)))}]

       ;; buffer
       [:div {:style {:width buffer-width :height "100%"}}]

       ;; read more button
       [mui/flat-button
        {:label read-more-label
         :label-style {:color "rgb(100,100,100)"
                       :font-weight 700
                       :top -2
                       :padding-left 4
                       :padding-right 4
                       :text-transform "none"}
         :href read-more-url
         :background-color "#FFFFFF"
         :hover-color "rgb(200,200,200)"
         :target "_none"
         :style {:border-width "1px"
                 :border-radius 4
                 :min-width button-min-width
                 :border-color "rgb(200,200,200)"
                 :border-style "solid"}}]])

    ))

(defn- widget-body
  []
  (fn [{:keys [style preview target description survey-button-label survey-url survey-id]}]
    (let [padding 10
          {:keys [primary-color dark-primary-color]} (styles/theme)]

      [:div {:style (merge {:width (str "calc(100% - " (* 2 padding) "px)")
                            :background "#FFFFFF"
                            :padding padding}
                           style)}

       [:div {:style {:width "100%"}}

        ;; preview image
        (when (not-empty (:img-src preview))
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
             (:img-caption preview)]]])

        ;; description text
        [description-container {:description description}]

        ;; action buttons
        [action-buttons
         {:survey-url survey-url
          :survey-button-label survey-button-label
          :read-more-url (:href target)
          :read-more-label (:label target)}]]])))

(def ^:private survey-height-percentage 0.80)

(defn- state->width
  [state]
  (if @(re-frame/subscribe [:show-typeform]) 350 350))

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
                 :box-shadow "rgba(0,0,0,0.50) 0 4px 4px"}}

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
                              :padding-bottom 4})} (first children)]]])))

(defn- mobile-milieu-frame
  []
  (let [window-size (re-frame/subscribe [:window-did-resize])
        show-typeform (re-frame/subscribe [:show-typeform])
        frame-id (str (gensym))]
    (fn []
      (reagent/create-class
       {:component-did-mount
        (fn [this]
          (let [rect (.getBoundingClientRect (.getElementById js/document frame-id))]
            (reagent/set-state
             this {:open-frame-height (.-height rect)})
            (go (<! (timeout 450))
                (reagent/set-state this {:render? true}))))
        :reagent-render
        (fn [{:keys [survey title description]}]
          (let [{:keys [state]} survey
                this (reagent/current-component)
                tab-width 35
                tab-height 60
                window-size @window-size
                window-width (:width window-size)
                window-height (:height window-size)
                {:keys [open-frame-height render? animation-state]
                 :or {animation-state :idle}} (reagent/state this)
                margin-left (if (and render? open-frame-height)
                              (condp = state
                                :hidden "100%"
                                :minimized (str "calc(100% - " tab-width "px)")
                                :open (if (or @show-typeform
                                              (= animation-state :slide-left))
                                        (- tab-width)
                                        0)
                                "100%")
                              "100%")

                survey-url (:url survey)
                survey-id (:id survey)
                survey-button-label (:survey-button-label survey)
                read-more-url (:href (:target survey))
                read-more-label (:label (:target survey))

                {:keys [primary-color dark-primary-color]} (styles/theme)

                expand? (= state :minimized)
                icon-size tab-width]

            ;; container
            [mui/paper {:id frame-id
                        :style {:position "fixed"
                                :display "flex"
                                :left margin-left
                                :top (if @show-typeform
                                       (if (= state :minimized)
                                         (- window-height
                                            open-frame-height)
                                         0)
                                       (- window-height
                                          open-frame-height))
                                :min-height 100
                                :background "transparent"}
                        :rounded false
                        :zDepth 0}

             ;; tab
             [mui/paper {:style {:height tab-height
                                 :width tab-width
                                 :background primary-color
                                 :cursor "pointer"
                                 :display "flex"}
                         :zDepth 1
                         :rounded false
                         :onTouchTap (fn []
                                       (re-frame/dispatch
                                        [:surveys (:id survey)
                                         (assoc survey :state
                                                (condp = state
                                                  :minimized :open
                                                  :open :minimized
                                                  state))]))}

              [(if expand?
                 ico/navigation-chevron-left
                 ico/navigation-chevron-right)
               {:style {:width icon-size
                        :height icon-size
                        :margin "auto"}
                :color "#FFF"}]

              ]


             ;; content
             [mui/paper {:style (merge {:width (- window-width
                                                  (if (or @show-typeform
                                                          (= animation-state :slide-left))
                                                    0
                                                    tab-width))
                                        :padding-top 4
                                        :padding-bottom 4
                                        :background primary-color
                                        :display "flex"}
                                       (when @show-typeform
                                         {:height window-height}))
                         :zDepth 0
                         :rounded false}

              [mui/paper {:style {:width "calc(100% - 8px)"
                                  :height (when @show-typeform
                                            (- window-height 8))
                                  :margin "auto"
                                  :overflow-y "hidden"
                                  :background "#FFF"}
                          :zDepth 0
                          :rounded false}

               [:div {:style {:width "100%"
                              :height 160
                              :position "relative"
                              :background (if @show-typeform
                                            primary-color
                                            "transparent")}}
                ;; title
                [title-container
                 {:title title
                  :text-color (if @show-typeform
                                "#FFF"
                                "#000")}]

                ;; description
                [:div {:style {:width "100%"
                               :display "flex"}}
                 [:div {:style {:margin-left "auto"
                                :margin-right "auto"
                                :max-width "90%"}}
                  [description-container
                   {:description description
                    :text-color (if @show-typeform
                                  "#FFF"
                                  "#000")}]]]

                ;; action buttons
                [:div {:style {:position "absolute"
                               :bottom 16
                               :width "100%"
                               :display "flex"}}
                 [:div {:style {:margin-left "auto"
                                :margin-right "auto"
                                :max-width "90%"
                                :width "100%"}}
                  [action-buttons
                   {:buffer-width 0
                    :button-min-width 130
                    :padding 0
                    :justify-content "space-around"
                    :survey-url survey-url
                    :survey-button-label (if @show-typeform
                                           "Close"
                                           survey-button-label)
                    :invert-survey-button? @show-typeform
                    :read-more-url read-more-url
                    :read-more-label read-more-label
                    :on-survey (fn [survey-fn]
                                 (if @show-typeform
                                   (re-frame/dispatch
                                    [:surveys survey-id
                                     (assoc survey :state :minimized)])
                                   (do
                                     (go (reagent/set-state this {:animation-state :slide-left})
                                         (<! (timeout 450))
                                         (survey-fn)
                                         (<! (timeout 450))
                                         (reagent/set-state this {:animation-state :idle})))))}]]]]

               (when @show-typeform
                 [:div {:style {:width "100%"
                                :height "calc(100% - 160px)"}}
                  [typeform {:href survey-url
                             :style {:height "100%"}}]])]]]))}))))

(defn- widget-frame
  [{:keys [target-url
           target-label
           survey-urls
           survey-button-label
           minimized-label
           description
           preview-img-src
           preview-img-caption
           appear-after-ms
           mobile-title
           mobile-description]}]
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
                        :survey-button-label survey-button-label
                        :description description
                        :tab-label minimized-label
                        :state :hidden}]

            ;; dispatch initial survey data
            (re-frame/dispatch [:surveys survey-id survey])

            ;; move survey to minimized after 3 seconds
            (go (<! (timeout (or appear-after-ms 3000)))
                (re-frame/dispatch
                 [:surveys survey-id
                  (assoc survey :state :minimized)]))))
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
                mobile? (= (:category window-size) :small)
                header-height 40
                tab-width 50
                tab-height 25
                margin-right (* window-width 0.15)
                margin-bottom 8

                typeform-height (- open-frame-height
                                   tab-height
                                   header-height
                                   margin-bottom)]

            (if (not-empty window-size)
              [:div

               ;; milieu tab
               (if mobile?
                 [mobile-milieu-frame
                  {:survey (assoc survey
                                  :id survey-id
                                  :url survey-url)
                   :title mobile-title
                   :description mobile-description}]
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
                           :right margin-right
                           :top (condp = state
                                  :minimized (- window-height
                                                tab-height
                                                header-height)
                                  :open (if @show-typeform
                                          (- window-height
                                             (+ typeform-height
                                                tab-height
                                                header-height
                                                margin-bottom
                                                4))
                                          (- window-height
                                             open-frame-height
                                             margin-bottom))
                                  :hidden window-height
                                  window-height)}}
                  (condp = state
                    :hidden [:div]

                    (if @show-typeform

                      [typeform {:href survey-url
                                 :style {:height typeform-height}}]

                      [widget-body
                       (merge {:survey? (= state :survey)
                               :survey-url survey-url
                               :survey-id survey-id}
                              survey)]))])]
              [:div])))}))))
