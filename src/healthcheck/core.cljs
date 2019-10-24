(ns healthcheck.core
  (:require
   ["aws-sdk" :as AWS]
   ["node-fetch" :as node-fetch]
   [clojure.string :as string]
   [goog.object :as gobj]
   [goog.string :as gstring]
   [goog.string.format]))

(def ses (new AWS/SES))
(def sns (new AWS/SNS))

(def icons
  {:ok  "✅"
   :nok "❌"})

(defn ->message [[url status]]
  (gstring/format "%s %s" (icons status) url))

(defn send-sms [{:keys [phone-number sender-id responses] :as context}]
  (let [message (->> responses
                     (map ->message)
                     (string/join "\n"))
        params  {:Message           message
                 :PhoneNumber       phone-number
                 :MessageAttributes {"AWS.SNS.SMS.SenderID"
                                     {:DataType    "String"
                                      :StringValue sender-id}}}]
    (-> sns
        (.publish (clj->js params))
        .promise
        (.then #(assoc context :result %)))))

(defn ->html [messages]
  (str
   "<html><body>"
   "<h1>Healthcheck alert</h1>"
   "<ul>"
   (->> messages
        (map #(str "<li>" % "</li>"))
        (string/join))
   "</ul>"
   "</body></html>"))

(defn send-email [{:keys [responses email email-sender] :as context}]
  (let [html    (->> responses (map ->message) ->html)
        charset "UTF-8"
        params  {:Destination {:ToAddresses [email]}
                 :Message
                 {:Subject {:Charset charset :Data "Healthcheck alert"}
                  :Body    {:Html {:Charset charset :Data html}
                            :Text {:Charset charset :Data (pr-str responses)}}}
                 :Source      email-sender}]
    (-> ses
        (.sendEmail (clj->js params))
        .promise
        (.then #(assoc context :result %)))))

(defn notify [{:keys [method] :as context}]
  (println "Healthcheck failed. Notifying via" method)
  (case method
    "email" (send-email context)
    "sms"   (send-sms context)
    (throw (js/Error. (gstring/format "Unkonwn delivery method '%'" method)))))

(defn maybe-notify [{:keys [responses] :as context}]
  (if (->> responses (map second) (some #(= % :nok)))
    (notify context)
    (assoc context :result (gstring/format "%s" responses))))

(defn fetch [url]
  (-> (node-fetch url)
      (.then (fn [res] [url (if (.-ok res) :ok :nok)]))
      (.catch (fn [_err] [url :nok]))))

(defn check-urls [{:keys [urls] :as context}]
  (-> (js/Promise.all (map fetch urls))
      (.then (fn [coll] (assoc context :responses coll)))))

(defn check-health [context]
  (-> (check-urls context)
      (.then maybe-notify)
      (.then println)))

(defn main [_evt _ctx]
  (let [urls (gobj/get js/process.env "HEALTHCHECK_URLS")
        context
        {:urls         (string/split urls ",")
         :method       (gobj/get js/process.env "DELIVERY_METHOD")
         :phone-number (gobj/get js/process.env "SMS_PHONE_NUMBER")
         :sender-id    (gobj/get js/process.env "SMS_SENDER_ID")
         :email        (gobj/get js/process.env "EMAIL_RECIPIENT")
         :email-sender (gobj/get js/process.env "EMAIL_SENDER")}]
    (println (gstring/format "Checking health for %s" urls))
    (check-health context)))
