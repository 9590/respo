
(ns respo.core
  (:require [respo.env :refer [element-type]]
            [respo.render.expand :refer [render-app]]
            [respo.controller.resolve :refer [build-deliver-event]]
            [respo.render.diff :refer [find-element-diffs]]
            [respo.util.format :refer [purify-element mute-element]]
            [respo.controller.client :refer [activate-instance! patch-instance!]]
            [respo.util.list :refer [pick-attrs pick-event val-exists?]]
            [respo.util.detect :refer [component?]]
            [respo.util.dom :refer [compare-to-dom!]]
            [respo.schema :as schema])
  (:require-macros [respo.core]))

(defonce *changes-logger (atom nil))

(defonce *dom-element (atom nil))

(defonce *global-element (atom nil))

(defn clear-cache! [] (reset! *dom-element nil))

(defn create-comp [comp-name render]
  (comment println "create component:" comp-name)
  (fn [& args] (merge schema/component {:args args, :name comp-name, :render render})))

(defn create-element [tag-name props & children]
  (assert
   (not (some sequential? children))
   (str "For rendering lists, please use list-> , got: " (pr-str children)))
  (let [attrs (pick-attrs props)
        styles (if (contains? props :style) (sort-by first (:style props)) (list))
        event (pick-event props)
        children (->> (map-indexed vector children) (filter val-exists?))]
    {:name tag-name,
     :coord nil,
     :attrs attrs,
     :style styles,
     :event event,
     :children children,
     :svg? false}))

(defn create-list-element [tag-name props child-map]
  (let [attrs (pick-attrs props)
        styles (if (contains? props :style) (sort-by first (:style props)) (list))
        event (pick-event props)]
    {:name tag-name,
     :coord nil,
     :attrs attrs,
     :style styles,
     :event event,
     :children child-map,
     :svg? false}))

(defn create-svg-element [tag-name props & children]
  (assert
   (not (some sequential? children))
   (str "For rendering lists, please use list-> , got: " (pr-str children)))
  (let [attrs (pick-attrs props)
        styles (if (contains? props :style) (sort-by first (:style props)) (list))
        event (pick-event props)
        children (->> (map-indexed vector children) (filter val-exists?))]
    {:name tag-name,
     :coord nil,
     :attrs attrs,
     :style styles,
     :event event,
     :children children,
     :svg? true}))

(defn create-svg-list [tag-name props child-map]
  (let [attrs (pick-attrs props)
        styles (if (contains? props :style) (sort-by first (:style props)) (list))
        event (pick-event props)]
    {:name tag-name,
     :coord nil,
     :attrs attrs,
     :style styles,
     :event event,
     :children child-map,
     :svg? true}))

(defn render-element [markup] (render-app markup @*dom-element))

(defn mount-app! [target markup dispatch!]
  (assert (instance? element-type target) "1st argument should be an element")
  (assert (component? markup) "2nd argument should be a component")
  (let [element (render-element markup)
        deliver-event (build-deliver-event *global-element dispatch!)]
    (comment println "mount app")
    (activate-instance! (purify-element element) target deliver-event)
    (reset! *global-element element)
    (reset! *dom-element element)))

(defn realize-ssr! [target markup dispatch!]
  (assert (instance? element-type target) "1st argument should be an element")
  (assert (component? markup) "2nd argument should be a component")
  (let [element (render-element markup), app-element (.-firstElementChild target)]
    (if (nil? app-element) (throw (js/Error. "Detected no element from SSR!")))
    (compare-to-dom! (purify-element element) app-element)
    (reset! *global-element (mute-element element))
    (reset! *dom-element element)))

(defn rerender-app! [target markup dispatch!]
  (let [element (render-element markup)
        deliver-event (build-deliver-event *global-element dispatch!)
        *changes (atom [])
        collect! (fn [x]
                   (assert (= 4 (count x)) "change op should has length 4")
                   (swap! *changes conj x))]
    (comment println @*global-element)
    (comment println "Changes:" (pr-str (mapv (partial take 2) @*changes)))
    (find-element-diffs collect! [] @*global-element element)
    (let [logger @*changes-logger]
      (if (some? logger) (logger @*global-element element @*changes)))
    (patch-instance! @*changes target deliver-event)
    (reset! *global-element element)
    (reset! *dom-element element)))

(defn render! [target markup dispatch!]
  (if (some? @*global-element)
    (rerender-app! target markup dispatch!)
    (mount-app! target markup dispatch!)))
