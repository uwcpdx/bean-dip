(ns bean-dip.core-test
  (:require [clojure.test :as test]
            [bean-dip.core :as main])
  (:import (bean_dip ParentBean TestBean)))

(set! *warn-on-reflection* true)

(main/deftranslation ParentBean #{:children})
(main/deftranslation TestBean #{[:foo-field :foo]})

(defmethod main/->bean-val :children [_ value]
  (mapv map->TestBean value))

(defmethod main/->map-val :foo [_ value]
  (str value))

(defmethod main/->bean-val :foo [_ value]
  (Long/parseLong value))

(def map-repr
  {:children [{:foo "42"}]})

(def bean-repr
  (doto (ParentBean.)
    (.setChildren [(doto (TestBean.)
                     (.setFooField 42))])))

(test/deftest bean->map
  (test/is (= map-repr (ParentBean->map bean-repr))))

(test/deftest map->bean
  (test/is (= bean-repr (map->ParentBean map-repr))))

