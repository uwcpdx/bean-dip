(ns bean-dip.core-test
  (:require [clojure.test :as test]
            [bean-dip.core :as main])
  (:import [bean_dip BasicBean ParentBean BuiltBean BuiltBean$Builder]))

(set! *warn-on-reflection* true)

(defmethod main/->map-val :foo [_ value]
  (str value))

(defmethod main/->bean-val :foo [_ value]
  (Long/parseLong value))

(main/def-translation ParentBean #{:children})
(main/def-translation BasicBean #{[:foo-field :foo]})

(defmethod main/->bean-val :children [_ value]
  (mapv map->BasicBean value))

(def basic-map {:foo "42"})
(def basic-bean (doto (BasicBean.)
                  (.setFooField 42)))

(def parent-map
  {:children [basic-map]})

(def parent-bean
  (doto (ParentBean.)
    (.setChildren [basic-bean])))

(test/deftest parent-child
  (test/testing "ParentBean->map"
    (test/is (= parent-map (ParentBean->map parent-bean))))
  (test/testing "map->ParentBean"
    (test/is (= parent-bean (map->ParentBean parent-map))))
  (test/testing "omit keys with null values"
    (test/is (= {} (ParentBean->map (ParentBean.))))))

(defmethod main/builder-override [BuiltBean :bar-field] [_ ^BuiltBean$Builder builder bar]
  (.barFieldUnconventional builder bar))

(main/def-builder-translation BuiltBean
                              BuiltBean$Builder
                              #{[:foo-field :foo]
                                :bar-field
                                :some-condition?
                                :read-only-field}
                              {:builder-form    (BuiltBean/builder)
                               :get-only-fields #{:read-only-field}})

(def test-map
  {:foo             "42"
   :bar-field       "hello"
   :some-condition? true
   :read-only-field "READ ONLY"})

(def test-bean (BuiltBean. 42 "hello" true))

(test/deftest built-bean
  (test/is (= test-bean (.build (map->BuiltBean$Builder test-map)))))

