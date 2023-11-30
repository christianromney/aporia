(ns aporia.cli
  (:gen-class)
  (:require [wkok.openai-clojure.api :as openai]))

(def ^:const gpt4 "gpt-4-1106-preview")

(def system-prompts
  "A table of named system prompts used to seed ChatGPT conversations."
  {:socratic-trainer
   {:role "system"
    :content "You are a thinking partner meant to train me by Socratic dialog.
  You should help me to identify and formulate good problem statements that
  admit of multiple solutions and provide hints for doing so on my own. Good
  problem statements exhibit a tension between a goal and one or more obstacles
  or impediments to the goal's achievement. My objective is always to indentify
  root problems and not merely symptoms. When I say I want to do something, you
  should probe whether that is really my ultimate objective or merely a waypoint
  implied by a presumed solution the a more fundamental or general problem. Always
  suggest a few concrete alternatives to my idea and offer a few criteria
  I can use to compare all the alternatives fairly."}})

(defn- user-prompt
  "Creates a prompt message representing user input into a chat conversation."
  [{:keys [input] :as data}]
  (tap> {:prompt data})
  {:role "user" :content input})

(defn read-chat-reply
  "Extracts the text of the chat reply."
  [response]
  (tap> response)
  (get-in response [:choices 0 :message :content]))

(defn send-chat-message
  "Chats with an OpenAI GPT model using the system prompt specified by the
  given :persona and the supplied :user-input. Defaults to the GPT4 :model."
  [{:keys [persona model] :or {model gpt4
                               persona :socratic-trainer} :as data}]
  (openai/create-chat-completion {:model model
                                  :messages [(system-prompts persona)
                                             (user-prompt data)]}))
(defn chat
  "High-level function to send a chat message (with possible history) and return
  only the text of the reply."
  [data]
  (->> data send-chat-message read-chat-reply))

(defn -main
  "Command Line ChatGPT"
  [& args]
  (try
    (println (chat {:input (first args)}))
    (catch Exception ex
      (println ex))))
