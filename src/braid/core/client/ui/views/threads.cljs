(ns braid.core.client.ui.views.threads
  (:require
   [braid.core.client.ui.views.new-thread :refer [new-thread-view]]
   [braid.core.client.ui.views.thread :refer [thread-view]]
   [clojure.set :refer [difference]]
   [reagent.core :as r]
   [reagent.dom :as r-dom]))

(defn threads-view
  [props]
  (let [threads (r/atom [])
        this-elt (r/atom nil)
        reset-threads! (fn [new-threads]
                         (reset! threads (vec new-threads)))
        update-threads!
        (fn [new-threads]
          (swap! threads
                 (fn [old-threads]
                   (let [old-thread-ids (set (map :id old-threads))
                         new-thread-ids (set (map :id new-threads))
                         to-remove (difference old-thread-ids new-thread-ids)
                         to-add (difference new-thread-ids old-thread-ids)
                         ordered-ids (concat (remove to-remove old-thread-ids)
                                             (filter to-add new-thread-ids))]
                     (mapv (comp first (group-by :id new-threads)) ordered-ids)))))
        scroll-horizontally
        (fn [e]
          (let [target-classes (.. e -target -classList)]
            ; TODO: check if threads-div needs to scroll?
            (when (and (or (.contains target-classes "thread")
                           (.contains target-classes "threads"))
                    (= 0 (.-deltaX e) (.-deltaZ e)))
              (set! (.-scrollLeft @this-elt)
                    (- (.-scrollLeft @this-elt) (.-deltaY e))))))]
    (r/create-class
      {:display-name "threads-view"

       :component-did-mount
       (fn [this]
         (reset-threads! (props :threads))
         (reset! this-elt (r-dom/dom-node this)))

       :component-will-receive-props
       (fn [this [_ next-props]]
         (if (not= ((r/props this) :group-id) (next-props :group-id))
           (reset-threads! (next-props :threads))
           (update-threads! (next-props :threads))))

       :reagent-render
       (fn [{:keys [show-new-thread?
                    threads-opts]
             :or {threads-opts {}}}]
         [:div.threads
          (merge threads-opts
                 {:on-wheel scroll-horizontally})

          (when show-new-thread?
            [new-thread-view])

          (doall
            (for [thread @threads]
              ^{:key (thread :id)}
              [thread-view thread]))])})))
