(ns urbot-survey.styles
  (:require [palette.core :as pp :refer [darken lighten transparent]]))

;;; Declarations

(def ^:private theme-atom (atom nil))
(def ^:private milieu-blue "rgb(175,208,201)")

;;; Themes

(def standard-desktop
  (let [primary-color milieu-blue]
    {:primary-color primary-color
     :dark-primary-color (darken primary-color)
     :light-primary-color (lighten primary-color)

     :accent-color "rgb(214, 81, 91)"
     :secondary-accent-color (lighten primary-color 0.25)

     :secondary-text-color (lighten "#000000" 0.50)
     :boosted-color "#FAFAFA"
     :font-family "'Roboto', sans-serif"

     :text-icons-color "#FAFAFA"
     :primary-text-color "#212121"
     :divider-color "#B6B6B6"
     :font-weight 200
     :app-bar {:height 50
               :color "#FAFAFA"
               :text-color "#000000"}
     :flat-button {:color "#92C7C6"
                   :text-color "#FAFAFA"}
     :text-field {:focus-color "#F6AE2D"}
     :snackbar {:text-color "#FAFAFA"
                :background-color "#283845"
                :action-color "#283845"}}))

(def light-theme
  (merge standard-desktop
         {:text-color "#212121"
          :alternate-text-color "#FFF"
          :disabled-color "#D3D3D3"
          :canvas-color "#FFF"
          :chart-colors ["#006064" "#00838F" "#0097A7" "#00ACC1" "#00BCD4"
                         "#26C6DA" "#4DD0E1" "#80DEEA" "#B2EBF2" "#E0F7FA"]
          :primary1Color "#0097A7"
          :accent1Color "#D70075"
          :pickerHeaderColor "#0097A7"
          :disabledColor "#D3D3D3"
          :paper {:color "#000000"
                  :border-radius "0px"
                  :background-color "#FAFAFA"}}))

(def dark-theme
  (merge standard-desktop
         {:text-color "#FFF"
          :alternate-text-color "#303030"
          :disabled-color "#D3D3D3"
          :canvas-color "#303030"
          :chart-colors ["#880E4F" "#AD1457" "#C2185B" "#D81B60" "#E91E63"
                         "#EC407A" "#F06292" "#F48FB1" "#F8BBD0" "#FCE4EC"]
          :primary1Color "#0097A7"
          :accent1Color "#D70075"
          :pickerHeaderColor "#0097A7"
          :disabledColor "#D3D3D3"
          :paper {:color "#FAFAFA"
                  :border-radius "0px"
                  :background-color "rgb(10,10,10)"}}))

(def default-theme light-theme)

;;; Public

(defn set-theme!
  [theme]
  (reset! theme-atom theme))

(defn theme
  []
  (or @theme-atom default-theme))
