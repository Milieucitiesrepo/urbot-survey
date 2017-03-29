(ns urbot-survey.flow)

(enable-console-print!)

;;; Declarations

(declare

 ;; Size Utilities
 window-size
 window-width
 window-height
 window-category
 window-physical-size
 screen-size
 screen-width
 screen-height
 add-dpi-element!

 ;; Scroll Utilities
 window-offset
 window-offset-x
 window-offset-y

 ;; Private
 category-for-size

 )

;;; Public

(defn init!
  "Bootstrap the environment with global-level settings and handlers"
  [& [{:keys [on-window-did-resize
              on-window-did-scroll]}]]

  ;; Setup initial size and scroll information
  (add-dpi-element!)

  ;; Notify window size changes
  (when (fn? on-window-did-resize)
    (on-window-did-resize (window-size))
    (.addEventListener
     js/window "resize"
     (fn [_]
       (on-window-did-resize (window-size)))))

  ;; Notify window scroll offset changes
  (when (fn? on-window-did-scroll)
    (.addEventListener
     js/window
     "scroll"
     (fn [_] (on-window-did-scroll)))))

(def init init!)

;; Size Utilities

(defn add-dpi-element! []
  ;; Remove old one
  (when-let [dpi-el (.getElementById js/document "dpi")]
    (.removeChild (.-parentNode dpi-el) dpi-el))

  ;; Add DPI element at new size
  (let [dpi-el (.createElement js/document "div")]
    (set! (.-id dpi-el) "dpi")
    (set! (.-style dpi-el) "height: 1in; width: 1in; left: 100%; position: fixed; top: 100%;")
    (.appendChild (.-body js/document) dpi-el)))

(defn dpi
  []
  (let [dpi-el (.getElementById js/document "dpi")
        dpi-x (.-offsetWidth dpi-el)
        dpi-y (.-offsetHeight dpi-el)]
    [(max dpi-x 1) (max dpi-y 1)]))

(defn window-physical-size
  "Return the physical size of the window in inches"
  ([]
   (window-physical-size
    (.-innerWidth js/window)
    (.-innerHeight js/window)))
  ([width height]
   (let [[dpi-x dpi-y] (dpi)]
     {:width (/ width dpi-x)
      :height (/ height dpi-y)})))

(defn window-size
  "Return the resolution of the browser window"
  []
  (let [width (.-innerWidth js/window)
        height (.-innerHeight js/window)]
    {:width width
     :height height
     :category (category-for-size (window-physical-size width height))}))

(defn window-width
  "Return the current width of the browser window"
  []
  (:width (window-size)))

(defn window-height
  "Return the current height of the browser window"
  []
  (:height (window-size)))

(defn window-category
  "Return the category that best fits the window size"
  []
  (:category (window-size)))

(defn screen-size
  "Return the screen's size"
  []
  {:width (.-width js/screen)
   :height (.-height js/screen)})

(defn screen-width
  "Return the width of the device screen"
  []
  (:width (screen-size)))

(defn screen-height
  "Return the height of the device screen"
  []
  (:height (screen-size)))

;; Scroll Utilities

(defn window-offset
  "Calculate and return the current offset of the browser window"
  []
  (let [doc (.-documentElement js/document)]
    {:y (- (or (.-pageYOffset js/window)
               (.-scrollTop doc))
           (or (.-clientTop doc)
               0))
     :x (- (or (.-pageXOffset js/window)
               (.-scrollLeft doc))
           (or (.-clientLeft doc)
               0))}))

(defn window-offset-x
  "Calculate and return the x-coordinate current offset of the browser window"
  []
  (:x (window-offset)))

(defn window-offset-y
  "Calculate and return the y-coordinate current offset of the browser window"
  []
  (:y (window-offset)))

(defn safari?
  []
  (let [user-agent (.-userAgent js/navigator)]
    (boolean (and (re-find #"Safari" user-agent)
                  (not (re-find #"Chrome" user-agent))))))

;;; Private

(defn- mobile?
  []
  (boolean
   (re-find #"Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini"
            (.-userAgent js/navigator))))

(defn- category-for-size
  "Based on the provided width and height, return a category that best describes
  the dimensions."
  [{:keys [width height]}]

  (cond
    (or (mobile?)
        (and (not (safari?))
             (< width 9))) :small
    (< width 15) :medium

    :else :large))

(defn- calc-app-bar-height
  []
  (let [{:keys [height category]} (window-size)]
    (condp = category
      :small (* height (/ 1 12))
      :medium 64
      :large 64
      64)))
