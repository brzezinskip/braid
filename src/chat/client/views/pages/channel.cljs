(ns chat.client.views.pages.channel
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.views.threads :refer [thread-view new-thread-view]]
            [chat.client.views.pills :refer [tag-view subscribe-button]]))

(defn channel-page-view [data owner]
  (reify
    om/IDidMount
    (did-mount [_]
      (dispatch! :threads-for-tag (get-in data [:page :id])))
    om/IRender
    (render [_]
      (let [page (data :page)
            tag-id (page :id)
            tag (get-in data [:tags tag-id])
            user-id (get-in data [:session :user-id])
            status (cond
                     (not (contains? page :thread-ids)) :searching
                     (seq (page :thread-ids)) :done-results
                     :else :done-empty)
            threads (->> (page :thread-ids)
                         (select-keys (data :threads))
                         vals
                         ; sort-by last reply, newest first
                         (sort-by
                           (comp (partial apply max)
                                 (partial map :created-at)
                                 :messages))
                         reverse)]
        (dom/div #js {:className "page channel"}
          (dom/div #js {:className "title"}
            (om/build tag-view tag)
            (subscribe-button tag))

          (dom/div #js {:className "description"}
            (dom/p nil "One day, a channel description will be here.")

            (dom/div nil
              (case status
                :searching "Searching..."
                :done-results "Done!"
                :done-empty "No Results")))


          (apply dom/div #js {:className "threads"}
            (concat
              [(new-thread-view {:tag-ids [tag-id]})]
              (map (fn [t] (om/build thread-view t {:key :id}))
                   threads))))))))