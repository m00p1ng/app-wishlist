(ns app-wishlist.core
  (:require [clj-http.client :as client]
            [cheshire.core :refer :all]
            [clojure.java.io :as io]))

; (defn get-price
;   [app-id]
;   (def result
;     (client/get
;      (str "https://itunes.apple.com/lookup?country=th&id=" app-id)
;      {:as :json}))
;   (int (get-in result [:body :results 0 :price])))

(defn get-price
  [app-id]
  179)

(defn retrive-data-from-database
  [filename]
  (if (.exists (io/as-file filename))
    (parse-string (slurp filename) true)
    {}))

(defn filter-lower-price
  [app-list
   database
   price-list]
  (filter
   (fn [app]
     (let [kw-app-id (keyword (:id app))
           previous-price (kw-app-id database)
           current-price (kw-app-id price-list)]
       (and
        (not= previous-price nil)
        (not= current-price nil)
        (< current-price previous-price))))
   app-list))

(defn get-price-list
  [app-list]
  (reduce
   (fn [acc app]
     (assoc acc (keyword (:id app))
            (get-price (:id app))))
   {}
   app-list))

(defn attach-current-price
  [app-list
   price-list
   database]
  (map
   (fn [app]
     (let [kw-app-id (keyword (:id app))]
       (assoc app :price (kw-app-id price-list))))
   (filter-lower-price app-list database price-list)))

(defn start
  []
  (def database-filename "database.json")
  (def database
    (retrive-data-from-database database-filename))
  (def app-list
    (parse-string (slurp "app_list.json") true))
  (def price-list
    (get-price-list app-list))
  (def price-down
    (attach-current-price app-list price-list database))
  (spit
   database-filename
   (generate-string price-list))
  (println price-down))

(defn -main
  []
  (while true
    (start)
    (Thread/sleep 2000)))