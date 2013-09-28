# Mummy

A small library to generate [Korma](https://github.com/korma/Korma) queries from [Ring](https://github.com/ring-clojure/ring-json) requests

## Muy Importante!!!
- Field types in query must match the db ones, so if a field's type is not a string, then you must specify it's type using the `fields-types` function on the Korma entity.
- For POST and PUT requests, form parameters will be considered as input for insert and update queries.
- For GET /entity/:id, the pk from the entity will be considered as id.

## Installation
Add to your project.clj:
```clj
[mummy "0.1.0-SNAPSHOT"]
```
## Usage
- Main function: `->query` (request requires Ring's wrap-params middleware)
- Ring Middleware: `wrap-sql-query`

Based on a Korma entity the following requests will be transformed to their corresponding queries
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
### Example
```clj
(ns my-crud-app
  (:use mummy.core
        korma.core))

(defentity users
  ;; Specify types for fields
  (fields-types
    {:id         :int
     :birth-date your-date-parser})

  ;; (optional) limit query string fields criteria on GET /users
  (query-fields :id :name :birth-date)

  ;; (optional) limit form parameters on POST /users
  (insert-fields :name :birth-date)

  ;; (optional) limit form parameters on PUT /users/:id
  (update-fields :number))

;; create the query directly inside a handler
(defn handler1
  [request]
  (if-let [query (->query request users)]
    (exec query)))

;; use the wrap-sql-query middleware and get the query from the :sql-query key in the request form your handler
(defn handler2
  [{query :sql-query :as requests}]
  (if query
    (exec query)))

(def handler3
  (wrap-sql-query handler2))
```

## License

Copyright © 2013 Sebastian Rojas

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
