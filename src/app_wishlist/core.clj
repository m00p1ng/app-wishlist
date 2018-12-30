(ns app-wishlist.core
  (:require [clj-http.client :as client]
            [cheshire.core :refer :all]
            [postal.core :refer :all]
            [dotenv :refer [env app-env]]
            [clojure.java.io :as io]))

(defn get-price
  [app-id]
  (def result
    (client/get
     (str "https://itunes.apple.com/lookup?country=th&id=" app-id)
     {:as :json}))
  (int (get-in result [:body :results 0 :price])))

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

(defn send-email
  [app]
  (def email (env "EMAIL"))
  (def pass (env "PASS"))

  (def conn {:host "smtp.gmail.com"
             :ssl true
             :user email
             :pass pass})

  (send-message conn {:from email
                      :to (env "TO")
                      :subject (str (:name app) " is sell now!")
                      :body (str (:name app) " is " (:price app) " baht now!")}))

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
  (doseq
   [app price-down]
    (send-email app)
    (println app)))

(defn -main
  []
  (while true
    (start)
    (Thread/sleep 2000)))