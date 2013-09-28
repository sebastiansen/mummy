(ns mummy.core
  (:use clout.core
        korma.core
        ring.middleware.params
        korma.sql.fns))

(def parsers
  {:int        #(if % (Integer/parseInt %))
   :double     #(if % (Double/parseDouble %))
   :float      #(if % (Float/parseFloat %))
   :boolean    #(Boolean/parseBoolean %)
   :long       #(if % (Long/parseLong %))
   :bigint     #(if % (BigInteger. %))
   :bigdecimal #(if % (BigDecimal. %))
   :uuid       #(if % (java.util.UUID/fromString %))})

(defn fields-types
  "Associate fields types to an entity with a map of field - type key-values.
   The field type can be a function to parse the param from a string,
   or one of the following keywords:
     :int
     :double
     :float
     :boolean
     :long
     :bigint
     :bigdecimal
     :uuid"
  [entity types]
  (assoc entity :fields-types types))

(defn query-fields
  "Associate fields to an entity to filter query params from request"
  [entity & fields]
  (assoc entity :query-fields (set fields)))

(defn insert-fields
  "Associate fields to an entity to filter form params from request"
  [entity & fields]
  (assoc entity :insert-fields (set fields)))

(defn update-fields
  "Associate fields to an entity to filter form params from request"
  [entity & fields]
  (assoc entity :update-fields (set fields)))

(defn- prepare-query-map
  "doc-string"
  [fields params types]
  (reduce
   (fn [params [param value]]
     (let [param (keyword param)]
       (if (contains? fields param)
         (let [value (if-let [parser (param types)]
                       ((cond
                         (keyword? parser) (parsers parser)
                         (fn? parser) parser)
                        value)
                       value)]
           (assoc params param value))
         (if fields
           params
           (let [value (if-let [parser (param types)]
                         ((cond
                           (keyword? parser) (parsers parser)
                           (fn? parser) parser)
                          value)
                         value)]
             (assoc params param value))))))
   {}
   params))

(defn collection-query
  "Create a sql korma query for an entity based on the following requests:
     GET /<entity-name>?<query>
     POST /<entity-name> (form paramters)"
  [{:keys [request-method query-params form-params] :as request}
   {:keys [name pk query-fields insert-fields fields-types] :as entity}]
  (if (route-matches (route-compile (str "/" name)) request)
    (case request-method
      :get  (-> (select* entity)
                (where (prepare-query-map
                        query-fields query-params fields-types)))
      :post (-> (insert* entity)
                (values (prepare-query-map
                         insert-fields form-params fields-types))))))

(defn member-query
  "Create a sql korma query for an entity based on the following requests:
     GET /<entity-name>/<id>
     PUT /<entity-name>/<id> (form paramters)
     DELETE /<entity-name>/<id>"
  [{:keys [request-method form-params] :as request}
   {:keys [pk update-fields fields-types query-fields] :as entity}]
  (if-let [{id :id} (route-matches
                     (route-compile (str "/" (:name entity) "/:id"))
                     request)]
    (-> (case request-method
          :get    (-> (select* entity)
                      (limit 1))
          :put    (-> (update* entity)
                      (set-fields (prepare-query-map
                                   update-fields form-params fields-types)))
          :delete (-> (delete* entity)))
        (where (prepare-query-map
                query-fields {(name pk) id} fields-types)))))

(defn ->query
  "Create a sql korma query for an entity based on the following requests:
     GET /<entity-name>?<query>
     POST /<entity-name> (form paramters)
     GET /<entity-name>/<id>
     PUT /<entity-name>/<id> (form paramters)
     DELETE /<entity-name>/<id>"
  [request entity]
  (or (collection-query request entity)
      (member-query request entity)))

(defn- wrap-sql-query*
  [handler entity]
  (fn [request]
    (let [query (->query request entity)]
      (handler (assoc request :sql-query query)))))

(defn wrap-sql-query
  "Middleware to create a sql korma query for an entity based on the following requests:
     GET /<entity-name>?<query>
     POST /<entity-name> (form paramters)
     GET /<entity-name>/<id>
     PUT /<entity-name>/<id> (form paramters)
     DELETE /<entity-name>/<id>
   Adds the query to the key :korma-query in request map."
  [handler entity]
  (-> handler
      (wrap-sql-query* entity)
      wrap-params))
