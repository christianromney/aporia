(ns aporia.aporia
  (:gen-class)
  (:require [wkok.openai-clojure.api :as openai]))

(def ^:const gpt4 "gpt-4-1106-preview")

(def system-prompts
  "A table of named system prompts used to seed ChatGPT conversations."
  {:socratic-partner
   {:role "system"
    :content "You are a Socratic partner meant to help me identify and formulate
  good problem statements that admit of multiple solutions. Good problem
  statements exhibit a tension between a goal and one or more obstacles or
  impediments to achieving that goal. Our objective is to indentify root
  problems and not merely symptoms. When I say I want to do something, you
  should probe whether that is really my ultimate objective or merely a
  perceived waypoint that presumes a given to solution to a more fundamental or
  general problem."}})

(defn- prompt
  "Creates a prompt message representing user input into a chat conversation."
  [{:keys [user-input] :as data}]
  (tap> {:prompt data})
  {:role "user" :content user-input})

(defn- extract-answer
  "Extracts the text of the chat response"
  [response]
  (get-in response [:choices 0 :message :content]))

(defn chat
  "Chats with an OpenAI GPT model using the system prompt specified by the
  given :persona and the supplied :user-input. Defaults to the GPT4 :model."
  [{:keys [persona model]
    :or {model gpt4
         persona :socratic-partner} :as data}]
  (openai/create-chat-completion {:model model
                                  :messages [(system-prompts persona)
                                             (prompt data)]}))
(defn -main
  "Command Line ChatGPT"
  [& args]
  (try
    (let [response (chat {:user-input (first args)})]
      (tap> {:stats (dissoc response :choices)})
      (println (extract-answer response)))
    (catch Exception ex
      (println ex))))
