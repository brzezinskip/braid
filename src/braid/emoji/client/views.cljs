(ns braid.emoji.client.views
  (:require
   [braid.core.client.s3 :as s3]
   [clojure.string :as string]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]))

(defn new-custom-emoji-view
  []
  (let [shortcode (r/atom "")
        uploading? (r/atom false)
        image-url (r/atom nil)]
    (fn []
      [:div.new-emoji
       [:label "Shortcode (omit ':')"
        ; TODO: validate shortcode is sensible
        [:input {:placeholder "shortcode"
                 :value @shortcode
                 :on-change (fn [e]
                              (reset! shortcode
                                      (.. e -target -value)))}]]
       [:label
        (if @uploading? "Uploading..."
            [:input {:type "file"
                     :multiple false
                     :on-change (fn [e]
                                  (reset! uploading? true)
                                  (s3/upload
                                    (aget (.. e -target -files) 0)
                                    (fn [url]
                                      (reset! uploading? false)
                                      (reset! image-url url))))}])]
       [:button
        {:disabled (or (string/blank? @shortcode)
                       (string/blank? @image-url)
                       @uploading?)
         :on-click (fn [_]
                     (dispatch [:emoji/add-emoji
                                {:group-id @(subscribe [:open-group-id])
                                 :shortcode @shortcode
                                 :image @image-url}])
                     (reset! shortcode "")
                     (reset! image-url nil))}
        "Add"]
       [:br]
       (when-let [url @image-url]
         [:img {:src url :width "300"}])])))

(defn extra-emoji-view
  [emoji]
  (let [editing? (r/atom false)
        new-code (r/atom (->> (emoji :shortcode)
                              (re-matches #"^:([^:]+):$")
                              second))]
    (fn [emoji]
      [:tr
       [:td
        (if @editing?
          [:span
           [:input {:value @new-code
                    :on-change (fn [e] (reset! new-code (.. e -target -value)))}]
           [:button
            {:on-click (fn [_]
                         (dispatch [:emoji/edit-emoji
                                    (emoji :id) @new-code])
                         (reset! editing? false))}
            "Save"]
           [:button {:on-click (fn [_] (reset! editing? false))}
            "Cancel"]]
          [:span (emoji :shortcode)
           [:button {:on-click (fn [_] (reset! editing? true))} "Edit"]])]
       [:td [:img {:src (emoji :image)}]]
       [:td [:button.delete
             {:on-click (fn [_] (dispatch [:emoji/retract-emoji (emoji :id)]))}
             \uf1f8]]])))

(defn extra-emoji-settings-view
  [group]
  [:div.settings.custom-emoji
   [:h2 "Custom Emoji"]
   [new-custom-emoji-view]
   (if-let [emojis (seq @(subscribe [:emoji/group-emojis (group :id)]))]
     [:table
      [:thead
       [:tr [:th "shortcode"] [:th ""] [:th ""]]]
      [:tbody
       (for [emoji emojis]
         ^{:key (emoji :id)}
         [extra-emoji-view emoji])]]
     [:p "No custom emoji yet"])])
