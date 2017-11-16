(ns reframe-tools.tools
  (:require [re-frame.core :as rf]
            [re-frame.db :refer [app-db]]))

(defonce tape (atom []))
(defonce replaying (atom false))

(defn- current-time []
  (.getTime (js/Date.)))

(defn mark
  ([] {:start-time (current-time)})
  ([time] {:start-time time}))

(defn end-mark
  ([mark] (assoc mark :end-time (current-time)))
  ([mark time] (assoc mark :end-time time)))

(defn db-at-time [time]
  (->> @tape
       (filter #(> time (:time %)))
       last
       :db))

(defn db-at-mark [{:keys [start-time]}]
  (db-at-time start-time))

(defn get-marked-tape [{:keys [start-time end-time]}]
  (->> @tape
       (filter #(<= start-time (:time %) end-time))))

(defn replay!
  ([tape-to-replay]
   (let [[first-tape-entry] tape-to-replay]
     (replay! tape-to-replay (db-at-time (dec (:time first-tape-entry))))))
  
  ([tape-to-replay initial-db]
   (let [events (map :event tape-to-replay)]
     (reset! replaying true)
     (rf/dispatch-sync [::reset initial-db])
     (doseq [event events]
       (rf/dispatch-sync event))
     (reset! replaying false)
     nil)))

(def recordable
  (rf/->interceptor
   :id :record
   :after (fn [context]
            (let [db (get-in context [:effects :db])
                  [event-type :as event] (get-in context [:coeffects :event])]
              (swap! tape #(conj % {:time (current-time)
                                    :db db
                                    :event event}))
              (if @replaying
                (update context :effects #(select-keys % [:db]))
                context)))))

(rf/reg-event-db
 ::reset
 [recordable]
 (fn [db [_ new-db]]
   new-db))
