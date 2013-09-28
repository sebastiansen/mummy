# Mummy

A small library to generate Korma queries from Ring requests

## Importante
- Field types in query must match the db ones, so if a field's type is not a string, then you must specify it's type using the `fields-types` function on the Korma entity.
- For POST and PUT requests form parameters will be considered as input for insert and update queries.
- For GET /entity/:id requests the pk from the entity will be considered as id

## Usage
Based on a Korma entity the following requests will be transformed to queries
```
GET /<entity-name>?<query>
=> (select entity (where query))

POST /<entity-name> (form paramters)
=> (insert entity (values (form parameters)))

GET /<entity-name>/<id>
=> (select entity (where {entity-pk id}) (limit 1))

PUT /<entity-name>/<id> (form paramters)
=> (update entity (where {entity-pk id}) (set-fields (form parameters)))

DELETE /<entity-name>/<id>
=> (delete entity (where {entity-pk id}))
```

```clj
(ns my-crud-app
  (:use mummy.core))

(defentity users
  (fields-types
    {:id         :int
     :birth-date your-date-parser})

  ;; optional to limit query string fields criteria on GET /users
  (query-fields :id :name :birth-date)

  ;; optional to limit form parameters on POST /users
  (insert-fields :name :birth-date)

  ;; optional to limit form parameters on PUT /users/:id
  (update-fields :number))

;; create the query directly inside a handler
(defn handler1
  [request]
  (if-let [query (->query request users)]
    (exec query)))

;; use the wrap-sql-query middleware and
;; get the query from the :sql-query in your handler
(defn handler2
  [{query :sql-query :as requests}]
  (if query
    (exec query)))

(def app
  (-> handler2
      (wrap-sql-query)))
```

## License

Copyright © 2013 Sebastian Rojas

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
