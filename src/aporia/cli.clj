(ns aporia.cli
  "A prototyping playground for Chat GPT"
  (:gen-class)
  (:require [aporia.chat :as chat]
            [aporia.text-to-speech :as tts]
            [clojure.datafy :refer [datafy]]
            [clojure.pprint :refer [pprint]])
  (:import [java.nio.file Paths]))

;; -- private --

(defn- valid-path?
  "Returns a java.nio.file.Path if the supplied path string is valid or nil
  otherwise."
  [path]
  (try
    (Paths/get path (into-array String []))
    (catch IllegalArgumentException bad-path
      nil)))

(defn- parse-positional-args
  "Returns a map naming the positional arguments supplied to -main."
  [[persona text audio-filename]]
  {:input text
   :path audio-filename
   :persona (keyword persona)})

;; == public ==

(defn -main
  "Command Line entrypoint to canned ChatGPT prompts."
  [& args]
  (try
    (let [{:keys [input path] :as opts}
          (parse-positional-args args)
          response   (chat/send-message opts)
          chat-reply (chat/get-response-text response)]
      (println "# User Input:\n")
      (println input)
      (println "\n# Response:\n")
      (println chat-reply)
      (when (valid-path? path)
        (tts/text-to-speech {:text chat-reply :path path})))
    (catch Exception ex
      (println (datafy ex)))))
