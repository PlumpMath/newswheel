(ns newswheel.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [newswheel.components.circleview :as circleview]
            [newswheel.components.reader :as reader]
            [ajax.core :refer [GET POST]]
            )
  (:import [goog.net Jsonp]
           [goog Uri]))

(enable-console-print!)


(def test-url
  "http://www.newyorker.com/arts/critics/atlarge/2014/03/24/140324crat_atlarge_menand")

(def app-state
  (atom
    {:hover-article ""
     :selected-article ""
     :urls [test-url]
     :article-info []}
    ))

;; state: list of article sources + hover-article + selected-article

(defn jsonp [uri]
  (let [out (chan)
        req (Jsonp. (Uri. uri))]
    (.send req nil (fn [res] (put! out res)))
    out))

;; Basic structures
(defrecord Article [title authors description content])

;; Embedly stuff
(def reader-width "600")

(def embedly-api-key "30c971bdaf234365b7ed9ce8f3817048")

(def embedly-api-url
  (str "http://api.embed.ly/1/extract?key="
       embedly-api-key
        "&format=json&url="))

(defn parse-embedly [resp]
  [
    (:title resp )
    (:authors resp )
    (:description resp )
    (:content resp )])

(defn query-url [q]
  (str embedly-api-url q))

(defn upd-embed [resp]
  (swap! app-state conj (:article-info app-state)
   (parse-embedly (js->clj resp :keywordize-keys true))))

(defn req-handler [response]
  (.log js/console (js->clj response)))

;; Sanity checks

(defn retrieve-embed [source callback error-callback]
  (.send (goog.net.Jsonp. (query-url source) )
    "" callback error-callback))

(defn upd-all []
  (doseq [url (:urls @app-state)]
       (retrieve-embed url upd-embed req-handler)))


(upd-all)

;(retrieve-embed test-url upd-embed req-handler)
;
(defn main [state owner]
  (reify
    ;om/IDidMount
    ;(did-mount [_]
      ;(om/transact! state
                  ;#(assoc % :article-info
                    ;(parse-urls (:urls %)))))
    om/IRender
    (render [_]
    (html
      [:div
        (om/build circleview/main state {})
        (om/build reader/main state {})]))))



(om/root
  main
  app-state
  {:target (. js/document (getElementById "app"))})

