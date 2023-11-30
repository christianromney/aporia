(ns aporia.chat
  (:require [wkok.openai-clojure.api :as openai]
            [aporia.models :as models]
            [aporia.prompts :as prompts]))

;; -- private --

(defn- text->user-message
  "Creates a prompt message representing user input into a chat conversation."
  [text]
  {:role "user" :content text})

;; == public ==

(defn send-message
  "Chats with an OpenAI GPT model using the system prompt specified by the
  given :persona and the supplied :input. Defaults to the gpt4 :model."
  [{:keys [model persona input] :or {model models/gpt4}}]
  (let [system-prompt (or (prompts/persona-prompts persona)
                          (prompts/random-prompt))]
    (openai/create-chat-completion {:model model
                                    :messages [system-prompt
                                               (text->user-message input)]})))
(defn get-response-metadata
  "Return a map containing the message id, timestamp and token usage information
  from a chat response."
  [response]
  (dissoc response :choices))

(defn get-response-text
  "Extracts the text of the chat reply from a response message."
  [response]
  (get-in response [:choices 0 :message :content]))
