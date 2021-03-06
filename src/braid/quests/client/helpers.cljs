(ns braid.quests.client.helpers
  (:require
   [braid.core.client.state.helpers :refer [key-by-id key-by]]
   [braid.quests.client.list :refer [quests]]))

; getters

(defn get-active-quest-records [state]
  (->> state
       :braid.quests.client.core/quest-records
       vals
       (filter (fn [quest-record]
                 (= (quest-record :quest-record/state) :active)))
       (sort-by :quest/order)))

(defn get-next-quest [state]
  (let [quest-ids-with-records (->> state
                                    :braid.quests.client.core/quest-records
                                    vals
                                    (map :quest-record/quest-id)
                                    set)
        next-quest (->> quests
                        (sort-by :quest/order)
                        (remove (fn [quest]
                                  (contains? quest-ids-with-records (quest :quest/id))))
                        first)]
    next-quest))

(defn get-quest-record [state quest-record-id]
  (get-in state [:braid.quests.client.core/quest-records quest-record-id]))

; setters

(defn set-quest-records [state quest-records]
  (assoc-in state [:braid.quests.client.core/quest-records] (key-by :quest-record/id quest-records)))

(defn store-quest-record [state quest-record]
  (assoc-in state [:braid.quests.client.core/quest-records (quest-record :quest-record/id)] quest-record))

(defn complete-quest [state quest-record-id]
  (assoc-in state [:braid.quests.client.core/quest-records quest-record-id :quest-record/state] :complete))

(defn skip-quest [state quest-record-id]
  (assoc-in state [:braid.quests.client.core/quest-records quest-record-id :quest-record/state] :skipped))

(defn increment-quest [state quest-record-id]
  (update-in state [:braid.quests.client.core/quest-records quest-record-id :quest-record/progress] inc))
