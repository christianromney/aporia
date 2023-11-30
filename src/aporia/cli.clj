(ns aporia.cli
  "A prototyping playground for Chat GPT"
  (:gen-class)
  (:require [wkok.openai-clojure.api :as openai]
            [clojure.datafy :refer [datafy]]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [clojure.reflect :refer [reflect]])
  (:import [java.nio.file Paths Files]))

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
  I can use to compare all the alternatives fairly."}
   :socratic-foil
   {:role "system"
    :content "You are a trusted colleague who is helping me sharpen my
    reasoning and analysis skills via Socratic dialogue. You are not easily
    convinced of my ideas. Whenever I suggest an idea, you probe my answer
    looking for possible weaknesses. You might draw attention to these
    drawbacks. You might also offer a couple of alternative ideas for me to
    consider. Our goal is to sharpen ideas by looking for weaknesses.
    However, whenever we find a weakness we try to think of ways to mitigate
    them."}
   :socratic-dreamer
   {:role "system"
    :content "You are a friendly colleague engaged in a group problem solving
    exercise. Whenever I suggest an idea, you might suggest a more general
    approach or broader applications of the idea. You are trying to expand the
    range of possibilities we consider. You prefer to find reusable, categorical
    solutions to general problems."}
   :socratic-focuser
   {:role "system"
    :content "You are a friendly colleague engaged in a group problem solving
   exercise. Whenever I suggest an idea, you try to narrow the focus or
   constrain the approach. You may offer suggestions that limit the
   possibilities we consider in order to prevent long implementation
   timelines and scope creep."}})

(defn- format-user-input
  "Creates a prompt message representing user input into a chat conversation."
  [{:keys [input] :as data}]
  {:role "user" :content input})

(defn read-metadata
  "Return the timestamp and token count."
  [response]
  (dissoc response :choices))

(defn read-chat-reply
  "Extracts the text of the chat reply from a response message."
  [response]
  (get-in response [:choices 0 :message :content]))

(defn find-interlocutor
  "Looks for the system prompt identified by the :persona, defaulting
  to :socratic-trainer if no value was supplied. If a known persona is
  requested, its prompt message is returned. Otherwise a random persona's
  message will be selected."
  [{:keys [persona] :or {persona :socratic-trainer}}]
  (or (system-prompts persona)
      (val (rand-nth (seq system-prompts)))))

(defn send-chat-message
  "Chats with an OpenAI GPT model using the system prompt specified by the
  given :persona and the supplied :input. Defaults to the gpt4 :model."
  [{:keys [model] :or {model gpt4} :as data}]
  (openai/create-chat-completion {:model model
                                  :messages
                                  [(find-interlocutor data)
                                   (format-user-input data)]}))

(defn print-response
  "Prints a chat completion api response to stdout."
  [response]
  (tap> {:stats (read-metadata response)})
  (println (read-chat-reply response)))

(defn spoken-response
  "Given a chat completion ressponse, invokes the Whisper API to convert the text
  to mp3 audio. Returns an input stream containing that audio content."
  [response]
  (openai/create-speech {:model "tts-1"
                         :input (read-chat-reply response)
                         :voice (random-nth ["onyx" "alloy" "nova"])
                         :response_format "mp3"}))

(defn save-stream-to-file
  "Writes the input stream to the given file path."
  [input-stream path]
  (with-open [is input-stream]
    (io/copy is (io/file path))))

(defn valid-path?
  "Returns a valid path object ot throws an IllegalArgumentException."
  [path]
  (Paths/get path (into-array String [])))

(defn -main
  "Command Line entrypoint to canned ChatGPT prompts."
  [& args]
  (try
    (let [input {:input (first args)}
          path (second args)
          response (send-chat-message input)]
      (print-response response)
      (when (valid-path? path)
        (-> response spoken-response (save-stream-to-file path))))
    (catch Exception ex
      (println (datafy ex)))))
