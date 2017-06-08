(ns bean-dip.core-test
  (:require [clojure.test :as test]
            [bean-dip.core :as main])
  (:import (bean_dip ParentBean TestBean TestBean$Builder)))

(set! *warn-on-reflection* true)

(main/def-translation ParentBean #{:children})

(defmethod main/builder-override [TestBean :bar-field] [_ ^TestBean$Builder builder bar]
  (.barFieldUnconventional builder bar))

(main/def-builder-translation TestBean
                              TestBean$Builder
                              #{[:foo-field :foo]
                                :bar-field
                                :some-condition?
                                :read-only-field}
                              {:builder-form   (TestBean/builder)
                               :exclude-fields #{:read-only-field}})

(defmethod main/->bean-val :children [_ value]
  (mapv map->TestBean value))

(defmethod main/->map-val :foo [_ value]
  (str value))

(defmethod main/->bean-val :foo [_ value]
  (Long/parseLong value))

(def test-map
  {:foo             "42"
   :bar-field       "hello"
   :some-condition? true
   :read-only-field "READ ONLY"})

(def map-repr
  {:children [test-map]})

(def test-bean (TestBean. 42 "hello" true))

(def bean-repr
  (doto (ParentBean.)
    (.setChildren [test-bean])))

(test/deftest bean->map
  (test/is (= map-repr (ParentBean->map bean-repr))))

(test/deftest map->bean
  (test/is (= bean-repr (map->ParentBean map-repr))))

(test/deftest omit-keys-with-null-values
  (test/is (= {}
              (ParentBean->map (ParentBean.)))))

(test/deftest map->builder
  (test/is (= test-bean (.build (map->TestBean$Builder test-map)))))

