(ns beandip.core-test
  (:require [clojure.test :as test]
            [beandip.core :as main])
  (:import (beandip OuterBean InnerBean)))

(def map->OuterBean
  (main/set-translation! OuterBean
                         #{:foo-field
                     :inners}))

(def map->InnerBean
  (main/set-translation! InnerBean
                         #{[:bar :bar-alias]}))

(defmethod main/->for-bean :inners [_ value]
  (mapv map->InnerBean value))

(defmethod main/->for-map :bar-alias [_ value]
  (str value))

(defmethod main/->for-bean :bar-alias [_ value]
  (Long/parseLong value))

(def map-repr
  {:foo-field "hi"
   :inners    [{:bar-alias "42"}]})

(def bean-repr
  (doto (OuterBean.)
    (.setFooField "hi")
    (.setInners [(doto (InnerBean.)
                   (.setBar 42))])))

(test/deftest ->map
  (test/is (= map-repr (main/->map bean-repr))))

(test/deftest map->bean
  (test/is (= bean-repr (map->OuterBean map-repr))))

