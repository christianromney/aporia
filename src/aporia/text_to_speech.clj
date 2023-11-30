(ns aporia.text-to-speech
  (:require [wkok.openai-clojure.api :as openai]
            [clojure.java.io :as io]))

(def ^:const voice-alloy "alloy")
(def ^:const voice-echo "echo")
(def ^:const voice-fable "fable")
(def ^:const voice-nova "nova")
(def ^:const voice-onyx "onyx")
(def ^:const voice-shimmer "shimmer")

;; -- private --

(defn- random-voice
  "Returns a random Open AI Whisper voice."
  []
  (rand-nth
   [voice-alloy
    voice-echo
    voice-fable
    voice-nova
    voice-onyx
    voice-shimmer]))

(defn- save-stream-to-file
  "Writes the input stream to the given file path."
  [input-stream path]
  (with-open [is input-stream]
    (io/copy is (io/file path))))

;; == public ==

(defn text-to-speech
  "Convert the given :text to spoken word using the specified :voice and save it
  to the file :path in the requested audio :file-format."
  [{:keys [text voice file-format path]
    :or {voice (random-voice)
         file-format "mp3"}}]
  (let [tts-response (openai/create-speech {:model "tts-1"
                                            :input text
                                            :voice voice
                                            :response_format file-format})]
    (save-stream-to-file tts-response path)))
