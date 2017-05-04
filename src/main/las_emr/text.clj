(ns las-emr.text
  (:require [clojure.string :as string]
            [clojure.tools.logging :refer [error]])
  (:import (fi.seco.lexical.combined CombinedLexicalAnalysisService)
           (java.util Locale)))

(definterface ILas
  (processText [^String text opts]))

(def ^:private las
  (proxy [CombinedLexicalAnalysisService ILas] []
    (processText [text opts]
      (let [lang (Locale. "fi")
            inflections (list)]
        (->> (.analyze this text lang inflections
                       (:baseform-segments? opts)
                       (:guess-unknown? opts)
                       (:segment-unknown? opts)
                       (:max-error-distance opts)
                       (:depth opts))
             (filter (fn [wtr] (not-empty (string/trim (.getWord wtr)))))
             (map (fn [wtr] {:wordform (string/trim (.getWord wtr))
                             :baseform (string/trim (proxy-super getBestLemma wtr lang false))}))
             (seq)
             (to-array))))))

; ====

(defn process [text]
  (-> (try
        (let [opts {:baseform-segments? true
                    :guess-unknown?     true
                    :segment-unknown?   true
                    :max-error-distance 1
                    :depth              1}]
          (if (empty? text)
            [true (to-array [])]
            [true (.processText las text opts)]))
        (catch Throwable e
          (error e "Got error while processing text:" text)
          [false (to-array [])]))
      (to-array)))


#_(process "Helsingissä sataa lumipalloja ja Pariisissa paistaa aurinko.")
; =>
; [true [["Helsingissä" "Helsinki"]
;        ["sataa" "sataa"]
;        ["lumipalloja" "lumipallo"]
;        ["ja" "ja"]
;        ["Pariisissa" "Pariisi"]
;        ["paistaa" "paistaa"]
;        ["aurinko" "aurinko"]
;        ["." "."]]]
