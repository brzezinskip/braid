(ns braid.client.gateway.user-auth.events
  (:require
    [clojure.string :as string]
    [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]))

(reg-event-fx
  :gateway.user/initialize
  (fn [{state :db} _]
    {:db (-> state
             (assoc
               :user-auth {:user nil
                           :checking? true
                           :register? true
                           :oauth-provider nil}))
     :dispatch [:gateway.user/remote-check-auth]}))

(reg-event-fx
  :gateway.user/set-user
  (fn [{state :db} [_ data]]
    {:db (-> state
           (assoc-in [:user-auth :user] data)
           (assoc-in [:user-auth :checking?] false)
           (assoc-in [:user-auth :oauth-provider] nil))}))

(reg-event-fx
  :gateway.user/switch-account
  (fn [{state :db} _]
    ; TODO ajax request to log-out
    {:dispatch-n [[:gateway.user/set-user nil]
                  [:gateway.user/set-user-register? false]]}))


(defn blank->nil [s]
  (when-not (string/blank? s) s))

(defn move-email-between-forms [state]
  (-> state
      (assoc-in [:fields :user-auth.login/email :value]
        (or (blank->nil (get-in state [:fields :user-auth.login/email :value]))
            (blank->nil (get-in state [:fields :user-auth.register/email :value]))))
      (assoc-in [:fields :user-auth.register/email :value]
        (or (blank->nil (get-in state [:fields :user-auth.register/email :value]))
            (blank->nil (get-in state [:fields :user-auth.login/email :value]))))))

(defn clear-errors [state]
  (-> state
      (assoc-in [:fields :user-auth.login/email :untouched?] true)
      (assoc-in [:fields :user-auth.login/password :untouched?] true)
      (assoc-in [:fields :user-auth.register/email :untouched?] true)
      (assoc-in [:fields :user-auth.register/password :untouched?] true)))

(reg-event-fx
  :gateway.user/set-user-register?
  (fn [{state :db} [_ bool]]
    {:db (-> state
             move-email-between-forms
             clear-errors
             (assoc-in [:user-auth :register?] bool))}))

(reg-event-fx
  :gateway.user/fake-remote-auth
  (fn [{state :db} _]
    (js/setTimeout (fn []
                     (dispatch
                       [:gateway.user/set-user
                        {:id "1234"
                         :nickname "rafd"
                         :email "rafal.dittwald@gmail.com"
                         :avatar "https://en.gravatar.com/userimage/612305/740d38e04f1c21f1fb27e76b5f63852a.jpeg"}]))
      1000)
    {:db (assoc-in state [:user-auth :checking?] true)}))

(reg-event-fx
  :gateway.user/remote-check-auth
  (fn [{state :db} _]
    ; TODO ajax request to check auth status
    (dispatch [:gateway.user/fake-remote-auth])
    {}))

(reg-event-fx
  :gateway.user/remote-oauth
  (fn [{state :db} [_ provider]]
    ; TODO kick off oauth process
    (dispatch [:gateway.user/fake-remote-auth])
    {:db (assoc-in state [:user-auth :oauth-provider] provider)}))

(reg-event-fx
  :gateway.user/remote-log-in
  (fn [{state :db} _]
    ; TODO kick off login process
    (dispatch [:gateway.user/fake-remote-auth])))

(reg-event-fx
  :gateway.user/remote-register
  (fn [{state :db} _]
    ; TODO kick off login process
    (dispatch [:gateway.user/fake-remote-auth])))

