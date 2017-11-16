(ns reframe-tools.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [reframe-tools.tools :refer [recordable] :as tools]))

(enable-console-print!)

(rf/reg-event-db
 :init
 [recordable]
 (constantly {:count 0}))

(rf/reg-event-fx
 :increment-count
 [recordable]
 (fn [{:keys [db]}]
   {:db (update db :count inc)
    :print (:count db)}))

(rf/reg-event-db
 :decrement-count
 [recordable]
 (fn [db]
   (if (< (:count db) 0)
     (update db :count dec)
     db)))

(rf/reg-fx
 :print
 (fn [message]
   (println (str "Printing: " message))))

(rf/reg-sub
 :count
 (fn [db]
   (:count db)))

(defn app []
  [:div
   [:h1 "Hello World"]
   [:p (str "Count: " @(rf/subscribe [:count]))]
   [:button {:on-click #(rf/dispatch [:increment-count])} "Inc"]
   [:button {:on-click #(rf/dispatch [:decrement-count])} "Dec"]])

(defn render []
  (r/render [app] (js/document.getElementById "app")))


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  (render))

(defn main []
  (rf/dispatch-sync [:init])
  (render))

(defonce initialised (atom false))
(when-not @initialised
  (println "initialising")
  (main)
  (reset! initialised true))
