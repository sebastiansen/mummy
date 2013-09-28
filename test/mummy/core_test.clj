(ns mummy.core-test
  (:use clout.core
        korma.core
        korma.db
        ring.middleware.params
        ring.mock.request
        korma.sql.fns
        clj-time.format
        clj-time.coerce
        mummy.core
        clojure.test)
  (:require [clj-time.core :as clj-time]))

(defdb test-db (postgres {:db "korma" :user "korma" :password "kormapass"}))

(defentity users
  (fields-types {:id         :int
                 :number     :int
                 :birth-date (comp to-sql-time parse)})
  (query-fields :id :name :number :birth-date)
  (insert-fields :name :number :birth-date)
  (update-fields :number))

(def handler (wrap-sql-query :sql-query users))

(deftest crud
  (are [result query] (= result query)

       "dry run :: SELECT \"users\".* FROM \"users\" :: []\n"
       (->
        (handler (request :get "/users"))
        exec
        dry-run
        with-out-str)

       "dry run :: SELECT \"users\".* FROM \"users\" WHERE (\"users\".\"name\" = ? AND \"users\".\"number\" = ? AND \"users\".\"birth-date\" = ?) :: [Sebastian 1 #inst \"1985-07-30T00:00:00.000000000-00:00\"]\n"
       (->
        (handler
         (-> (request :get "/users")
             (query-string {:name       "Sebastian"
                            :number     1
                            :birth-date (clj-time/date-time 1985 7 30)})))
        exec
        dry-run
        with-out-str)

       "dry run :: INSERT INTO \"users\" (\"name\", \"number\", \"birth-date\") VALUES (?, ?, ?) :: [Sebastian 1 #inst \"1985-07-30T00:00:00.000000000-00:00\"]\n"
       (->
        (handler
         (-> (request :post "/users")
             (body {:name       "Sebastian"
                    :number     1
                    :birth-date (clj-time/date-time 1985 7 30)})))
        exec
        dry-run
        with-out-str)

       "dry run :: SELECT \"users\".* FROM \"users\" WHERE (\"users\".\"id\" = ?) LIMIT 1 :: [1]\n"
       (->
        (handler
         (-> (request :get "/users/1")))
        exec
        dry-run
        with-out-str)

       "dry run :: UPDATE \"users\" SET \"number\" = ? WHERE (\"users\".\"id\" = ?) :: [1 1]\n"
       (->
        (handler
         (-> (request :put "/users/1")
             (body {:number 1})))
        exec
        dry-run
        with-out-str)

       "dry run :: DELETE FROM \"users\" WHERE (\"users\".\"id\" = ?) :: [1]\n"
       (->
        (handler
         (request :delete "/users/1"))
        exec
        dry-run
        with-out-str)))
