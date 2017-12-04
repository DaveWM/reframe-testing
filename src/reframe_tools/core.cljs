(ns reframe-tools.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [re-play.core :refer [recordable] :as rp]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]))

(enable-console-print!)

(rf/reg-event-db
 :init
 [recordable]
 (constantly {:count 0
              :loading false
              :user nil}))

(rf/reg-event-fx
 :increment-count
 [recordable]
 (fn [{:keys [db]}]
   {:db (update db :count inc)
    :print (:count db)}))

(rf/reg-event-fx
 :request-data
 [recordable]
 (fn [{:keys [db]}]
   {:db (assoc db :loading true)
    :http-xhrio {:method :get
                 :uri "https://api.github.com/users/davewm"
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:data-loaded]}}))

(rf/reg-event-db
 :data-loaded
 [recordable]
 (fn [db [_ user]]
   (-> db
       (assoc :user user)
       (assoc :loading false))))

(rf/reg-event-db
 :decrement-count
 [recordable]
 (fn [db]
   (if (> (:count db) 0)
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

(rf/reg-sub
 :loading
 (fn [db]
   (:loading db)))

(rf/reg-sub
 :repos
 (fn [db]
   (get-in db [:user :public_repos])))

(defn app []
  [:div
   [:h1 "Hello World"]
   [:p (str "Count: " @(rf/subscribe [:count]))]
   (if @(rf/subscribe [:loading])
     [:p "Loading!"]
     [:p (str "Repos: " @(rf/subscribe [:repos]))])
   [:button {:on-click #(rf/dispatch [:increment-count])} "Inc"]
   [:button {:on-click #(rf/dispatch [:decrement-count])} "Dec"]
   [:button {:on-click #(rf/dispatch [:request-data])} "Go"]])

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
