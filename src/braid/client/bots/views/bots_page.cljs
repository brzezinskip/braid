(ns braid.client.bots.views.bots-page
  (:require
    [clojure.string :as string]
    [reagent.core :as r]
    [reagent.ratom :refer-macros [reaction]]
    [re-frame.core :refer [dispatch subscribe]]
    [braid.client.ui.views.pills :refer [user-pill-view]]
    [braid.client.ui.views.upload :refer [avatar-upload-view]]
    [braid.common.util :refer [bot-name-re]]))

(defn bot-view
  [bot]
  (let [group-id (subscribe [:open-group-id])
        admin? (subscribe [:current-user-is-group-admin?] [group-id])
        editing? (r/atom false)
        edited-info (r/atom nil)
        detailed-info (r/atom nil)
        dragging? (r/atom false)]
    (fn [bot]
      [:div.bot
       [:img.avatar {:src (:avatar bot)}]
       (:nickname bot)
       (when @admin?
         [:div
          [:button.dangerous
           {:on-click (fn [_]
                        (when (js/confirm "Permanently remove this bot?")
                          (dispatch [:retract-bot {:bot-id (bot :id)}])))}
           "Delete Bot"]
          (if-let [info @detailed-info]
            [:div
             (into
               [:dl]
               (mapcat
                 (fn [[k v]]
                   [[:dt (name k)]
                    [:dd (if @editing?
                           (case k
                             (:id :user-id :group-id :token) v

                             :avatar
                             [:div.dragging.new-avatar
                              {:class (when @dragging? "dragging")}
                              (when-let [avatar (get @edited-info :avatar)]
                                [:img {:src avatar}])
                              [avatar-upload-view
                               {:on-upload (fn [url] (swap! edited-info assoc :avatar url))
                                :dragging-change (partial reset! dragging?)}]]

                             (:webhook-url :event-webhook-url)
                             [:input
                              {:value (get @edited-info k)
                               :type "url"
                               :on-change (fn [e] (swap! edited-info assoc k
                                                         (.. e -target -value)))}]

                             [:input
                              {:value (get @edited-info k)
                               :type "text"
                               :on-change (fn [e] (swap! edited-info assoc k
                                                         (.. e -target -value)))}])
                           v)]]))
               info)
             [:button
              {:on-click (fn [_]
                           (when-not @editing?
                             (reset! edited-info @detailed-info))
                           (swap! editing? not))}
              (if @editing? "Cancel" "Edit")]
             (when @editing?
               [:button
                {:on-click (fn [_]
                             (dispatch
                               [:edit-bot
                                {:bot @edited-info
                                 :on-complete
                                 (fn [updated]
                                   (when updated
                                     (do (reset! detailed-info @edited-info)
                                         (reset! editing? false))))}]))}
                "Save"])
             [:button {:on-click (fn [_] (reset! detailed-info nil))}
              "Hide"]]
            [:button
             {:on-click (fn [_]
                          (dispatch
                            [:get-bot-info
                             {:bot-id (bot :id)
                              :on-complete (fn [info]
                                             (reset! detailed-info info))}]))}
             "See bot config"])])])))

(defn group-bots-view []
  (let [group-id (subscribe [:open-group-id])
        group-bots (subscribe [:group-bots] [group-id])]
    (fn []
      [:div.bots-list
       (if (empty? @group-bots)
         [:h2 "No bots in this group"]
         (doall (for [b @group-bots]
                  ^{:key (:id b)}
                  [bot-view b])))])))

(defn new-bot-view []
  (let [group-id (subscribe [:open-group-id])
        admin? (subscribe [:current-user-is-group-admin?] [group-id])
        dragging? (r/atom false)
        error (r/atom nil)
        new-bot (r/atom {})
        state (r/atom :initial)
        bot-valid? (reaction (and
                               (and (string? (@new-bot :name))
                                 (re-matches bot-name-re (@new-bot :name)))
                               (and (string? (@new-bot :webhook-url))
                                 (re-matches #"\S+" (@new-bot :webhook-url)))
                               (some? (@new-bot :avatar))))]
    (fn []
      (when @admin?
        [:div.add-bot
         [:h2 "Add a New Bot"]
         (case @state

           :creating
           [:div "Creating..."]

           :initial
           [:form.new-bot-form
            {:action "/"
             :on-submit (fn [e] (.preventDefault e)
                          (when @bot-valid?
                            (reset! error nil)
                            (reset! state :creating)
                            (swap! new-bot assoc :group-id @group-id)
                            (dispatch [:new-bot
                                       {:bot @new-bot
                                        :on-complete
                                        (fn [created]
                                          (if created
                                            (do (reset! new-bot created)
                                                (reset! state :created))
                                            (do
                                              (reset! error "Failed to create bot")
                                              (reset! state :initial))))}])
                            (reset! state :creating)))}
            (when-let [err @error]
              [:div.error err])
            [:label "Bot Name"
             [:input {:type "text" :placeholder "name" :value (@new-bot :name)
                      :on-change #(swap! new-bot assoc :name (.. % -target -value))}]]
            [:br]
            [:label "Webhook URL"
             [:input {:type "url"
                      :placeholder "https://example.com/bot_message"
                      :value (@new-bot :webhook-url)
                      :on-change #(swap! new-bot assoc :webhook-url (.. % -target -value))}]]
            [:br]
            [:div.dragging.new-avatar {:class (when @dragging? "dragging")}
             (when-let [avatar (@new-bot :avatar)]
               [:img {:src avatar}])
             [avatar-upload-view {:on-upload (fn [url] (swap! new-bot assoc :avatar url))
                                  :dragging-change (partial reset! dragging?)}]]

            [:br]
            [:label "Optional: Group event webhook url"
             [:input
              {:type "url"
               :placeholder "https://example.com/bot_group_event"
               :value (@new-bot :event-webhook-url)
               :on-change
               (fn [e] (let [url (.. e -target -value)]
                         (if (string/blank? url)
                           (swap! new-bot dissoc :event-webhook-url)
                           (swap! new-bot assoc :event-webhook-url url))))}]]
            [:br]
            [:input {:type "submit" :value "Add Bot" :disabled (not @bot-valid?)}]]

           :created
           [:div
            [:p (str "New bot has been created! Here are the credentials you'll "
                     "need to connect")]
            [:label "Bot ID"
             [:input {:type "text"
                      :value (@new-bot :id)
                      :read-only true
                      :on-focus #(.. % -target select)}]]

            [:label "Bot Token"
             [:input {:type "text"
                      :value (@new-bot :token)
                      :read-only true
                      :on-focus #(.. % -target select)}]]
            [:button {:on-click (fn [_]
                                  (reset! new-bot {})
                                  (reset! state :initial))}
             "Done"]])]))))

(defn bots-page-view []
  [:div.page.bots
   [:div.title "Bots"]
   [:div.content
    [group-bots-view]
    [new-bot-view]]])
