(ns las-emr.text
  (:require [clojure.string :as string])
  (:import (fi.seco.lexical.combined CombinedLexicalAnalysisService)
           (java.util Locale)))

(definterface ILas
  (processText [^String text opts]))

;// text, language, inflections, baseformSegments, guessUnknown, segmentUnknown, maxErrorCorrectDistance, depth
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
             (seq))))))

; ====

(defn process [text]
  (let [opts {:baseform-segments? true
              :guess-unknown?     true
              :segment-unknown?   true
              :max-error-distance 1
              :depth              1}]
    (.processText las text opts)))


#_(process "Helsingissä sataa lumipalloja ja Pariisissa paistaa aurinko.")
; =>
; [["Helsingissä" "Helsinki"]
;  ["sataa" "sataa"]
;  ["lumipalloja" "lumipallo"]
;  ["ja" "ja"]
;  ["Pariisissa" "Pariisi"]
;  ["paistaa" "paistaa"]
;  ["aurinko" "aurinko"]
;  ["." "."]]
