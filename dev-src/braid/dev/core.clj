(ns braid.dev.core
  "Starting namespace for Braid REPL. Utilities for dev."
  (:require
   [braid.base.conf :refer [config]]
   [braid.base.server.seed :as seed]
   [braid.chat.seed :as chat.seed]
   [braid.core.modules :as modules]
   [braid.core.server.db :as db]
   [datomic.api :as d]
   [mount.core :as mount]
   [environ.core :refer [env]]
   ;; the following namespaces are required for their mount components:
   [braid.core]
   [braid.dev.figwheel]))

(defn drop-db!
  "Drops the database (and also the database connection).
  To re-seed, you'll need to make a new connection, usually by the (start!) function in dev.core"
  []
  (d/delete-database (config :db-url)))

(defn init-db! []
  (db/init! (config :db-url)))

(defn seed!
  "Seed the database.
   Database connection must be started prior to seeding, so be sure to call start! first."
  []
  (seed/seed!)
  (println "\nYou can log in with:\n"
           (select-keys (chat.seed/users 0) [:user/email :user/password])
           "\nor:\n"
           (select-keys (chat.seed/users 1) [:user/email :user/password])))

(defn start!
  "Helper function to start Braid components in dev.
   Does not start the email worker."
  [port]
  ;; modules must run first
  (modules/init! modules/default)
  (-> (mount/except #{#'braid.core.server.email-digest/email-jobs})
      (mount/with-args (cond-> (assoc env :port port)
                         (env :aws-access-key)
                         (assoc :aws/credentials-provider
                                (constantly ((juxt :aws-access-key :aws-secret-key) env)))))
      (mount/start))
  (when (zero? port)
    (mount/stop #'braid.base.conf/config)
    (mount/start #'braid.base.conf/config)))

(defn stop!
  "Helper function for stopping all Braid components."
  []
  (mount/stop))
